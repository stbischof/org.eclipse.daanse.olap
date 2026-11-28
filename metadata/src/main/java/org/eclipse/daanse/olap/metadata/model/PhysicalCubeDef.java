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

/** A cube with facts of its own. */
public final class PhysicalCubeDef extends CubeDef {

    private final List<MeasureGroupDef> measureGroups;
    private final List<ActionDef> actions;
    private final CachePolicyDef cachePolicy;
    private final String factSet;
    private final java.util.function.Predicate<FactRow> factFilter;
    private final org.eclipse.daanse.olap.metadata.relational.CubeFacts facts;

    public PhysicalCubeDef(String name, List<DimensionUsageDef> dimensions, List<MeasureGroupDef> measureGroups,
            List<CalculatedMemberDef> calculatedMembers, List<NamedSetDef> namedSets, List<KpiDef> kpis,
            List<ActionDef> actions, String defaultMeasure, CachePolicyDef cachePolicy, ElementInfo info) {
        this(name, dimensions, measureGroups, calculatedMembers, namedSets, kpis, actions, defaultMeasure, cachePolicy,
                info, null, null);
    }

    public PhysicalCubeDef(String name, List<DimensionUsageDef> dimensions, List<MeasureGroupDef> measureGroups,
            List<CalculatedMemberDef> calculatedMembers, List<NamedSetDef> namedSets, List<KpiDef> kpis,
            List<ActionDef> actions, String defaultMeasure, CachePolicyDef cachePolicy, ElementInfo info,
            String factSet, java.util.function.Predicate<FactRow> factFilter) {
        this(name, dimensions, measureGroups, calculatedMembers, namedSets, kpis, actions, defaultMeasure, cachePolicy,
                info, factSet, factFilter, null);
    }

    public PhysicalCubeDef(String name, List<DimensionUsageDef> dimensions, List<MeasureGroupDef> measureGroups,
            List<CalculatedMemberDef> calculatedMembers, List<NamedSetDef> namedSets, List<KpiDef> kpis,
            List<ActionDef> actions, String defaultMeasure, CachePolicyDef cachePolicy, ElementInfo info,
            String factSet, java.util.function.Predicate<FactRow> factFilter, org.eclipse.daanse.olap.metadata.relational.CubeFacts facts) {
        super(name, dimensions, calculatedMembers, namedSets, kpis, defaultMeasure, info);
        this.facts = facts;
        this.factSet = factSet == null ? name : factSet;
        this.factFilter = factFilter;
        this.measureGroups = List.copyOf(measureGroups);
        this.actions = List.copyOf(actions);
        this.cachePolicy = cachePolicy;
    }

    public List<MeasureGroupDef> measureGroups() {
        return measureGroups;
    }

    /** All stored measures, group by group. */
    public List<MeasureDef> measures() {
        return measureGroups.stream().flatMap(g -> g.measures().stream()).toList();
    }

    public List<ActionDef> actions() {
        return actions;
    }

    /**
     * The set of facts the cube reads: its own name, or a set it shares with other cubes, each keeping the rows of
     * its {@link #factFilter()} -- Mondrian's table filter, a PhysView, SSAS' partition binding.
     */
    public String factSet() {
        return factSet;
    }

    /** Which facts of its set the cube reads; {@code null} for all of them. */
    public java.util.function.Predicate<FactRow> factFilter() {
        return factFilter;
    }

    /** Where the cube's facts lie in tables; empty for a cube whose facts come through a source of its own. */
    public java.util.Optional<org.eclipse.daanse.olap.metadata.relational.CubeFacts> facts() {
        return java.util.Optional.ofNullable(facts);
    }

    public CachePolicyDef cachePolicy() {
        return cachePolicy;
    }

    @Override
    public List<String> measureNames() {
        return measures().stream().map(MeasureDef::name).toList();
    }
}
