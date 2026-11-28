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

import org.eclipse.daanse.olap.metadata.model.ValueKind;

/**
 * What a level, a measure or a property reads a value from: a column of a table, or a SQL expression computed
 * over the row. Both have a name the SQL may refer to them by, and a kind of value.
 */
public sealed interface ColumnDef permits PhysicalColumnDef, SqlExpressionColumnDef {

    String name();

    /** The kind of value the column holds; {@code null} when the catalog does not say. */
    ValueKind type();

    /**
     * A field of the fact rows named without a table, as a source that holds its facts itself names them: an
     * aggregator that orders by a field reads the fact value of that name.
     */
    static PhysicalColumnDef field(String name) {
        return new PhysicalColumnDef(name, null, null, null, null, true);
    }
}
