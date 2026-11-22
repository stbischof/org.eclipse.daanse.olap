 /*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.execution;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.daanse.olap.api.calc.profile.ProfileHandler;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.query.component.Query;

/**
 * Implementation of {@link Statement}.
 *
 * Not part of Mondrian's public API. This class may change without
 * notice.
 *
 * @author jhyde
 */
public abstract class AbstractStatement implements Statement {
    private static final AtomicLong SEQ = new AtomicLong();

    /**
     * Writer to which to send profiling information, or null if profiling is
     * disabled.
     */
    private ProfileHandler profileHandler;

    protected Query query;

    /**
     * Query timeout, in milliseconds
     */
    private final long queryTimeout;

    /**
     * The current execution context, or null if query is not executing.
     */
    private Execution execution;

    /**
     * Whether {@link #cancel()} was called before the statement was started.
     * When the statement is started, it will immediately be marked canceled.
     */
    private boolean cancelBeforeStart;

    private final long id;

    /**
     * Creates a StatementImpl.
     */
    protected AbstractStatement(int queryTimeout) {
        this.queryTimeout = queryTimeout * 1000l;
        this.id = SEQ.getAndIncrement();
    }

    @Override
	public synchronized void start(Execution execution) {
        if (this.execution != null) {
            throw new AssertionError();
        }
        if (execution.getDaanseStatement() != this) {
            throw new AssertionError();
        }
        this.execution = execution;
        execution.start();
        if (cancelBeforeStart) {
            execution.cancel();
            cancelBeforeStart = false;
        }
    }

    @Override
	public synchronized void cancel() throws SQLException {
        if (execution == null) {
            // There is no current execution. Flag that we need to cancel as
            // soon as we start execution.
            cancelBeforeStart = true;
        } else {
            execution.cancel();
        }
    }

    @Override
	public synchronized void end(Execution execution) {
        if (execution == null
            || execution != this.execution)
        {
            throw new IllegalArgumentException(
                new StringBuilder().append(execution).append(" != ").append(this.execution).toString());
        }
        this.execution = null;
        execution.end();
    }

    @Override
	public void enableProfiling(ProfileHandler profileHandler) {
        this.profileHandler = profileHandler;
    }

    @Override
	public ProfileHandler getProfileHandler() {
        if (profileHandler != null) {
            return profileHandler;
        }
        // the handler the profiling scopes chose for the running query only; a statement keeps no scope decision
        // past its query
        org.eclipse.daanse.olap.api.query.component.Query query = getQuery();
        return query == null ? null : query.getProfileHandler();
    }

    @Override
	public long getQueryTimeoutMillis() {
        return queryTimeout;
    }

    @Override
	public CatalogReader getCatalogReader() {
        return getDaanseConnection().getCatalogReader().withLocus();
    }

    @Override
	public Catalog getCatalog() {
        return getDaanseConnection().getCatalog();
    }


    @Override
	public Query getQuery() {
        return query;
    }

    @Override
	public void setQuery(Query query) {
        this.query = query;
    }

    @Override
	public Execution getCurrentExecution() {
        return execution;
    }

    @Override
	public long getId() {
        return id;
    }
}
