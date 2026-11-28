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
package org.eclipse.daanse.olap.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.findings.FindingPolicy;
import org.eclipse.daanse.olap.findings.Findings;
import org.eclipse.daanse.olap.findings.Layer;
import org.eclipse.daanse.olap.findings.Place;
import org.eclipse.daanse.olap.findings.Rule;
import org.eclipse.daanse.olap.metadata.model.AggregatorDef;
import org.eclipse.daanse.olap.metadata.model.CalculatedMemberDef;
import org.eclipse.daanse.olap.metadata.model.CatalogDef;
import org.eclipse.daanse.olap.metadata.model.CubeDef;
import org.eclipse.daanse.olap.metadata.model.CubeUsageDef;
import org.eclipse.daanse.olap.metadata.model.DimensionDef;
import org.eclipse.daanse.olap.metadata.model.DimensionKind;
import org.eclipse.daanse.olap.metadata.model.DimensionUsageDef;
import org.eclipse.daanse.olap.metadata.model.HierarchyDef;
import org.eclipse.daanse.olap.metadata.model.KpiDef;
import org.eclipse.daanse.olap.metadata.model.LevelDef;
import org.eclipse.daanse.olap.metadata.model.LevelKind;
import org.eclipse.daanse.olap.metadata.model.MeasureDef;
import org.eclipse.daanse.olap.metadata.model.MeasureGroupDef;
import org.eclipse.daanse.olap.metadata.model.MeasureGroupDimensionDef;
import org.eclipse.daanse.olap.metadata.model.MeasureUsageDef;
import org.eclipse.daanse.olap.metadata.model.NamedSetDef;
import org.eclipse.daanse.olap.metadata.model.ParentChildHierarchyDef;
import org.eclipse.daanse.olap.metadata.model.PhysicalCubeDef;
import org.eclipse.daanse.olap.metadata.model.RoleDef;
import org.eclipse.daanse.olap.metadata.model.VirtualCubeDef;

/** Holds a catalog description to the {@link DescriptionRules}; finds every breach, never stops at the first. */
final class ModelValidator {

    private static final String MEASURES = "Measures";

    /** The rule of a name used twice, by the kind of what is named. */
    private static final Map<String, Rule> TWICE = Map.ofEntries(Map.entry("shared dimension", DescriptionRules.M016),
            Map.entry("cube", DescriptionRules.M017), Map.entry("named set", DescriptionRules.M018),
            Map.entry("role", DescriptionRules.M019), Map.entry("parameter", DescriptionRules.M020),
            Map.entry("function", DescriptionRules.M021), Map.entry("hierarchy", DescriptionRules.M023),
            Map.entry("member property", DescriptionRules.M024), Map.entry("dimension", DescriptionRules.O15),
            Map.entry("measure", DescriptionRules.M025), Map.entry("KPI", DescriptionRules.M026),
            Map.entry("measure group", DescriptionRules.M027), Map.entry("action", DescriptionRules.M028));

    private final List<Finding> findings = new ArrayList<>();

    private ModelValidator() {
    }

    static List<Finding> validate(CatalogDef catalog) {
        ModelValidator validator = new ModelValidator();
        validator.catalog(catalog);
        return List.copyOf(validator.findings);
    }

    private void catalog(CatalogDef catalog) {
        Place path = Place.of("catalog", catalog.name());
        name(path, catalog.name());
        unique(path, "shared dimension", catalog.sharedDimensions(), DimensionDef::name);
        unique(path, "cube", catalog.cubes(), CubeDef::name);
        unique(path, "named set", catalog.namedSets(), NamedSetDef::name);
        for (NamedSetDef set : catalog.namedSets()) {
            name(path.then("named set", set.name()), set.name());
            text(DescriptionRules.M010, path.then("named set", set.name()), "formula", set.formula());
        }
        unique(path, "role", catalog.roles(), RoleDef::name);
        unique(path, "parameter", catalog.parameters(), p -> p.name());
        unique(path, "function", catalog.functions(), f -> f.name());
        catalog.functions().forEach(f -> {
            name(path.then("function", f.name()), f.name());
            if (f.body() == null) {
                problem(DescriptionRules.M011, path.then("function", f.name()), "has no body");
            }
        });
        Set<DimensionDef> seen = new HashSet<>();
        for (DimensionDef dimension : catalog.sharedDimensions()) {
            dimension(path, dimension, seen);
        }
        for (CubeDef cube : catalog.cubes()) {
            cube(path, cube, catalog, seen);
        }
        // without regard to case, as the check of duplicate role names and the lookup of a role
        if (catalog.defaultRole() != null
                && catalog.roles().stream().noneMatch(r -> r.name().equalsIgnoreCase(catalog.defaultRole()))) {
            problem(DescriptionRules.O8, path,
                    "default role " + catalog.defaultRole() + " is not a role of the catalog");
        }
        for (RoleDef role : catalog.roles()) {
            Place rolePath = path.then("role", role.name());
            name(rolePath, role.name());
            if (!role.unionOf().isEmpty() && !role.cubeGrants().isEmpty()) {
                problem(DescriptionRules.M040, rolePath,
                        "combines other roles and has grants of its own; it is one or the other");
            }
            for (RoleDef part : role.unionOf()) {
                if (!catalog.roles().contains(part)) {
                    problem(DescriptionRules.O8, rolePath,
                            "combines role " + part.name() + ", which is not a role of the catalog");
                }
            }
        }
    }

    private void dimension(Place parent, DimensionDef dimension, Set<DimensionDef> seen) {
        if (!seen.add(dimension)) {
            return; // a shared dimension is checked once, however often it is used
        }
        Place path = parent.then("dimension", dimension.name());
        name(path, dimension.name());
        if (dimension.hierarchies().isEmpty()) {
            problem(DescriptionRules.M031, path, "has no hierarchy");
        }
        unique(path, "hierarchy", dimension.hierarchies(), HierarchyDef::name);
        for (HierarchyDef hierarchy : dimension.hierarchies()) {
            Place hierarchyPath = path.then("hierarchy", hierarchy.name());
            name(hierarchyPath, hierarchy.name());
            if (hierarchy instanceof ParentChildHierarchyDef parentChild && parentChild.level() == null) {
                problem(DescriptionRules.M032, hierarchyPath, "a parent-child hierarchy has exactly one level");
            } else if (hierarchy.levels().isEmpty()) {
                // in Mondrian's words
                problem(DescriptionRules.M033, hierarchyPath, "Hierarchy '" + hierarchyUniqueName(dimension, hierarchy)
                        + "' must have at least one level.");
            }
            // a level name twice in one hierarchy, in Mondrian's words
            Set<String> levelNames = new HashSet<>();
            Set<String> reportedLevels = new HashSet<>();
            for (LevelDef level : hierarchy.levels()) {
                String key = level.name() == null ? null : level.name().toLowerCase(java.util.Locale.ROOT);
                if (key != null && !levelNames.add(key) && reportedLevels.add(key)) {
                    problem(DescriptionRules.M029, hierarchyPath,
                            "Level names within hierarchy '" + hierarchyUniqueName(dimension, hierarchy)
                            + "' are not unique; there is more than one level with name '" + level.name() + "'.");
                }
            }
            for (LevelDef level : hierarchy.levels()) {
                Place levelPath = hierarchyPath.then("level", level.name());
                name(levelPath, level.name());
                if (level.kind() != LevelKind.REGULAR && dimension.kind() != DimensionKind.TIME) {
                    problem(DescriptionRules.M034, levelPath,
                            "is of kind " + level.kind() + ", but its dimension is not a time dimension");
                }
                unique(levelPath, "member property", level.properties(), p -> p.name());
            }
        }
    }

    /** [Dimension].[Hierarchy], as a message about the hierarchy names it. */
    private static String hierarchyUniqueName(DimensionDef dimension, HierarchyDef hierarchy) {
        return "[" + dimension.name() + "].[" + hierarchy.name() + "]";
    }

    private void cube(Place parent, CubeDef cube, CatalogDef catalog, Set<DimensionDef> seen) {
        Place path = parent.then("cube", cube.name());
        name(path, cube.name());
        unique(path, "dimension", cube.dimensions(), DimensionUsageDef::name);
        for (DimensionUsageDef usage : cube.dimensions()) {
            Place usagePath = path.then("dimension", usage.name());
            name(usagePath, usage.name());
            // the name under which the cube uses the dimension is the one a query writes
            if (MEASURES.equalsIgnoreCase(usage.name())) {
                problem(DescriptionRules.M035, usagePath, "the name Measures is taken by the measures every cube has");
            }
            if (usage.dimension() == null) {
                problem(DescriptionRules.M036, usagePath, "names no dimension");
            } else {
                dimension(path, usage.dimension(), seen);
            }
        }
        List<String> measureNames = new ArrayList<>(cube.measureNames());
        for (CalculatedMemberDef member : cube.calculatedMembers()) {
            Place memberPath = path.then("calculated member", member.name());
            name(memberPath, member.name());
            // in Mondrian's words
            if (member.formula() == null || member.formula().isBlank()) {
                problem(DescriptionRules.M012, memberPath,
                        "The calculated member '" + member.name() + "' in cube '" + cube.name()
                        + "' has no formula");
            }
            text(DescriptionRules.M013, memberPath, "hierarchy", member.hierarchy());
            if (isMeasures(member.hierarchy()) && member.parent() == null) {
                measureNames.add(member.name());
            }
        }
        duplicates(path, "measure", measureNames);
        // a cube needs a stored measure; a catalog may declare no cube and describe a database only
        if (cube.measureNames().isEmpty()) {
            problem(DescriptionRules.M039, path, "has no measure");
        }
        if (cube.defaultMeasure() != null
                && measureNames.stream().noneMatch(m -> cube.defaultMeasure().equalsIgnoreCase(m))) {
            problem(DescriptionRules.O4, path,
                    "default measure " + cube.defaultMeasure() + " is not a measure of the cube");
        }
        if (cube instanceof PhysicalCubeDef physical) {
            for (MeasureGroupDef group : physical.measureGroups()) {
                for (MeasureGroupDimensionDef varies : group.dimensions()) {
                    Place groupPath = path.then("measure group", group.name());
                    DimensionUsageDef used = cube.dimensions().stream()
                            .filter(u -> u.name().equals(varies.usage())).findFirst().orElse(null);
                    if (used == null) {
                        problem(DescriptionRules.O5, groupPath,
                                "varies by " + varies.usage() + ", which the cube does not use");
                    } else if (varies.granularity() != null && !hasLevel(used, varies.granularity())) {
                        problem(DescriptionRules.O3, groupPath,
                                "reaches " + varies.granularity() + " in " + varies.usage()
                                + ", which is no level of it");
                    }
                }
                List<String> distinct = group.measures().stream()
                        .filter(m -> m.aggregator() == AggregatorDef.Standard.DISTINCT_COUNT)
                        .map(MeasureDef::name).toList();
                if (distinct.size() > 1) {
                    problem(DescriptionRules.M041, path.then("measure group", group.name()),
                            "counts distinct values more than once ("
                            + String.join(", ", distinct) + "); measures that count distinct values each need"
                            + " a group of their own");
                }
            }
        }
        if (cube instanceof PhysicalCubeDef physical) {
            relational(path, physical);
        }
        unique(path, "named set", cube.namedSets(), NamedSetDef::name);
        for (NamedSetDef set : cube.namedSets()) {
            name(path.then("named set", set.name()), set.name());
            text(DescriptionRules.M010, path.then("named set", set.name()), "formula", set.formula());
        }
        unique(path, "KPI", cube.kpis(), KpiDef::name);
        for (KpiDef kpi : cube.kpis()) {
            Place kpiPath = path.then("KPI", kpi.name());
            name(kpiPath, kpi.name());
            text(DescriptionRules.M014, kpiPath, "value", kpi.value());
            if (kpi.parentKpi() != null && cube.kpis().stream().noneMatch(k -> k.name().equals(kpi.parentKpi()))) {
                problem(DescriptionRules.M045, kpiPath, "parent KPI " + kpi.parentKpi() + " is not a KPI of the cube");
            }
        }
        kpiRings(path, cube.kpis());
        switch (cube) {
        case PhysicalCubeDef physical -> physical(path, physical);
        case VirtualCubeDef virtual -> virtual(path, virtual, catalog);
        }
    }

    private void physical(Place path, PhysicalCubeDef cube) {
        unique(path, "measure group", cube.measureGroups(), MeasureGroupDef::name);
        cube.measures().forEach(m -> {
            name(path.then("measure", m.name()), m.name());
            if (m.aggregator() == null) {
                problem(DescriptionRules.M047, path.then("measure", m.name()), "has no aggregator");
            }
        });
        long defaults = cube.actions().stream()
                .filter(a -> a instanceof org.eclipse.daanse.olap.metadata.model.ActionDef.DrillThrough d
                        && d.isDefault())
                .count();
        if (defaults > 1) {
            problem(DescriptionRules.M048, path,
                    "has " + defaults + " default drill-through actions; at most one is the default");
        }
        unique(path, "action", cube.actions(), a -> a.name());
        for (org.eclipse.daanse.olap.metadata.model.ActionDef action : cube.actions()) {
            Place at = path.then("action", action.name());
            name(at, action.name());
            if (action instanceof org.eclipse.daanse.olap.metadata.model.ActionDef.DrillThrough drill) {
                drillThroughReturns(at, drill, cube);
            }
        }
    }

    /**
     * What a drill-through returns is checked here, every problem at once and with its path, rather than
     * when the cube is bound: a stored measure, a level or a level property of the cube.
     */
    private void drillThroughReturns(Place at,
            org.eclipse.daanse.olap.metadata.model.ActionDef.DrillThrough drill, PhysicalCubeDef cube) {
        if (drill.returns() == null) {
            problem(DescriptionRules.M049, at, "has no list of what it returns");
            return;
        }
        for (String mdx : drill.returns()) {
            if (mdx == null) {
                problem(DescriptionRules.M050, at, "returns a null entry");
                continue;
            }
            List<String> segments = segments(mdx);
            if (segments.size() == 2 && "Measures".equalsIgnoreCase(segments.get(0))) {
                String measure = segments.get(1);
                if (cube.measures().stream().anyMatch(m -> measure.equalsIgnoreCase(m.name()))) {
                    continue;
                }
                if (cube.calculatedMembers().stream().anyMatch(c -> measure.equalsIgnoreCase(c.name()))) {
                    problem(DescriptionRules.M051, at,
                            "returns the calculated measure " + mdx + "; a drill-through returns stored measures only");
                    continue;
                }
            } else if (segments.size() == 3 || segments.size() == 4) {
                if (levelOrProperty(cube, segments)) {
                    continue;
                }
            }
            problem(DescriptionRules.M052, at,
                    "returns " + mdx + ", which is no level, level property or stored measure of this cube");
        }
    }

    /** [Dimension].[Hierarchy].[Level] or [Dimension].[Hierarchy].[Level].[Property], names without case. */
    private static boolean levelOrProperty(PhysicalCubeDef cube, List<String> segments) {
        for (DimensionUsageDef usage : cube.dimensions()) {
            if (!segments.get(0).equalsIgnoreCase(usage.name()) || usage.dimension() == null) {
                continue;
            }
            for (var hierarchy : usage.dimension().hierarchies()) {
                String hierarchyName = hierarchy.name() == null ? usage.name() : hierarchy.name();
                if (!segments.get(1).equalsIgnoreCase(hierarchyName)) {
                    continue;
                }
                for (var level : hierarchy.levels()) {
                    if (!segments.get(2).equalsIgnoreCase(level.name())) {
                        continue;
                    }
                    if (segments.size() == 3) {
                        return true;
                    }
                    return level.properties().stream().anyMatch(p -> segments.get(3).equalsIgnoreCase(p.name()));
                }
            }
        }
        return false;
    }

    /** The names of an identifier like [A].[B].[C], a doubled ] read as one. */
    private static List<String> segments(String mdx) {
        List<String> names = new ArrayList<>();
        int i = 0;
        while (i < mdx.length()) {
            if (mdx.charAt(i) != '[') {
                i++;
                continue;
            }
            StringBuilder name = new StringBuilder();
            i++;
            while (i < mdx.length()) {
                char ch = mdx.charAt(i);
                if (ch == ']' && i + 1 < mdx.length() && mdx.charAt(i + 1) == ']') {
                    name.append(']');
                    i += 2;
                } else if (ch == ']') {
                    i++;
                    break;
                } else {
                    name.append(ch);
                    i++;
                }
            }
            names.add(name.toString());
        }
        return names;
    }

    /** KPIs whose parents lead back to one of themselves; a client can draw none of them. */
    private void kpiRings(Place path, List<KpiDef> kpis) {
        java.util.Map<String, KpiDef> byName = new java.util.LinkedHashMap<>();
        kpis.forEach(kpi -> byName.putIfAbsent(kpi.name(), kpi));
        Set<Set<String>> reported = new HashSet<>();
        for (KpiDef start : kpis) {
            List<String> walked = new ArrayList<>();
            KpiDef current = start;
            while (current != null && !walked.contains(current.name())) {
                walked.add(current.name());
                current = current.parentKpi() == null ? null : byName.get(current.parentKpi());
            }
            if (current == null) {
                continue;
            }
            List<String> ring = walked.subList(walked.indexOf(current.name()), walked.size());
            if (reported.add(new HashSet<>(ring))) {
                problem(DescriptionRules.M046, path,
                        "the KPIs " + String.join(", ", ring) + " name each other as parent in a ring");
            }
        }
    }

    private void virtual(Place path, VirtualCubeDef cube, CatalogDef catalog) {
        if (cube.cubes().isEmpty()) {
            problem(DescriptionRules.O10, path, "a virtual cube draws from at least one cube");
        }
        for (CubeUsageDef usage : cube.cubes()) {
            if (!catalog.cubes().contains(usage.cube())) {
                problem(DescriptionRules.M077, path,
                        "draws from cube " + usage.cube().name() + ", which is not a cube of the catalog");
            }
        }
        for (MeasureUsageDef usage : cube.measures()) {
            Place usagePath = path.then("measure", usage.name());
            if (cube.cubes().stream().noneMatch(c -> c.cube() == usage.cube())) {
                problem(DescriptionRules.M078, usagePath,
                        "comes from cube " + usage.cube().name() + ", which the virtual cube does not draw from");
            } else if (!usage.cube().measures().contains(usage.measure())) {
                problem(DescriptionRules.O10, usagePath, "is not a measure of cube " + usage.cube().name());
            }
        }
        for (DimensionUsageDef usage : cube.dimensions()) {
            boolean shared = usage.dimension() != null && cube.cubes().stream().anyMatch(
                    c -> c.cube().dimensions().stream().anyMatch(d -> d.dimension() == usage.dimension()));
            if (!shared) {
                problem(DescriptionRules.M079, path.then("dimension", usage.name()),
                        "is a dimension of none of the cubes drawn from");
            }
        }
    }

    private static boolean isMeasures(String hierarchy) {
        return hierarchy != null && hierarchy.replace("[", "").replace("]", "").trim().equalsIgnoreCase(MEASURES);
    }

    /**
     * A raw name carries whatever characters it likes, a closing bracket included: MDX addresses
     * it by doubling that bracket. Only a name that is not there at all cannot be addressed.
     */
    private void name(Place path, String name) {
        if (name == null || name.isBlank()) {
            problem(DescriptionRules.M001, path, "has no name");
        }
    }

    private void text(Rule rule, Place path, String what, String mdx) {
        if (mdx == null || mdx.isBlank()) {
            problem(rule, path, "has no " + what);
        }
    }

    private <T> void unique(Place path, String what, List<T> elements, Function<T, String> name) {
        duplicates(path, what, elements.stream().map(name).toList());
    }

    /** One clash is one problem, however many elements of that name follow the first. */
    private void duplicates(Place path, String what, List<String> names) {
        Set<String> seen = new HashSet<>();
        Set<String> reported = new HashSet<>();
        for (String name : names) {
            String key = name == null ? null : name.toLowerCase(java.util.Locale.ROOT);
            if (key != null && !seen.add(key) && reported.add(key)) {
                // a parameter in Mondrian's words, the rest in ours
                problem(java.util.Objects.requireNonNull(TWICE.get(what), what), path,
                        "parameter".equals(what) ? "Duplicate parameter '" + name + "' in schema"
                        : "has more than one " + what + " named " + name);
            }
        }
    }

    /**
     * The relational half of a cube: every column it names lies in a table of the source that reads it -- the cube's
     * facts for measures and foreign keys, a hierarchy's rows (or the facts, where the hierarchy names none) for its
     * levels, properties and parent column. A source of SQL text is not checked; its columns are what the SQL says.
     * A cube without any binding reads no tables and is not checked.
     */
    private void relational(Place path, PhysicalCubeDef cube) {
        Set<org.eclipse.daanse.olap.metadata.relational.TableDef> facts = cube.facts().map(f -> tablesOf(f.source())).orElse(null);
        boolean factsNamed = cube.facts().isPresent();
        for (DimensionUsageDef usage : cube.dimensions()) {
            Place usagePath = path.then("dimension", usage.name());
            if (usage.join() != null) {
                if (!factsNamed) {
                    problem(DescriptionRules.M080, usagePath,
                            "joins by a foreign key, but the cube names no fact table");
                }
                usage.join().foreignKeyColumn().ifPresent(key -> inTables(usagePath, "foreign key", key, facts));
            }
            if (usage.dimension() == null) {
                continue;
            }
            for (HierarchyDef hierarchy : usage.dimension().hierarchies()) {
                Place hierarchyPath = usagePath.then("hierarchy", hierarchy.name());
                var own = hierarchy.rows().flatMap(org.eclipse.daanse.olap.metadata.relational.HierarchyRows::ownSource);
                // not own.map(tablesOf): SQL text has no known tables (null), which must not fall back to the facts
                Set<org.eclipse.daanse.olap.metadata.relational.TableDef> rows = own.isPresent() ? tablesOf(own.get()) : facts;
                boolean rowsNamed = own.isPresent() || factsNamed;
                if (hierarchy instanceof ParentChildHierarchyDef parentChild) {
                    parentChild.columns().ifPresent(c -> inTables(hierarchyPath, "parent column", c.parent(), rows));
                }
                for (LevelDef level : hierarchy.levels()) {
                    Place levelPath = hierarchyPath.then("level", level.name());
                    if (level.columns().isPresent() && !rowsNamed) {
                        problem(DescriptionRules.M081, levelPath,
                                "reads columns, but neither its hierarchy nor the cube names a table");
                        continue;
                    }
                    level.columns().ifPresent(columns -> {
                        columns.keys().forEach(key -> inTables(levelPath, "key column", key, rows));
                        columns.nameColumn().ifPresent(c -> inTables(levelPath, "name column", c, rows));
                        columns.captionColumn().ifPresent(c -> inTables(levelPath, "caption column", c, rows));
                    });
                    for (var property : level.properties()) {
                        if (property.column() != null) {
                            inTables(levelPath.then("member property", property.name()), "column", property.column(),
                                    rows);
                        }
                    }
                }
            }
        }
        for (MeasureGroupDef group : cube.measureGroups()) {
            Set<org.eclipse.daanse.olap.metadata.relational.TableDef> source = group.facts() == null ? facts
                    : tablesOf(group.facts().source());
            for (MeasureDef measure : group.measures()) {
                measure.column().ifPresent(c -> inTables(path.then("measure", measure.name()), "column", c, source));
            }
        }
        cube.facts().ifPresent(f -> {
            for (var table : f.aggregationTables()) {
                if (table instanceof org.eclipse.daanse.olap.metadata.relational.AggregationTableDef.ByName byName
                        && (byName.columns() == null || byName.columns().factCount() == null)) {
                    problem(DescriptionRules.M085, path.then("aggregation table", String.valueOf(byName.table())),
                            "has no fact count column");
                }
            }
            f.writebackTable().ifPresent(writeback -> {
                Place writebackPath = path.then("writeback table", writeback.name());
                for (var attribute : writeback.attributes()) {
                    if (cube.dimensions().stream().noneMatch(u -> u.name().equalsIgnoreCase(attribute.usage()))) {
                        problem(DescriptionRules.M086, writebackPath,
                                "writes " + attribute.usage() + ", which the cube does not use");
                    }
                }
                for (var measure : writeback.measures()) {
                    if (cube.measureNames().stream().noneMatch(m -> m.equalsIgnoreCase(measure.measure()))) {
                        problem(DescriptionRules.M087, writebackPath,
                                "writes " + measure.measure() + ", which is no measure of the cube");
                    }
                }
            });
        });
    }

    /** The tables a source reads; {@code null} for SQL text, whose tables are not known. */
    private static Set<org.eclipse.daanse.olap.metadata.relational.TableDef> tablesOf(org.eclipse.daanse.olap.metadata.relational.RelationalSource source) {
        Set<org.eclipse.daanse.olap.metadata.relational.TableDef> tables = new HashSet<>();
        return collect(source, tables) ? tables : null;
    }

    private static boolean collect(org.eclipse.daanse.olap.metadata.relational.RelationalSource source, Set<org.eclipse.daanse.olap.metadata.relational.TableDef> tables) {
        return switch (source) {
        case org.eclipse.daanse.olap.metadata.relational.RelationalSource.Table t -> tables.add(t.table()) || true;
        case org.eclipse.daanse.olap.metadata.relational.RelationalSource.Inline i -> tables.add(i.table().table()) || true;
        case org.eclipse.daanse.olap.metadata.relational.RelationalSource.Join j -> collect(j.left().source(), tables) && collect(j.right().source(), tables);
        case org.eclipse.daanse.olap.metadata.relational.RelationalSource.SqlSelect s -> false;
        };
    }

    /** A column of a table must lie in one of the tables; an expression or a field without table is not checked. */
    private void inTables(Place path, String what, org.eclipse.daanse.olap.metadata.relational.ColumnDef column, Set<org.eclipse.daanse.olap.metadata.relational.TableDef> tables) {
        if (tables != null && column instanceof org.eclipse.daanse.olap.metadata.relational.PhysicalColumnDef physical && physical.table() != null
                && !tables.contains(physical.table())) {
            // a level's column outside its hierarchy's tables, in Mondrian's words too
            String mondrian = what.endsWith(" column") && path.passes("level")
                    ? ": Table '" + physical.table().name() + "' not found" : "";
            problem(DescriptionRules.M083, path, what + " " + physical + " lies in none of the tables it is read from ("
                    + tables.stream().map(Object::toString).sorted().toList() + ")" + mondrian);
        }
    }

    private void problem(Rule rule, Place path, String message) {
        findings.add(Findings.of(FindingPolicy.NONE, rule, Layer.DESCRIPTION, path, message));
    }

    /** Whether the dimension a cube uses has a level of that name, in any of its hierarchies. */
    private static boolean hasLevel(DimensionUsageDef usage, String level) {
        return usage.dimension() != null && usage.dimension().hierarchies().stream()
                .flatMap(h -> h.levels().stream()).anyMatch(l -> level.equals(l.name()));
    }
}
