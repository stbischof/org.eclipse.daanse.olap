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

import java.util.ArrayList;
import java.util.List;

/**
 * A table the catalog gives with its rows, for a dimension small enough to write down: the columns as a table of
 * their own, and the rows as values in the order of those columns. A row's value may be {@code null}, as a SQL NULL.
 */
public record InlineTableDef(TableDef table, List<List<Object>> rows) {

    public InlineTableDef {
        List<List<Object>> copy = new ArrayList<>(rows.size());
        for (List<Object> row : rows) {
            if (row.size() != table.columns().size()) {
                throw new IllegalArgumentException("inline table " + table.name() + ": a row holds " + row.size()
                        + " values for " + table.columns().size() + " columns");
            }
            copy.add(java.util.Collections.unmodifiableList(new ArrayList<>(row)));
        }
        rows = java.util.Collections.unmodifiableList(copy);
    }
}
