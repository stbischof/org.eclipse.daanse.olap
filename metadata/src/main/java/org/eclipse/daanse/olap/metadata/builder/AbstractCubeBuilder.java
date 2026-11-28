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
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.findings.Place;
import org.eclipse.daanse.olap.metadata.DescriptionRules;
import org.eclipse.daanse.olap.metadata.model.CalculatedMemberDef;
import org.eclipse.daanse.olap.metadata.model.DimensionDef;
import org.eclipse.daanse.olap.metadata.model.DimensionUsageDef;
import org.eclipse.daanse.olap.metadata.model.ElementInfo;
import org.eclipse.daanse.olap.metadata.model.KpiDef;
import org.eclipse.daanse.olap.metadata.model.NamedSetDef;

/** What a physical and a virtual cube have in common: dimensions, calculated members, sets, KPIs. */
public abstract class AbstractCubeBuilder<B extends AbstractCubeBuilder<B>> extends ElementBuilder<B> {

    /** A dimension of the cube before the shared dimensions of the catalog are known. */
    private record PendingUsage(String usageName, String sharedName, DimensionBuilder own, ElementInfo info) {
    }

    final List<Finding> problems;
    final Place path;
    private final List<PendingUsage> usages = new ArrayList<>();
    private final java.util.Map<String, org.eclipse.daanse.olap.metadata.relational.UsageJoin> joins = new java.util.HashMap<>();

    /**
     * How the cube's facts reach a dimension it uses: the fact column that holds the member key, and where the facts
     * stop above the leaves, the level.
     */
    @SuppressWarnings("unchecked")
    public B join(String usageName, org.eclipse.daanse.olap.metadata.relational.UsageJoin join) {
        joins.put(usageName, join);
        return (B) this;
    }
    final List<CalculatedMemberDef> calculatedMembers = new ArrayList<>();
    final List<NamedSetDef> namedSets = new ArrayList<>();
    final List<KpiDef> kpis = new ArrayList<>();
    String defaultMeasure;

    AbstractCubeBuilder(String name, List<Finding> problems, Place catalogPath) {
        super(name);
        this.problems = problems;
        this.path = catalogPath.then("cube", name);
    }

    /** Uses a shared dimension of the catalog under its own name. */
    public B dimension(String sharedName) {
        return dimension(sharedName, sharedName);
    }

    /** Uses a shared dimension of the catalog under another name, as "Ship Date" uses "Time". */
    public B dimension(String usageName, String sharedName) {
        usages.add(new PendingUsage(usageName, sharedName, null, ElementInfo.DEFAULT));
        return self();
    }

    /** Uses a shared dimension under another name, with a caption, description or visibility of its own. */
    public B dimension(String usageName, String sharedName, Consumer<UsageBuilder> usage) {
        UsageBuilder builder = new UsageBuilder(usageName);
        usage.accept(builder);
        usages.add(new PendingUsage(usageName, sharedName, null, builder.described()));
        return self();
    }

    /** A dimension of this cube alone. */
    public B dimension(String dimensionName, Consumer<DimensionBuilder> dimension) {
        DimensionBuilder builder = new DimensionBuilder(dimensionName, problems, path);
        dimension.accept(builder);
        usages.add(new PendingUsage(dimensionName, null, builder, ElementInfo.DEFAULT));
        return self();
    }

    public B calculatedMember(String memberName, String formula) {
        return calculatedMember(memberName, formula, m -> { });
    }

    /** A calculated member; a calculated measure unless the builder names another hierarchy. */
    public B calculatedMember(String memberName, String formula, Consumer<CalculatedMemberBuilder> member) {
        CalculatedMemberBuilder builder = new CalculatedMemberBuilder(memberName, formula);
        member.accept(builder);
        calculatedMembers.add(builder.build());
        return self();
    }

    public B namedSet(String setName, String formula) {
        return namedSet(setName, formula, (String) null);
    }

    static org.eclipse.daanse.olap.metadata.model.LocalizedText folder(String displayFolder) {
        return displayFolder == null ? null : org.eclipse.daanse.olap.metadata.model.LocalizedText.of(displayFolder);
    }

    /** A named set that clients show in a folder. */
    public B namedSet(String setName, String formula, String displayFolder) {
        namedSets.add(new NamedSetDef(setName, formula, folder(displayFolder), ElementInfo.DEFAULT));
        return self();
    }

    /** A named set in a folder with a description a client shows. */
    public B namedSet(String setName, String formula, String displayFolder, String description) {
        namedSets.add(new NamedSetDef(setName, formula, folder(displayFolder), description == null ? ElementInfo.DEFAULT
                : new ElementInfo(null, org.eclipse.daanse.olap.metadata.model.LocalizedText.of(description), true,
                        java.util.List.of())));
        return self();
    }

    /** A named set with what it says of itself: folder, caption, description, annotations. */
    public B namedSet(String setName, String formula, Consumer<NamedSetBuilder> set) {
        NamedSetBuilder builder = new NamedSetBuilder(setName, formula);
        set.accept(builder);
        namedSets.add(builder.build());
        return self();
    }

    public B kpi(String kpiName, Consumer<KpiBuilder> kpi) {
        KpiBuilder builder = new KpiBuilder(kpiName);
        kpi.accept(builder);
        kpis.add(builder.build());
        return self();
    }

    public B defaultMeasure(String measureName) {
        this.defaultMeasure = measureName;
        return self();
    }

    final List<DimensionUsageDef> resolveUsages(Function<String, DimensionDef> shared) {
        List<DimensionUsageDef> resolved = new ArrayList<>();
        for (PendingUsage usage : usages) {
            DimensionDef dimension;
            if (usage.own() != null) {
                dimension = usage.own().build();
            } else {
                dimension = shared.apply(usage.sharedName());
                if (dimension == null) {
                    problems.add(BuildFindings.of(DescriptionRules.M112, path.then("dimension", usage.usageName()),
                            "uses the shared dimension " + usage.sharedName() + ", which the catalog does not declare"));
                    continue;
                }
            }
            resolved.add(new DimensionUsageDef(usage.usageName(), dimension, usage.info(), joins.get(usage.usageName())));
        }
        return resolved;
    }
}
