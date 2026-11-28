/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.metadata.model;

/** A property every member of a level carries, such as a population or a colour. */
public record MemberPropertyDef(String name, ValueKind kind, ElementInfo info, FormatterSpec formatter,
        org.eclipse.daanse.olap.metadata.relational.ColumnDef column, boolean dependsOnLevelValue) {

    /**
     * A property read from a column; {@code dependsOnLevelValue} says the column's value follows from the level's key
     * (Mondrian dependsOnLevelValue), so a reader of it beside the key needs not group by it.
     */
    public MemberPropertyDef(String name, ValueKind kind, ElementInfo info, FormatterSpec formatter,
            org.eclipse.daanse.olap.metadata.relational.ColumnDef column) {
        this(name, kind, info, formatter, column, false);
    }

    /** A property whose values come through the member source, not from a column. */
    public MemberPropertyDef(String name, ValueKind kind, ElementInfo info, FormatterSpec formatter) {
        this(name, kind, info, formatter, null);
    }
}
