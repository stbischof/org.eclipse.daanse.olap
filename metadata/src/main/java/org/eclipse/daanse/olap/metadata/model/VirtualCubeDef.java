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

/** A cube that combines measures of other cubes over the dimensions they share. */
public final class VirtualCubeDef extends CubeDef {

    private final List<CubeUsageDef> cubes;
    private final List<MeasureUsageDef> measures;

    public VirtualCubeDef(String name, List<CubeUsageDef> cubes, List<DimensionUsageDef> dimensions,
            List<MeasureUsageDef> measures, List<CalculatedMemberDef> calculatedMembers,
            List<NamedSetDef> namedSets, List<KpiDef> kpis, String defaultMeasure, ElementInfo info) {
        super(name, dimensions, calculatedMembers, namedSets, kpis, defaultMeasure, info);
        this.cubes = List.copyOf(cubes);
        this.measures = List.copyOf(measures);
    }

    public List<CubeUsageDef> cubes() {
        return cubes;
    }

    public List<MeasureUsageDef> measures() {
        return measures;
    }

    @Override
    public List<String> measureNames() {
        return measures.stream().map(MeasureUsageDef::name).toList();
    }
}
