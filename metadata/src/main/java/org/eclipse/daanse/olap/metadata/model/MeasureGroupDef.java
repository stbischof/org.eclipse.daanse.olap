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

import java.util.List;

/**
 * Measures that share their facts, and with them the dimensions they vary by.
 *
 * @param name       the name a client is told; it stands beside every measure of the group
 * @param measures   the measures read from those facts
 * @param dimensions the dimension usages the group varies by, each with the level its facts reach.
 *                   Empty means all of them, as finely as they go, which is what a cube with one group
 *                   says. A usage that is not named is one the group does not vary by: what a cell of such
 *                   a measure shows there is {@code ignoreUnrelatedDimensions}'
 * @param ignoreUnrelatedDimensions whether a member of a usage the group does not vary by is read as
 *                   the whole of it (the builders' default, as SSAS) instead of leaving the cell empty anywhere but
 *                   at the default member of that usage
 * @param facts      where the group's facts lie in tables, where they are not the cube's (a measure group per fact
 *                   table); {@code null} for the cube's
 */
public record MeasureGroupDef(String name, List<MeasureDef> measures, List<MeasureGroupDimensionDef> dimensions,
        boolean ignoreUnrelatedDimensions, org.eclipse.daanse.olap.metadata.relational.CubeFacts facts) {

    public MeasureGroupDef {
        measures = List.copyOf(measures);
        dimensions = List.copyOf(dimensions);
    }

    /** A group that reads the facts of its cube. */
    public MeasureGroupDef(String name, List<MeasureDef> measures, List<MeasureGroupDimensionDef> dimensions,
            boolean ignoreUnrelatedDimensions) {
        this(name, measures, dimensions, ignoreUnrelatedDimensions, null);
    }

    /** Whether this group varies by the usage of that name; a group that names none varies by all. */
    public boolean varies(String usage) {
        return dimensions.isEmpty() || dimensions.stream().anyMatch(d -> d.usage().equals(usage));
    }

    /** The level the facts of this group reach in that usage, where named; nothing: as finely as the dimension goes. */
    public java.util.Optional<String> granularityOf(String usage) {
        return dimensions.stream().filter(d -> d.usage().equals(usage)).map(MeasureGroupDimensionDef::granularity)
                .filter(java.util.Objects::nonNull).findFirst();
    }
}
