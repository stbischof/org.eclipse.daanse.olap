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

/** A stored measure of a cube. Identity: a virtual cube points at the measure it takes over. */
public final class MeasureDef {

    private final String name;
    private final AggregatorDef aggregator;
    private final ValueKind kind;
    private final String formatString;
    private final FormatterSpec formatter;
    private final LocalizedText displayFolder;
    private final ElementInfo info;
    private final Integer ordinal;
    private final org.eclipse.daanse.olap.metadata.relational.ColumnDef column;
    private final java.util.function.Function<FactRow, Object> derived;

    public MeasureDef(String name, AggregatorDef aggregator, ValueKind kind, String formatString,
            FormatterSpec formatter, LocalizedText displayFolder, ElementInfo info) {
        this(name, aggregator, kind, formatString, formatter, displayFolder, info, null);
    }

    public MeasureDef(String name, AggregatorDef aggregator, ValueKind kind, String formatString,
            FormatterSpec formatter, LocalizedText displayFolder, ElementInfo info, Integer ordinal) {
        this(name, aggregator, kind, formatString, formatter, displayFolder, info, ordinal, null);
    }

    public MeasureDef(String name, AggregatorDef aggregator, ValueKind kind, String formatString,
            FormatterSpec formatter, LocalizedText displayFolder, ElementInfo info, Integer ordinal,
            java.util.function.Function<FactRow, Object> derived) {
        this(name, aggregator, kind, formatString, formatter, displayFolder, info, ordinal, derived, null);
    }

    public MeasureDef(String name, AggregatorDef aggregator, ValueKind kind, String formatString,
            FormatterSpec formatter, LocalizedText displayFolder, ElementInfo info, Integer ordinal,
            java.util.function.Function<FactRow, Object> derived,
            org.eclipse.daanse.olap.metadata.relational.ColumnDef column) {
        this.column = column;
        this.derived = derived;
        this.ordinal = ordinal;
        this.name = name;
        this.aggregator = aggregator;
        this.kind = kind;
        this.formatString = formatString;
        this.formatter = formatter;
        this.displayFolder = displayFolder;
        this.info = info;
    }

    public String name() {
        return name;
    }

    /** Where the measure stands among the measures of its cube (MEMBER_ORDINAL); {@code null} where it is described. */
    public Integer ordinal() {
        return ordinal;
    }

    /**
     * How a fact derives this measure's value from its other values, as SSAS' named calculation and Mondrian's
     * MeasureExpression do: per fact, before aggregating -- the largest margin is
     * the largest of the facts' margins, not the largest price less the largest cost. A result
     * {@code null} gives the fact no value, as SQL NULL. The cell source computes it; one that cannot refuses the
     * measure by name. {@code null} for a measure read under its own name.
     */
    public java.util.function.Function<FactRow, Object> derived() {
        return derived;
    }

    /** The column the facts give this measure from; empty for a measure read through a source of its own. */
    public java.util.Optional<org.eclipse.daanse.olap.metadata.relational.ColumnDef> column() {
        return java.util.Optional.ofNullable(column);
    }

    public AggregatorDef aggregator() {
        return aggregator;
    }

    public ValueKind kind() {
        return kind;
    }

    /** {@code null} for the default format. */
    public String formatString() {
        return formatString;
    }

    /** {@code null} when the format string is enough. */
    public FormatterSpec formatter() {
        return formatter;
    }

    /** {@code null} when the measure sits in no folder. */
    public LocalizedText displayFolder() {
        return displayFolder;
    }

    public ElementInfo info() {
        return info;
    }

    @Override
    public String toString() {
        return "measure " + name;
    }
}
