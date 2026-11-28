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
import java.util.Optional;

/**
 * Which columns a level reads its members from: the key, and where they differ from it, the name, the caption and
 * the order. A level whose members come through a member source has none.
 *
 * @param keys        the columns that identify a member, never empty, in the order the key lists its parts; members
 *                    are read by the first
 * @param name        the column the member's name is read from; {@code null} for the key
 * @param caption     the column the caption is read from; {@code null} for the name
 * @param ordinals    the columns members are ordered by, first one first; empty for the key
 * @param sourceAlias the alias of the hierarchy's source the columns lie in, where that source is a join of a table
 *                    with itself and the table name does not say; {@code null} otherwise
 */
public record LevelColumns(List<ColumnDef> keys, ColumnDef name, ColumnDef caption, List<OrderedColumnDef> ordinals,
        String sourceAlias) {

    public LevelColumns {
        keys = List.copyOf(keys);
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("a level is keyed by one column at least");
        }
        ordinals = List.copyOf(ordinals);
    }

    public static LevelColumns of(ColumnDef key) {
        return new LevelColumns(List.of(key), null, null, List.of(), null);
    }

    /** A level keyed by one column. */
    public static LevelColumns of(ColumnDef key, ColumnDef name, ColumnDef caption, List<OrderedColumnDef> ordinals,
            String sourceAlias) {
        return new LevelColumns(List.of(key), name, caption, ordinals, sourceAlias);
    }

    /** The first column of the key: the whole key of a level keyed by one column. */
    public ColumnDef firstKey() {
        return keys.get(0);
    }

    public Optional<ColumnDef> nameColumn() {
        return Optional.ofNullable(name);
    }

    public Optional<ColumnDef> captionColumn() {
        return Optional.ofNullable(caption);
    }
}
