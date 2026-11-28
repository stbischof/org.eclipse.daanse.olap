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
 * The table written values go to: one column per dimension usage that holds the member key of the written cell, one
 * column per measure that may be written, beside the columns added for the id and the user.
 *
 * @param schema     the schema it lies in, or {@code null} for the connection's
 * @param attributes the dimension usages of the cube and their key columns
 * @param measures   the measures that may be written and their columns
 */
public record WritebackTableDef(String name, String schema, List<Attribute> attributes, List<Measure> measures) {

    public WritebackTableDef {
        attributes = List.copyOf(attributes);
        measures = List.copyOf(measures);
    }

    /** A dimension usage of the cube and the column that holds the key of the written cell's member in it. */
    public record Attribute(String usage, ColumnDef column) {
    }

    /**
     * A measure of the cube and the column its written values go to.
     *
     * @param editable whether a client may write it; a measure listed but not editable is shown as read-only
     */
    public record Measure(String measure, ColumnDef column, boolean editable) {
    }
}
