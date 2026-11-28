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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.findings.Place;
import org.eclipse.daanse.olap.metadata.DescriptionRules;
import org.eclipse.daanse.olap.metadata.model.ActionDef;
import org.eclipse.daanse.olap.metadata.model.CachePolicyDef;
import org.eclipse.daanse.olap.metadata.model.DimensionDef;
import org.eclipse.daanse.olap.metadata.model.ElementInfo;
import org.eclipse.daanse.olap.metadata.model.MeasureDef;
import org.eclipse.daanse.olap.metadata.model.MeasureGroupDef;
import org.eclipse.daanse.olap.metadata.model.PhysicalCubeDef;

public final class CubeBuilder extends AbstractCubeBuilder<CubeBuilder> {

    private final Map<String, List<MeasureDef>> measureGroups = new LinkedHashMap<>();
    private final Map<String, MeasureGroupBuilder> groupTraits = new LinkedHashMap<>();
    private final List<ActionDef> actions = new ArrayList<>();
    private CachePolicyDef cachePolicy = CachePolicyDef.DEFAULT;

    CubeBuilder(String name, List<Finding> problems, Place catalogPath) {
        super(name, problems, catalogPath);
    }

    /** A summed numeric measure in the measure group named like the cube. */
    public CubeBuilder measure(String measureName) {
        return measure(measureName, m -> { });
    }

    public CubeBuilder measure(String measureName, Consumer<MeasureBuilder> measure) {
        return measure(name, measureName, measure);
    }

    /** A measure in the named measure group; measures of one group share their facts. */
    public CubeBuilder measure(String groupName, String measureName, Consumer<MeasureBuilder> measure) {
        MeasureBuilder builder = new MeasureBuilder(measureName);
        measure.accept(builder);
        measureGroups.computeIfAbsent(groupName, g -> new ArrayList<>()).add(builder.build());
        return this;
    }

    /**
     * A drill-through action.
     *
     * @param returns MDX text naming the levels, properties and measures to return
     */
    public CubeBuilder drillThrough(String actionName, boolean isDefault, String... returns) {
        // a null among the returns reaches the model check, which names it
        actions.add(new ActionDef.DrillThrough(actionName, isDefault,
                returns == null ? null : java.util.Arrays.asList(returns), ElementInfo.DEFAULT));
        return this;
    }

    /** A drill-through action that carries a caption and a description of its own. */
    public CubeBuilder drillThrough(String actionName, Consumer<DrillThroughBuilder> action) {
        DrillThroughBuilder builder = new DrillThroughBuilder(actionName);
        action.accept(builder);
        actions.add(builder.build());
        return this;
    }

    private String factSet;
    private java.util.function.Predicate<org.eclipse.daanse.olap.metadata.model.FactRow> factFilter;

    /**
     * Reads the facts of a set shared with other cubes, the rows the filter keeps -- two cubes over one set of facts,
     * each with its own rows.
     */
    public CubeBuilder facts(String factSet,
            java.util.function.Predicate<org.eclipse.daanse.olap.metadata.model.FactRow> filter) {
        this.factSet = factSet;
        this.factFilter = filter;
        return this;
    }

    public CubeBuilder cache(CachePolicyDef cachePolicy) {
        this.cachePolicy = cachePolicy;
        return this;
    }

    /**
     * What a measure group varies by, and what it does about the dimensions it does not.
     *
     * <p>A group that is not described this way varies by every dimension of the cube, which is what a
     * cube with one group means.
     */
    public CubeBuilder measureGroup(String groupName, Consumer<MeasureGroupBuilder> group) {
        MeasureGroupBuilder builder = groupTraits.computeIfAbsent(groupName, MeasureGroupBuilder::new);
        group.accept(builder);
        measureGroups.computeIfAbsent(groupName, g -> new ArrayList<>());
        return this;
    }

    private org.eclipse.daanse.olap.metadata.relational.CubeFacts tables;
    private final List<org.eclipse.daanse.olap.metadata.relational.AggregationTableDef> aggregationTables = new java.util.ArrayList<>();
    private final List<org.eclipse.daanse.olap.metadata.relational.AggregationExcludeDef> aggregationExcludes = new java.util.ArrayList<>();
    private org.eclipse.daanse.olap.metadata.relational.WritebackTableDef writeback;

    /** A table that holds the cube's facts already summed up, matched by name or by pattern. */
    public CubeBuilder aggregationTable(org.eclipse.daanse.olap.metadata.relational.AggregationTableDef table) {
        aggregationTables.add(table);
        return this;
    }

    /** A table the pattern matching of aggregation tables leaves out. */
    public CubeBuilder aggregationExclude(org.eclipse.daanse.olap.metadata.relational.AggregationExcludeDef exclude) {
        aggregationExcludes.add(exclude);
        return this;
    }

    /** The table written values go to. */
    public CubeBuilder writeback(org.eclipse.daanse.olap.metadata.relational.WritebackTableDef table) {
        this.writeback = table;
        return this;
    }

    /** The facts with what the cube adds to them; aggregation tables and writeback need a fact table. */
    private org.eclipse.daanse.olap.metadata.relational.CubeFacts tables() {
        boolean added = !aggregationTables.isEmpty() || !aggregationExcludes.isEmpty() || writeback != null;
        if (!added) {
            return tables;
        }
        if (tables == null) {
            problems.add(BuildFindings.of(DescriptionRules.M111, path,
                    "names aggregation tables or a writeback table, but no facts"));
            return null;
        }
        List<org.eclipse.daanse.olap.metadata.relational.AggregationTableDef> allTables = new java.util.ArrayList<>(tables.aggregationTables());
        allTables.addAll(aggregationTables);
        List<org.eclipse.daanse.olap.metadata.relational.AggregationExcludeDef> allExcludes = new java.util.ArrayList<>(tables.aggregationExcludes());
        allExcludes.addAll(aggregationExcludes);
        return new org.eclipse.daanse.olap.metadata.relational.CubeFacts(tables.source(), allTables, allExcludes,
                writeback != null ? writeback : tables.writebackTable().orElse(null));
    }

    /** Where the cube's facts lie in tables. */
    public CubeBuilder facts(org.eclipse.daanse.olap.metadata.relational.CubeFacts facts) {
        this.tables = facts;
        return this;
    }

    /** The table or join the cube's fact rows are read from. */
    public CubeBuilder facts(org.eclipse.daanse.olap.metadata.relational.RelationalSource source) {
        return facts(org.eclipse.daanse.olap.metadata.relational.CubeFacts.of(source));
    }

    PhysicalCubeDef build(Function<String, DimensionDef> shared) {
        List<MeasureGroupDef> groups = measureGroups.entrySet().stream()
                .map(e -> {
                    MeasureGroupBuilder traits = groupTraits.get(e.getKey());
                    return new MeasureGroupDef(e.getKey(), e.getValue(),
                            traits == null ? List.of() : traits.dimensions(),
                            traits == null || traits.ignoreUnrelated(), traits == null ? null : traits.facts());
                }).toList();
        return new PhysicalCubeDef(name, resolveUsages(shared), groups, calculatedMembers, namedSets, kpis, actions,
                defaultMeasure, cachePolicy, info(), factSet, factFilter, tables());
    }
}
