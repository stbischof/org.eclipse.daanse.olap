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

/** A cube: {@link PhysicalCubeDef} with facts of its own or {@link VirtualCubeDef} over others. Identity. */
public abstract sealed class CubeDef permits PhysicalCubeDef, VirtualCubeDef {

    private final String name;
    private final List<DimensionUsageDef> dimensions;
    private final List<CalculatedMemberDef> calculatedMembers;
    private final List<NamedSetDef> namedSets;
    private final List<KpiDef> kpis;
    private final String defaultMeasure;
    private final ElementInfo info;

    CubeDef(String name, List<DimensionUsageDef> dimensions, List<CalculatedMemberDef> calculatedMembers,
            List<NamedSetDef> namedSets, List<KpiDef> kpis, String defaultMeasure, ElementInfo info) {
        this.name = name;
        this.dimensions = List.copyOf(dimensions);
        this.calculatedMembers = List.copyOf(calculatedMembers);
        this.namedSets = List.copyOf(namedSets);
        this.kpis = List.copyOf(kpis);
        this.defaultMeasure = defaultMeasure;
        this.info = info;
    }

    public String name() {
        return name;
    }

    /** The dimensions of the cube, without the measures, which every cube has implicitly. */
    public List<DimensionUsageDef> dimensions() {
        return dimensions;
    }

    public List<CalculatedMemberDef> calculatedMembers() {
        return calculatedMembers;
    }

    public List<NamedSetDef> namedSets() {
        return namedSets;
    }

    public List<KpiDef> kpis() {
        return kpis;
    }

    /** The name of the default measure; {@code null} for the first one. */
    public String defaultMeasure() {
        return defaultMeasure;
    }

    public ElementInfo info() {
        return info;
    }

    /** The names of the measures a query sees, stored ones first. */
    public abstract List<String> measureNames();

    @Override
    public String toString() {
        return "cube " + name;
    }
}
