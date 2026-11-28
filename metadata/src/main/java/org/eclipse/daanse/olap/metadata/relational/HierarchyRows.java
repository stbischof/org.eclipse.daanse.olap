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
 * Where a hierarchy's members lie: the source its rows come from and the column the facts join it by. The binding
 * of a hierarchy to tables; a hierarchy that reads its members from the fact rows themselves has a source of
 * {@code null} and is read from the cube's facts.
 *
 * @param source          what the member rows are read from; {@code null} for the cube's facts
 * @param primaryKey      the column of the source a fact's foreign key points at
 * @param primaryKeyTable the alias of the source the primary key lies in, where the source is a join;
 *                        {@code null} for a single table
 * @param uniqueKeyLevel  the level whose key alone tells a row of the source apart, so reading the rows down to it
 *                        needs no GROUP BY (Mondrian uniqueKeyLevelName); {@code null} for none
 */
public record HierarchyRows(RelationalSource source, ColumnDef primaryKey, String primaryKeyTable,
        String uniqueKeyLevel) {

    public HierarchyRows(RelationalSource source, ColumnDef primaryKey, String primaryKeyTable) {
        this(source, primaryKey, primaryKeyTable, null);
    }

    public Optional<RelationalSource> ownSource() {
        return Optional.ofNullable(source);
    }
}
