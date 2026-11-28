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
 * What a cube reads its facts from, or a hierarchy its members: a table, a join of sources, a select the catalog
 * writes, or a table given inline with its rows. A value: re-aliasing a source gives a new value that shares its
 * table, columns and parts.
 */
public sealed interface RelationalSource
        permits RelationalSource.Table, RelationalSource.Join, RelationalSource.SqlSelect, RelationalSource.Inline {

    /** The alias the SQL refers to this source by, where the catalog gives one. */
    Optional<String> alias();

    /** The alias for SQL: the given one, else what the source is naturally called. */
    String sqlAlias();

    /**
     * The same source under another alias; this very source when the alias is the one it has. A join carries no
     * alias of its own -- its elements do -- and answers with itself.
     */
    RelationalSource withAlias(String alias);

    /** A hint for the database on how to read a table, given as the dialect understands it. */
    record OptimizationHint(String type, String value) {
    }

    /**
     * A table or a view, with an optional where clause the catalog adds to every read of it.
     *
     * @param givenAlias the alias the SQL refers to the table by; {@code null} for the table's name
     * @param where the rows to keep, per dialect; {@code null} for all rows
     * @param hints what to tell the database about reading the table
     */
    record Table(String givenAlias, TableDef table, SqlStatement where, List<OptimizationHint> hints)
            implements RelationalSource {

        public Table {
            hints = List.copyOf(hints);
        }

        public static Table of(TableDef table) {
            return new Table(null, table, null, List.of());
        }

        @Override
        public Optional<String> alias() {
            return Optional.ofNullable(givenAlias);
        }

        @Override
        public String sqlAlias() {
            return givenAlias != null ? givenAlias : table.name();
        }

        @Override
        public Table withAlias(String newAlias) {
            // the where clause keeps the alias it was written with: rewriting SQL text is for the reader that knows
            // the dialect's quoting; the model only says which rows
            return java.util.Objects.equals(newAlias, givenAlias) ? this : new Table(newAlias, table, where, hints);
        }
    }

    /**
     * One side of a join: the source and the column it is joined by, under the alias the SQL refers to it by.
     *
     * @param alias  the alias of this side; {@code null} for the source's own
     * @param key    the column of this side the join compares
     * @param source what is joined
     */
    record Element(String alias, ColumnDef key, RelationalSource source) {

        /** The alias for SQL: the given one, else the source's. */
        public String sqlAlias() {
            return alias != null ? alias : source.sqlAlias();
        }
    }

    /** Two sources joined on a column each; a snowflake nests another join on the right. */
    record Join(Element left, Element right) implements RelationalSource {

        @Override
        public Optional<String> alias() {
            return Optional.empty();
        }

        @Override
        public String sqlAlias() {
            return left.sqlAlias();
        }

        @Override
        public Join withAlias(String newAlias) {
            return this;
        }
    }

    /**
     * A select the catalog writes itself, read as a table under its alias; one text per set of dialects.
     */
    record SqlSelect(String givenAlias, List<SqlStatement> sql) implements RelationalSource {

        public SqlSelect {
            sql = List.copyOf(sql);
        }

        @Override
        public Optional<String> alias() {
            return Optional.ofNullable(givenAlias);
        }

        @Override
        public String sqlAlias() {
            return givenAlias;
        }

        @Override
        public SqlSelect withAlias(String newAlias) {
            return java.util.Objects.equals(newAlias, givenAlias) ? this : new SqlSelect(newAlias, sql);
        }
    }

    /** A table whose rows the catalog gives itself, read under its alias. */
    record Inline(String givenAlias, InlineTableDef table) implements RelationalSource {

        @Override
        public Optional<String> alias() {
            return Optional.ofNullable(givenAlias);
        }

        @Override
        public String sqlAlias() {
            return givenAlias != null ? givenAlias : table.table().name();
        }

        @Override
        public Inline withAlias(String newAlias) {
            return java.util.Objects.equals(newAlias, givenAlias) ? this : new Inline(newAlias, table);
        }
    }
}
