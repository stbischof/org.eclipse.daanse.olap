/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.ContextConfig;
import org.eclipse.daanse.olap.api.agg.OlapAggregationManager;
import org.eclipse.daanse.olap.api.cache.CatalogCache;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.monitor.EventBus;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionEndEvent;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionStartEvent;
import org.eclipse.daanse.olap.api.monitor.event.EventCommon;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementEndEvent;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementStartEvent;
import org.eclipse.daanse.olap.api.monitor.event.ServertEventCommon;
import org.eclipse.daanse.olap.api.result.ResultShepherd;
import org.eclipse.daanse.olap.common.MapContextConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBasicContext<C extends Connection> implements Context<C> {

	public static final String SERVER_ALREADY_SHUTDOWN = "Server already shutdown.";
	/**
	 * Id of server. Unique within JVM's lifetime. Not the same as the ID of the
	 * server within a lockbox.
	 */
	private final long id = ID_GENERATOR.incrementAndGet();

	protected ResultShepherd shepherd;

	private final List<Connection> connections = Collections.synchronizedList(new ArrayList<>());

	private final List<Statement> statements =Collections.synchronizedList(new ArrayList<>());

    // volatile: the actor and SQL threads read this unsynchronized, and a
	// test tap installed after startup needs a happens-before edge
	protected volatile EventBus eventBus;

	protected OlapAggregationManager aggMgr;

	// the catalog pool (the historical "schema cache" - flushSchemaCache()
	// keeps the old SPI name)
	protected CatalogCache catalogCache;


	// volatile: a Cleaner-driven shutdown runs on a foreign thread; the
	// getters' guard reads must see it
	private volatile boolean shutdown = false;

	/**
	 * Safety net for embedders that never call shutdown (OSGi deactivates,
	 * the testkit closes, but direct constructions may not): the Cleaner
	 * action must hold NO strong path back to the context, or the context
	 * stays reachable through the Cleaner forever and the action can never
	 * fire. The managers all reference the context, so the action carries
	 * only the shepherd (context-free: executor, timer, an empty task list
	 * once queries ended) and the aggregation manager's
	 * {@code orphanCleanup()} runnable, whose contract is the same
	 * no-context-capture rule. The catalog cache is deliberately absent -
	 * it references the context and its heap dies with it; external
	 * segment stores keep their entries by design. NOTE: with an attached
	 * external cache the manager's async listener is registered on the
	 * (OSGi-owned) service and pins the context - there, deactivation is
	 * the teardown path and this net stays cold.
	 */
	private static final java.lang.ref.Cleaner CLEANER = java.lang.ref.Cleaner.create();
	private java.lang.ref.Cleaner.Cleanable cleanable;
	private java.util.concurrent.atomic.AtomicBoolean teardownDone;

	/** The orphan teardown, deliberately without a reference to the context. */
	private record OrphanAction(ResultShepherd shepherd, Runnable aggOrphanCleanup,
			java.util.concurrent.atomic.AtomicBoolean done) implements Runnable {
		@Override
		public void run() {
			if (!done.compareAndSet(false, true)) {
				return;
			}
			try {
				aggOrphanCleanup.run();
				shepherd.shutdown();
			} catch (RuntimeException | Error e) {
				LOGGER.info("orphaned context cleanup failed", e);
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBasicContext.class);

	private static final AtomicLong ID_GENERATOR = new AtomicLong();

	protected Map<String, Object> configuration = null;

	/**
	 * Reads {@link #configuration} through a supplier rather than a copy, so it
	 * stays correct across {@link #updateConfiguration(Map)} and across the
	 * in-place writes the test contexts do.
	 */
	private final ContextConfig config = new MapContextConfig(() -> configuration);


	protected void updateConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	@Override
	public ContextConfig getConfig() {
		return config;
	}

	/** Arms the orphaned-context safety net; call once resources exist. */
	protected void registerCleanup() {
		this.teardownDone = new java.util.concurrent.atomic.AtomicBoolean(false);
		this.cleanable = CLEANER.register(this,
				new OrphanAction(shepherd, aggMgr.orphanCleanup(), teardownDone));
	}

	protected long getId() {
		return id;
	}

	/**
	 * Every compiled calc of a profiled statement in a hull that measures it. Shared by every art, so a query
	 * profiles the same whichever answers it.
	 */
	@Override
	public org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler createProfilingCompiler(
			org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler compiler) {
		return new org.eclipse.daanse.olap.calc.base.profile.ProfilingCompiler(compiler);
	}

	@Override
	public ResultShepherd getResultShepherd() {
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		return this.shepherd;
	}

	public OlapAggregationManager getAggregationManager() {
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		return aggMgr;
	}

	protected void shutdown() {
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		this.shutdown = true;
		// the FULL ordered teardown - the orphan action is only the
		// context-free subset. The shared once-latch keeps the two from
		// running on top of each other.
		if (teardownDone == null || teardownDone.compareAndSet(false, true)) {
			// a provider without a catalog cache or a cell cache manager leaves them unset
			if (catalogCache != null) {
				catalogCache.clear();
			}
			if (aggMgr != null) {
				aggMgr.shutdown();
			}
			if (shepherd != null) {
				shepherd.shutdown();
			}
		}
		if (cleanable != null) {
			// latch already spent: this only unregisters the safety net
			cleanable.clean();
		}
	}

	@Override
	synchronized public void addConnection(Connection connection) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addConnection , id={}, statements={}, connections=", id, statements.size(),
					connections.size());
		}
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		connections.add(connection);

		ConnectionStartEvent connectionStartEvent = new ConnectionStartEvent(new ConnectionEventCommon(
								new ServertEventCommon(
				EventCommon.ofNow(), getName()), connection.getId()));
		eventBus.accept(connectionStartEvent);
	}

	@Override
	synchronized public void removeConnection(Connection connection) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeConnection , id={}, statements={}, connections={}", id, statements.size(),
					connections.size());
		}
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		connections.remove(connection);

		ConnectionEndEvent connectionEndEvent = new ConnectionEndEvent(
				new ConnectionEventCommon(
										new ServertEventCommon(
										EventCommon.ofNow(), getName()), connection.getId()));
		eventBus.accept(connectionEndEvent);
	}

	@Override
	public synchronized void addStatement(Statement statement) {
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addStatement , id={}, statements={}, connections={}", id, statements.size(),
					connections.size());
		}
		statements.add( statement);
		final Connection connection = statement.getDaanseConnection();

		MdxStatementStartEvent mdxStatementStartEvent = new MdxStatementStartEvent(new MdxStatementEventCommon(
				new ConnectionEventCommon(
						new ServertEventCommon(EventCommon.ofNow(), getName()),
						connection.getId()),
				statement.getId()));
		eventBus.accept(mdxStatementStartEvent);
	}

	@Override
	public synchronized void removeStatement(Statement statement) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeStatement , id={}, statements={}, connections={}", id, statements.size(),
					connections.size());
		}
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		statements.remove(statement);
		final Connection connection = statement.getDaanseConnection();


		MdxStatementEndEvent mdxStatementEndEvent = new MdxStatementEndEvent(
				new MdxStatementEventCommon(new ConnectionEventCommon(
						new ServertEventCommon(EventCommon.ofNow(), getName()),
						connection.getId()), statement.getId()));

		eventBus.accept(mdxStatementEndEvent);
	}

	@Override
	public EventBus getMonitor() {
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		return eventBus;
	}



	@Override
	public List<Statement> getStatements(org.eclipse.daanse.olap.api.connection.Connection connection) {
		return statements.stream().filter(stmnt -> stmnt.getDaanseConnection().equals(connection))
				.toList();
	}


	@Override
	public CatalogCache getCatalogCache() {
		return catalogCache;
	}

	@Override
	public <T> T getConfigValue(String key, T dflt, Class<T> clazz) {

		if (configuration == null) {
			return dflt;
		} else {
			Object value = configuration.get(key);
			if (value == null) {
				return dflt;
			}
			if (clazz.isInstance(value)) {
				return clazz.cast(value);
			}
			return dflt;
		}
	}

	/**
	 * Writes {@code value} for {@code key} into the live configuration - the
	 * write counterpart to {@link #getConfigValue(String, Object, Class)}, for
	 * test infrastructure that needs to override a single setting (see
	 * {@link #removeConfigValue(String)} to undo).
	 *
	 * <p>
	 * An absent or immutable {@link #configuration} (for instance the
	 * {@code Map.of()} a context activates with) is upgraded to a mutable copy
	 * first, so this is safe to call regardless of how the context was set up.
	 * </p>
	 */
	public synchronized void putConfigValue(String key, Object value) {
		mutableConfiguration().put(key, value);
	}

	/**
	 * Removes {@code key} from the live configuration, if present, so
	 * {@link #getConfigValue(String, Object, Class)} falls back to its default
	 * again.
	 */
	public synchronized void removeConfigValue(String key) {
		if (configuration != null) {
			mutableConfiguration().remove(key);
		}
	}

	private Map<String, Object> mutableConfiguration() {
		if (!(configuration instanceof ConcurrentHashMap)) {
			configuration = configuration == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(configuration);
		}
		return configuration;
	}
}
