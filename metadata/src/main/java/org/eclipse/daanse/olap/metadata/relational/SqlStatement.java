/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.metadata.relational;

import java.util.List;

/**
 * A piece of SQL the catalog writes itself -- a where clause, an expression, a whole select -- for the dialects it
 * names. An empty list of dialects, or the dialect {@code generic}, means every database.
 *
 * @param dialects the dialect names the statement is written for; empty for all
 * @param sql      the text
 */
public record SqlStatement(List<String> dialects, String sql) {

    public static final String GENERIC = "generic";

    public SqlStatement {
        dialects = List.copyOf(dialects);
    }

    /** A statement for every database. */
    public static SqlStatement of(String sql) {
        return new SqlStatement(List.of(), sql);
    }

    /** Whether the statement is written for this dialect, or for every one. */
    public boolean appliesTo(String dialect) {
        return dialects.isEmpty() || dialects.contains(GENERIC)
                || dialects.stream().anyMatch(d -> d.equalsIgnoreCase(dialect));
    }
}
