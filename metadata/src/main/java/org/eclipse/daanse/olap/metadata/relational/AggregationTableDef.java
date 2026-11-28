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
 * A table that holds a cube's facts already summed up to some levels, read instead of the facts when a query asks
 * for no more than it holds. Named outright, or matched by a pattern over the table
 * names of the schema.
 */
public sealed interface AggregationTableDef permits AggregationTableDef.ByName, AggregationTableDef.ByPattern {

    /** Which columns of the aggregation table hold what. */
    Columns columns();

    /** Whether table names (and a pattern) match regardless of case. */
    boolean ignoreCase();

    /**
     * The columns of an aggregation table: how many facts each row stands for, which levels and measures it holds,
     * and how it joins the dimensions.
     *
     * @param factCount         the column that counts the facts behind a row
     * @param ignored           columns the matching may not take for levels or measures
     * @param foreignKeys       the fact-table foreign keys and the columns that stand for them here
     * @param measures          the measures and the columns that hold them summed up
     * @param levels            the levels and the columns that hold their keys
     * @param measureFactCounts per measure, the column that counts the facts that gave it a value
     */
    record Columns(ColumnDef factCount, List<ColumnDef> ignored, List<ForeignKey> foreignKeys, List<Measure> measures,
            List<Level> levels, List<MeasureFactCount> measureFactCounts) {

        public Columns {
            ignored = List.copyOf(ignored);
            foreignKeys = List.copyOf(foreignKeys);
            measures = List.copyOf(measures);
            levels = List.copyOf(levels);
            measureFactCounts = List.copyOf(measureFactCounts);
        }
    }

    /** A fact-table foreign key and the column of the aggregation table that stands for it. */
    record ForeignKey(ColumnDef factColumn, ColumnDef aggregationColumn) {
    }

    /**
     * A measure of the cube and the column that holds it summed up.
     *
     * @param rollupType how the summed values roll up further, where it differs from the measure's aggregator;
     *                   {@code null} for the same
     */
    record Measure(String name, ColumnDef column, String rollupType) {
    }

    /**
     * A level of the cube and the columns that hold its members in the aggregation table.
     *
     * @param name       the level, as {@code [Dimension].[Hierarchy].[Level]}
     * @param collapsed  whether the table holds this level without the levels above it
     * @param properties the member properties the table holds, each with its column
     */
    record Level(String name, ColumnDef column, ColumnDef nameColumn, ColumnDef captionColumn,
            List<ColumnDef> ordinalColumns, boolean collapsed, List<LevelProperty> properties) {

        public Level {
            ordinalColumns = List.copyOf(ordinalColumns);
            properties = List.copyOf(properties);
        }
    }

    record LevelProperty(String name, ColumnDef column) {
    }

    /** Per measure, the column that counts the facts that gave that measure a value. */
    record MeasureFactCount(ColumnDef factColumn, ColumnDef column) {
    }

    /**
     * An aggregation table named outright.
     *
     * @param approxRowCount how many rows the table holds, as the catalog estimates it; {@code null} for unknown
     */
    record ByName(TableDef table, Integer approxRowCount, Columns columns, boolean ignoreCase)
            implements AggregationTableDef {

        /** A table matched by its name as written. */
        public ByName(TableDef table, Integer approxRowCount, Columns columns) {
            this(table, approxRowCount, columns, false);
        }

        public Optional<Integer> rows() {
            return Optional.ofNullable(approxRowCount);
        }
    }

    /**
     * Every table of the schema whose name matches the pattern, less the excluded ones.
     *
     * @param pattern  a regular expression over table names
     * @param excludes tables the pattern must leave out
     */
    record ByPattern(String pattern, List<AggregationExcludeDef> excludes, Columns columns, boolean ignoreCase)
            implements AggregationTableDef {

        public ByPattern {
            excludes = List.copyOf(excludes);
        }

        /** A pattern matched as written. */
        public ByPattern(String pattern, List<AggregationExcludeDef> excludes, Columns columns) {
            this(pattern, excludes, columns, false);
        }
    }
}
