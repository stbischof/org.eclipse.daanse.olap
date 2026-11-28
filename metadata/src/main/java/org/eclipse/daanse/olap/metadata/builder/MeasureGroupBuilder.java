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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.metadata.model.MeasureGroupDimensionDef;

/**
 * Describes a measure group: the dimensions its measures vary by, and what a cell says where they do not. A group
 * that names no dimension varies by all of them, which is what a cube with a single group means.
 */
public final class MeasureGroupBuilder {

    private final String name;
    private final List<MeasureGroupDimensionDef> dimensions = new ArrayList<>();
    private boolean ignoreUnrelated = true;

    MeasureGroupBuilder(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** A dimension usage of the cube this group varies by, as finely as that dimension goes. */
    public MeasureGroupBuilder dimension(String usageName) {
        return dimension(usageName, null);
    }

    /** A dimension usage of the cube this group varies by, and the level its facts reach in it. */
    public MeasureGroupBuilder dimension(String usageName, String granularity) {
        dimensions.add(new MeasureGroupDimensionDef(usageName, granularity));
        return this;
    }

    /**
     * Reads a member of a dimension this group does not vary by as the whole of that dimension, the default (SSAS:
     * true). The value is then right for the whole and repeats in every row of that dimension -- easy to misread,
     * which is why {@link #ignoreUnrelatedDimensions(boolean) false} exists.
     */
    public MeasureGroupBuilder ignoreUnrelatedDimensions() {
        return ignoreUnrelatedDimensions(true);
    }

    /**
     * @param ignore {@code false}: the cell is empty at a dimension the group does not vary by, except at the
     *               dimension's default member -- {@code ValidMeasure} still reads the whole
     */
    public MeasureGroupBuilder ignoreUnrelatedDimensions(boolean ignore) {
        this.ignoreUnrelated = ignore;
        return this;
    }

    List<MeasureGroupDimensionDef> dimensions() {
        return List.copyOf(dimensions);
    }

    private org.eclipse.daanse.olap.metadata.relational.CubeFacts facts;

    /** Where the group's facts lie in tables, where they are not the cube's (a measure group per fact table). */
    public MeasureGroupBuilder facts(org.eclipse.daanse.olap.metadata.relational.CubeFacts facts) {
        this.facts = facts;
        return this;
    }

    /** The table or join the group's fact rows are read from. */
    public MeasureGroupBuilder facts(org.eclipse.daanse.olap.metadata.relational.RelationalSource source) {
        return facts(org.eclipse.daanse.olap.metadata.relational.CubeFacts.of(source));
    }

    org.eclipse.daanse.olap.metadata.relational.CubeFacts facts() {
        return facts;
    }

    boolean ignoreUnrelated() {
        return ignoreUnrelated;
    }
}
