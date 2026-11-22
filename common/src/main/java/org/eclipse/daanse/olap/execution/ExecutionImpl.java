/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2002-2005 Julian Hyde
 * Copyright (C) 2005-2021 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.execution;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.cache.CacheCommand;
import org.eclipse.daanse.olap.api.cache.OlapSegmentCacheManager;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.execution.ExecutionContext;
import org.eclipse.daanse.olap.api.execution.ExecutionMetadata;
import org.eclipse.daanse.olap.api.execution.QueryTiming;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.EventCommon;
import org.eclipse.daanse.olap.api.monitor.event.ExecutionEndEvent;
import org.eclipse.daanse.olap.api.monitor.event.ExecutionEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.ExecutionPhaseEvent;
import org.eclipse.daanse.olap.api.monitor.event.ExecutionStartEvent;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.ServertEventCommon;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.common.QueryTimingImpl;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.eclipse.daanse.olap.exceptions.MemoryLimitExceededException;
import org.eclipse.daanse.olap.exceptions.QueryCanceledException;
import org.eclipse.daanse.olap.exceptions.QueryTimeoutException;

/**
 * Execution context.
 *
 *
 * Loosely corresponds to a CellSet. A given statement may be executed several
 * times over its lifetime, but at most one execution can be going on at a time.
 *
 *
 * @author jhyde
 */
public class ExecutionImpl implements Execution {
    /**
     * Used for MDX logging, allows for a MDX Statement UID.
     */
    private static final AtomicLong SEQ = new AtomicLong();

    /**
     * Wording of the timeout error. Carries the configured budget in seconds,
     * because that is the unit the {@code queryTimeout} setting is expressed in and
     * the first thing anyone reading the message wants to know.
     */
    private static final String QUERY_TIMEOUT_MESSAGE = "Query timeout of {0} seconds reached";

    private final AbstractStatement statement;

    /**
     * ExecutionContext for ScopedValue-based context propagation. This provides the
     * bridge to the new ExecutionContext system.
     */
    private final ExecutionContext executionContext;

    // volatile: writers hold stateLock, but checkCancelOrTimeout reads and
    // writes under the THIS monitor - without volatile there was no
    // happens-before between a cancel and the query thread's state read
    private volatile State state = State.FRESH;

    /**
     * This is a lock object to sync on when changing the {@link #state} variable.
     */
    private final Object stateLock = new Object();

    /**
     * If not null, this query was notified that it might cause an OutOfMemoryError.
     */
    private String outOfMemoryMsg;

    private LocalDateTime startTime;
    private final Optional<Duration> duration;

    /**
     * Absolute moment at which this execution has run out of time, or null when it
     * has no time limit.
     *
     * <p>
     * Computed once in {@link #start()} rather than compared against
     * {@link #startTime} on every check: a deadline is a single value to test, and
     * a null deadline states "no limit" without arithmetic. A zero or negative
     * {@link #duration} yields null - that is what makes {@code queryTimeout=0}
     * mean "no limit" rather than "already expired".
     * </p>
     */
    private Instant deadline;
    private final QueryTimingImpl queryTiming = new QueryTimingImpl();
    private int phase;
    private int cellCacheHitCount;
    private int cellCacheMissCount;
    private int cellCachePendingCount;
    private int expCacheHitCount;
    private int expCacheMissCount;

    /**
     * Execution id, global within this JVM instance.
     */
    private final long id;

    public static final ExecutionImpl NONE = new ExecutionImpl(null, Optional.empty());

    private final Execution parent;

    public ExecutionImpl(Statement statement, Optional<Duration> duration) {
        Execution parentExec = null;
        ExecutionContext parentContext = null;
        // Skip ExecutionContext during static initialization (when statement is null for NONE)
        // Use currentOrNull() as we need to handle the case where no context is bound
        if (statement != null) {
            ExecutionContext currentContext = ExecutionContext.currentOrNull();
            if (currentContext != null) {
                parentExec = currentContext.getExecution();
                if (parentExec != null) {
                    parentContext = parentExec.asContext();
                }
            }
        }
        this.parent = parentExec;
        this.id = SEQ.getAndIncrement();
        this.statement = (AbstractStatement) statement;
        this.duration = duration;

        // Initialize ExecutionContext
        if (parentContext != null) {
            // Child context - inherits timeout from parent
            this.executionContext = parentContext.createChild(ExecutionMetadata.empty(), Optional.empty());
        } else {
            // Root context - create with execution metadata
            ExecutionMetadata rootMetadata = ExecutionMetadata.of("ExecutionImpl", "Query Execution", null, 0);
            this.executionContext = ExecutionContext.root(duration, rootMetadata);
        }

        // Set bidirectional reference for compatibility with legacy code
        this.executionContext.setExecution(this);
    }

    /**
     * Marks the start of an Execution instance. It is called by
     * {@link Statement#start(ExecutionImpl)} automatically. Users don't need to
     * call this method.
     */
    public void start() {
        assert this.state == State.FRESH;
        this.startTime = LocalDateTime.now();
        this.deadline = duration
                .filter(d -> !d.isZero() && !d.isNegative())
                .map(Instant.now()::plus)
                .orElse(null);
        this.state = State.RUNNING;
        this.queryTiming.init(this.statement.getProfileHandler() != null);
        fireExecutionStartEvent();
    }

    private String getMdx() {
        final Query query = statement.query;
        return query != null ? Util.unparse(query) : null;
    }

    public void tracePhase(int hitCount, int missCount, int pendingCount) {
        final Connection connection = statement.getDaanseConnection();
        final Context context = connection.getContext();
        final int hitCountInc = hitCount - this.cellCacheHitCount;
        final int missCountInc = missCount - this.cellCacheMissCount;
        final int pendingCountInc = pendingCount - this.cellCachePendingCount;
        ExecutionPhaseEvent executionPhaseEvent = new ExecutionPhaseEvent(
                new ExecutionEventCommon(new MdxStatementEventCommon(new ConnectionEventCommon(
                        new ServertEventCommon(EventCommon.ofNow(), context.getName()), connection.getId()),
                        statement.getId()), id),
                phase, hitCountInc, missCountInc, pendingCountInc);

        context.getMonitor().accept(executionPhaseEvent);
        ++phase;
        this.cellCacheHitCount = hitCount;
        this.cellCacheMissCount = missCount;
        this.cellCachePendingCount = pendingCount;
    }

    /**
     * Cancels the execution instance.
     */
    public void cancel() {
        synchronized (stateLock) {
            this.state = State.CANCELED;
        }
        // OUTSIDE stateLock: canceling the statements is one JDBC network
        // round-trip per statement (PG/MySQL open a new connection for a
        // cancel), and the shepherd's single timer thread polls
        // isCancelOrTimeout() - which takes this lock - for EVERY running
        // query. One cancel against a wedged database must not suspend
        // timeout enforcement server-wide.
        this.cancelSqlStatements();
        fireExecutionEndEvent();
    }

    /**
     * This method will change the state of this execution to {@link State#ERROR}
     * and will set the message to display. Cleanup of the resources used by this
     * execution instance will be performed in the background later on.
     *
     * @param msg The message to display to the user, describing the problem
     *            encountered with the memory space.
     */
    public final void setOutOfMemory(String msg) {
        synchronized (stateLock) {
            assert msg != null;
            this.outOfMemoryMsg = msg;
            this.state = State.ERROR;
        }
    }

    /**
     * Checks the state of this Execution and throws an exception if something is
     * wrong. This method should be called by the user thread.
     *
     * It won't throw anything if the query has successfully completed.
     *
     * @throws OlapRuntimeException The exception encountered.
     */
    public synchronized void checkCancelOrTimeout() throws OlapRuntimeException {
        if (parent != null) {
            parent.checkCancelOrTimeout();
        }
        boolean needInterrupt = false;
        switch (this.state) {
        case CANCELED:
            try {
                if (Thread.interrupted()) {
                    // Checking the state of the thread will clear the
                    // interrupted flag so we can send an event out.
                    // After that, we make sure that we set it again
                    // so the thread state remains consistent.
                    needInterrupt = true;
                }
                fireExecutionEndEvent();
            } finally {
                if (needInterrupt) {
                    Thread.currentThread().interrupt();
                }
            }
            throw new QueryCanceledException();
        case RUNNING:
        case TIMEOUT:
            if (deadline != null && Instant.now().isAfter(deadline)) {
                this.state = State.TIMEOUT;
                fireExecutionEndEvent();
                throw new QueryTimeoutException(MessageFormat.format(QUERY_TIMEOUT_MESSAGE,
                        duration.map(Duration::toMillis).orElse(0L) / 1000d));
            }
            break;
        case ERROR:
            try {
                if (Thread.interrupted()) {
                    // Checking the state of the thread will clear the
                    // interrupted flag so we can send an event out.
                    // After that, we make sure that we set it again
                    // so the thread state remains consistent.
                    needInterrupt = true;
                }
                fireExecutionEndEvent();
            } finally {
                if (needInterrupt) {
                    Thread.currentThread().interrupt();
                }
            }
            throw new MemoryLimitExceededException(outOfMemoryMsg);
        }
    }

    /**
     * Returns whether this execution is currently in a failed state and will throw
     * an exception as soon as the next check is performed using
     * {@link ExecutionImpl#checkCancelOrTimeout()}.
     *
     * @return True or false, depending on the timeout state.
     */
    public boolean isCancelOrTimeout() {
        if (parent != null && parent.isCancelOrTimeout()) {
            return true;
        }
        synchronized (stateLock) {
            return state == State.CANCELED || state == State.ERROR || state == State.TIMEOUT
                    || (state == State.RUNNING && deadline != null && Instant.now().isAfter(deadline));
        }
    }

    /**
     * Tells whether this execution is done executing.
     */
    public boolean isDone() {
        synchronized (stateLock) {
            switch (this.state) {
            case CANCELED:
            case DONE:
            case ERROR:
            case TIMEOUT:
                return true;
            default:
                return false;
            }
        }
    }

    /**
     * Called by the RolapResultShepherd when the execution needs to clean all of
     * its resources for whatever reasons, typically when an exception has occurred
     * or the execution has ended. Any currently running SQL statements will be
     * canceled. It should only be called if
     * {@link ExecutionImpl#isCancelOrTimeout()} returns true.
     *
     *
     * This method doesn't need to be called by a user. It will be called internally
     * by Mondrian when the system is ready to clean the remaining resources.
     *
     *
     * To check if this execution is failed, use
     * {@link ExecutionImpl#isCancelOrTimeout()} instead.
     */
    public void cancelSqlStatements() {
        if (parent != null) {
            parent.cancelSqlStatements();
        }
        // Delegate to ExecutionContext which now handles statement cancellation
        executionContext.cancel();
        // Also cleanup the segment registrations from the index.
        unregisterSegmentRequests();
    }

    /**
     * Called when query execution has completed. Once query execution has ended, it
     * is not possible to cancel or timeout the query until it starts executing
     * again.
     */
    public void end() {
        synchronized (stateLock) {
            queryTiming.done();
            if (this.state == State.FRESH || this.state == State.RUNNING) {
                this.state = State.DONE;
            }
        }
        // OUTSIDE stateLock: this is a synchronous actor round-trip, and
        // isCancelOrTimeout()/checkCancelOrTimeout() polls (shepherd timer,
        // every registerStatement) must never queue behind it.
        unregisterSegmentRequests();
        // Fire up a monitor event.
        fireExecutionEndEvent();
    }

    /**
     * Calls into the SegmentCacheManager and unregisters all the registrations made
     * for this execution on segments form the index.
     */
    public void unregisterSegmentRequests() {
        // We also have to cancel all requests for the current segments.
        final ExecutionContext currentContext = executionContext;
        AbstractBasicContext abc = (AbstractBasicContext) statement.getConnection().getContext();
        final var aggregationManager = abc.getAggregationManager();
        if (aggregationManager == null) {
            // a provider without segment caches registered nothing
            return;
        }
        final OlapSegmentCacheManager shared = aggregationManager.getSegmentCacheManager();
        // an isolated session (pending writeback / session caching)
        // registered in ITS OVERLAY's index - sweeping only the shared one
        // left the registrations (and any linked SQL statement) behind for
        // the life of the overlay. Overlays share the shared manager's
        // actor, so ONE command may sweep both registries.
        final OlapSegmentCacheManager session =
            aggregationManager.peekSegmentCacheManager(statement.getConnection());
        shared.execute(new CacheCommand<Void>() {
            @Override
            public Void call() throws Exception {
                shared.getIndexRegistry().cancelExecutionSegments(ExecutionImpl.this);
                if (session != null && session != shared) {
                    session.getIndexRegistry().cancelExecutionSegments(ExecutionImpl.this);
                }
                return null;
            }

            @Override
            public ExecutionContext getExecutionContext() {
                return currentContext;
            }
        });
    }


    public Statement getDaanseStatement() {
        return statement;
    }

    public final QueryTiming getQueryTiming() {
        return queryTiming;
    }

    public final long getId() {
        return id;
    }

    public final Duration getElapsedMillis() {
        return startTime == null ? Duration.ZERO : Duration.between(startTime, LocalDateTime.now());
    }

    private void fireExecutionEndEvent() {
        final Connection connection = statement.getDaanseConnection();
        final Context<?> context = connection.getContext();

        ExecutionEndEvent endEvent = new ExecutionEndEvent(new ExecutionEventCommon(

                new MdxStatementEventCommon(new ConnectionEventCommon(new ServertEventCommon(
                        new EventCommon(Instant.now()),
                        context.getName()), connection.getId()), this.statement.getId()),
                this.id), phase, state, cellCacheHitCount, cellCacheMissCount, cellCachePendingCount, expCacheHitCount,
                expCacheMissCount);
        context.getMonitor().accept(endEvent);
    }

    /**
     * What every event of this execution carries -- the context, the connection, the statement and this execution --
     * stamped now.
     */
    public ExecutionEventCommon eventCommon() {
        final Connection connection = statement.getDaanseConnection();
        return new ExecutionEventCommon(new MdxStatementEventCommon(new ConnectionEventCommon(new ServertEventCommon(
                EventCommon.ofNow(), connection.getContext().getName()), connection.getId()), statement.getId()), id);
    }

    /** Hands an event of this execution to its context's monitor. */
    public void report(org.eclipse.daanse.olap.api.monitor.event.ExecutionEvent event) {
        statement.getDaanseConnection().getContext().getMonitor().accept(event);
    }

    private void fireExecutionStartEvent() {
        final Connection connection = statement.getDaanseConnection();
        final Context context = connection.getContext();

        ExecutionStartEvent executionStartEvent = new ExecutionStartEvent(new ExecutionEventCommon(

                new MdxStatementEventCommon(new ConnectionEventCommon(new ServertEventCommon(
                        new EventCommon(Instant.now()),
                        context.getName()), connection.getId()), statement.getId()),
                id), getMdx());
        context.getMonitor().accept(executionStartEvent);
    }

    public void setCellCacheHitCount(int cellCacheHitCount) {
        this.cellCacheHitCount = cellCacheHitCount;
    }

    public void setCellCacheMissCount(int cellCacheMissCount) {
        this.cellCacheMissCount = cellCacheMissCount;
    }

    public void setCellCachePendingCount(int cellCachePendingCount) {
        this.cellCachePendingCount = cellCachePendingCount;
    }

    public void setExpCacheCounts(int hitCount, int missCount) {
        this.expCacheHitCount = hitCount;
        this.expCacheMissCount = missCount;
    }

    public int getExpCacheHitCount() {
        return expCacheHitCount;
    }

    public int getExpCacheMissCount() {
        return expCacheMissCount;
    }

    /**
     * Returns the ExecutionContext for ScopedValue-based context propagation. This
     * bridges the legacy Execution interface with the new ExecutionContext system.
     *
     * @return the ExecutionContext representation of this execution
     */
    @Override
    public ExecutionContext asContext() {
        return executionContext;
    }
}
