/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.api.relational;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.result.DrillThroughRows;

/**
 * A SQL statement and its JDBC result set; as {@link DrillThroughRows} it reads that result set.
 */
public interface SqlStatementI extends DrillThroughRows {


    /**
     * Executes the current statement, and handles any SQLException.
     */
    void execute();

    /**
     * Closes all resources (statement, result set) held by this SqlStatement.
     *
     * If any of them fails, wraps them in a RuntimeException describing the
     * high-level operation which this statement was performing. No further
     * error-handling is required to produce a descriptive stack trace, unless you
     * want to absorb the error.
     *
     * This method is idempotent.
     */
    void close();

    ResultSet getResultSet();

    /**
     * Returns the result set in a proxy which automatically closes this
     * SqlStatement (and hence also the statement and result set) when the result
     * set is closed.
     *
     * This helps to prevent connection leaks. The caller still has to remember to
     * call ResultSet.close(), of course.
     *
     * @return Wrapped result set
     */
    ResultSet getWrappedResultSet();

    @Override
    default List<String> getColumnNames() {
        try {
            ResultSetMetaData metaData = getResultSet().getMetaData();
            List<String> names = new ArrayList<>(metaData.getColumnCount());
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                names.add(metaData.getColumnLabel(i));
            }
            return names;
        } catch (SQLException e) {
            throw new OlapRuntimeException(e);
        }
    }

    @Override
    default boolean next() {
        try {
            return getResultSet().next();
        } catch (SQLException e) {
            throw new OlapRuntimeException(e);
        }
    }

    @Override
    default Object getValue(int columnIndex) {
        try {
            return getResultSet().getObject(columnIndex + 1);
        } catch (SQLException e) {
            throw new OlapRuntimeException(e);
        }
    }
}
