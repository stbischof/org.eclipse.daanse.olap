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

import java.util.Optional;

/**
 * How a parent-child hierarchy's rows point at their parents, and the closure table descendants may be read from
 * instead of walking the tree.
 *
 * @param parent          the column that holds the key of a member's parent
 * @param nullParentValue the value of that column that marks a root, where it is not SQL NULL; {@code null} for NULL
 * @param closure         the table of every (ancestor, descendant) pair, or {@code null} when there is none
 */
public record ParentChildColumns(ColumnDef parent, String nullParentValue, Closure closure) {

    /** A table that holds each member once per ancestor, so that descendants are read in one go. */
    public record Closure(RelationalSource.Table table, ColumnDef parent, ColumnDef child) {
    }

    public Optional<Closure> closureTable() {
        return Optional.ofNullable(closure);
    }
}
