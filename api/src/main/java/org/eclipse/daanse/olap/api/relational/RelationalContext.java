/*
* Copyright (c) 2024 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*/

package org.eclipse.daanse.olap.api.relational;

import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.eclipse.daanse.sql.guard.api.SqlGuardFactory;

/**
 * A context whose provider answers from a relational database. What needs a JDBC data source or
 * a SQL dialect is asked here, so that {@link Context} stays free of SQL for providers that
 * have neither.
 *
 * @param <C> the connection type
 */
public interface RelationalContext<C extends Connection> extends Context<C> {

    /** The data source that hands out the JDBC connections to the database. */
    DataSource getDataSource();

    /** The dialect SQL is generated in for {@link #getDataSource()}. */
    Dialect getDialect();

    /** The pool that shares member key values between SQL member sources; empty when not pooled. */
    Optional<Map<Object, Object>> getSqlMemberSourceValuePool();

    /** The factory of the guard that checks SQL statements of a client; empty when SQL is not accepted. */
    Optional<SqlGuardFactory> getSqlGuardFactory();

    /**
     * The given context as a relational one.
     *
     * @throws IllegalArgumentException when the context is not backed by a database
     */
    static RelationalContext<?> of(Context<?> context) {
        if (context instanceof RelationalContext<?> relational) {
            return relational;
        }
        throw new IllegalArgumentException("Context is not backed by a relational database: "
                + (context == null ? null : context.getName()));
    }
}
