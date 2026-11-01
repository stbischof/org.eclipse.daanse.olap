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
 */

package org.eclipse.daanse.olap.spi;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.execution.Execution;

/**
 * Provides estimates of the number of rows in a database.
 *
 * Mondrian generally finds statistics providers via the
 * Dialect#getStatisticsProviders method on the dialect object for the
 * current connection. The default implementation of that method looks first at
 * the "mondrian.statistics.providers.DATABASE" property (substituting the
 * current database name, e.g. MYSQL or ORACLE, for DATABASE), then at
 * the the statistics-provider configuration "mondrian.statistics.providers"
 * property.
 *
 * mondrian.spi.impl.JdbcStatisticsProvider
 * mondrian.spi.impl.SqlStatisticsProvider
 *
 */
public interface StatisticsProvider {
    /**
     * Returns an estimate of the number of rows in a table.
     *
     * @param context Context
     * @param catalog Catalog name
     * @param schema Schema name
     * @param table Table name
     * @param execution Execution
     *
     * @return Estimated number of rows in table, or -1 if there
     * is no estimate
     */
    long getTableCardinality(
        Context context,
        String catalog,
        String schema,
        String table,
        Execution execution);

    /**
     * Returns an estimate of the number of rows returned by a query.
     *
     * @param context Context
     * @param sql Query, e.g. "select * from customers where age less 20"
     * @param execution Execution
     *
     * @return Estimated number of rows returned by query, or -1 if there
     * is no estimate
     */
    long getQueryCardinality(
        Context context,
        String sql,
        Execution execution);

    /**
     * Returns an estimate of the number of rows in a table.
     *
     * @param context Context
     * @param catalog Catalog name
     * @param schema Schema name
     * @param table Table name
     * @param column Column name
     * @param execution Execution
     *
     * @return Estimated number of rows in table, or -1 if there
     * is no estimate
     */
    long getColumnCardinality(
        Context context,
        String catalog,
        String schema,
        String table,
        String column,
        Execution execution);
}
