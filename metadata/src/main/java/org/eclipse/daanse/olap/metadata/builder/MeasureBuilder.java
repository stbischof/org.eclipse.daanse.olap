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
package org.eclipse.daanse.olap.metadata.builder;

import java.util.function.Function;

import org.eclipse.daanse.olap.metadata.model.AggregatorDef;
import org.eclipse.daanse.olap.metadata.model.FormatterSpec;
import org.eclipse.daanse.olap.metadata.model.MeasureDef;
import org.eclipse.daanse.olap.metadata.model.ValueKind;

/** Builds a stored measure. It sums numeric values unless told otherwise. */
public final class MeasureBuilder extends ElementBuilder<MeasureBuilder> {

    private AggregatorDef aggregator = AggregatorDef.Standard.SUM;
    /** What the measure was declared to hold; null when nothing was said and the kind is derived. */
    private ValueKind kind;
    private String formatString;
    private FormatterSpec formatter;
    private org.eclipse.daanse.olap.metadata.model.LocalizedText displayFolder;

    MeasureBuilder(String name) {
        super(name);
    }

    public MeasureBuilder aggregator(AggregatorDef aggregator) {
        this.aggregator = aggregator;
        return this;
    }

    public MeasureBuilder kind(ValueKind kind) {
        this.kind = kind;
        return this;
    }

    public MeasureBuilder formatString(String formatString) {
        this.formatString = formatString;
        return this;
    }

    public MeasureBuilder formatter(Function<Object, String> formatter) {
        this.formatter = new FormatterSpec.ByInstance(formatter);
        return this;
    }

    /** A formatter named by its class, as a catalog file names it; an engine that looks none up refuses it by name. */
    public MeasureBuilder formatterNamed(String className) {
        this.formatter = new FormatterSpec.ByName(className);
        return this;
    }

    /** The folder clients show the measure in. */
    public MeasureBuilder displayFolder(String folder) {
        this.displayFolder = folder == null ? null : org.eclipse.daanse.olap.metadata.model.LocalizedText.of(folder);
        return this;
    }

    private Integer ordinal;

    /** Where the measure stands among the measures of its cube, its MEMBER_ORDINAL. */
    public MeasureBuilder ordinal(int ordinal) {
        this.ordinal = ordinal;
        return this;
    }

    private Function<org.eclipse.daanse.olap.metadata.model.FactRow, Object> derived;

    /**
     * The value each fact gives this measure, computed from its other values before the cells aggregate it -- a
     * margin {@code row -> price - cost}; the facts then need no value under the measure's name.
     */
    public MeasureBuilder derived(Function<org.eclipse.daanse.olap.metadata.model.FactRow, Object> derived) {
        this.derived = derived;
        return this;
    }

    private org.eclipse.daanse.olap.metadata.relational.ColumnDef column;

    /** The column (or SQL expression) the facts give this measure from. */
    public MeasureBuilder column(org.eclipse.daanse.olap.metadata.relational.ColumnDef column) {
        this.column = column;
        return this;
    }

    MeasureDef build() {
        return new MeasureDef(name, aggregator, kind, formatString, formatter, displayFolder, info(), ordinal,
                derived, column);
    }
}
