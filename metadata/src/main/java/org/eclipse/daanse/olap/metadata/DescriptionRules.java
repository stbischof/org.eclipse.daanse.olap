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

import java.util.EnumSet;
import java.util.List;

import org.eclipse.daanse.olap.findings.Consequence;
import org.eclipse.daanse.olap.findings.Rule;
import org.eclipse.daanse.olap.findings.RuleId;
import org.eclipse.daanse.olap.findings.Severity;

/**
 * The rules a catalog description is held to when it is built: the family {@code description}, and the family
 * {@code olap} for rules any description of an OLAP catalog shares. Each rule says what it asks in one sentence; a
 * finding of any of them is an error that stops the build, and no policy may give it another consequence.
 */
public final class DescriptionRules {

    public static final Rule M001 = stops("description/M001",
            "Every named object of a catalog has a name that is not empty.");

    public static final Rule M010 = stops("description/M010", "A named set has a formula.");

    public static final Rule M011 = stops("description/M011", "A function has a body.");

    public static final Rule M012 = stops("description/M012", "A calculated member has a formula.");

    public static final Rule M013 = stops("description/M013", "A calculated member names the hierarchy it belongs to.");

    public static final Rule M014 = stops("description/M014", "A KPI has a value expression.");

    public static final Rule M016 = stops("description/M016",
            "A catalog has no two shared dimensions with the same name.");

    public static final Rule M017 = stops("description/M017", "A catalog has no two cubes with the same name.");

    public static final Rule M018 = stops("description/M018",
            "A catalog or cube has no two named sets with the same name.");

    public static final Rule M019 = stops("description/M019", "A catalog has no two roles with the same name.");

    public static final Rule M020 = stops("description/M020", "A catalog has no two parameters with the same name.");

    public static final Rule M021 = stops("description/M021", "A catalog has no two functions with the same name.");

    public static final Rule M023 = stops("description/M023", "A dimension has no two hierarchies with the same name.");

    public static final Rule M024 = stops("description/M024",
            "A level has no two member properties with the same name.");

    public static final Rule M025 = stops("description/M025",
            "A cube has no two measures with the same name, calculated measures included.");

    public static final Rule M026 = stops("description/M026", "A cube has no two KPIs with the same name.");

    public static final Rule M027 = stops("description/M027", "A cube has no two measure groups with the same name.");

    public static final Rule M028 = stops("description/M028", "A cube has no two actions with the same name.");

    public static final Rule M029 = stops("description/M029", "A hierarchy has no two levels with the same name.");

    public static final Rule M031 = stops("description/M031", "A dimension has at least one hierarchy.");

    public static final Rule M032 = stops("description/M032", "A parent-child hierarchy has exactly one level.");

    public static final Rule M033 = stops("description/M033", "A hierarchy has at least one level.");

    public static final Rule M034 = stops("description/M034", "A level of a time kind belongs to a time dimension.");

    public static final Rule M035 = stops("description/M035", "A dimension usage is not named Measures.");

    public static final Rule M036 = stops("description/M036", "A dimension usage names a dimension.");

    public static final Rule M039 = stops("description/M039", "A cube has at least one stored measure.");

    public static final Rule M040 = stops("description/M040",
            "A role either unites other roles or has grants of its own, not both.");

    public static final Rule M041 = stops("description/M041",
            "A measure group has at most one distinct count measure.");

    public static final Rule M045 = stops("description/M045", "The parent KPI of a KPI is a KPI of the same cube.");

    public static final Rule M046 = stops("description/M046", "The parent KPIs of a cube form no ring.");

    public static final Rule M047 = stops("description/M047", "Every measure has an aggregator.");

    public static final Rule M048 = stops("description/M048", "A cube has at most one default drill-through action.");

    public static final Rule M049 = stops("description/M049", "A drill-through action has a list of what it returns.");

    public static final Rule M050 = stops("description/M050",
            "A drill-through action has no null entry among its returns.");

    public static final Rule M051 = stops("description/M051",
            "A drill-through action returns stored measures only, not calculated measures.");

    public static final Rule M052 = stops("description/M052",
            "Every entry that a drill-through action returns is a level, a level property or a stored measure of the "
            + "cube.");

    public static final Rule M077 = stops("description/M077", "A virtual cube draws only from cubes of its catalog.");

    public static final Rule M078 = stops("description/M078",
            "A virtual measure comes from a cube that the virtual cube draws from.");

    public static final Rule M079 = stops("description/M079",
            "A virtual dimension is a dimension of at least one cube that the virtual cube draws from.");

    public static final Rule M080 = stops("description/M080",
            "A cube dimension joined by a foreign key to the cube facts has a fact table named by the cube.");

    public static final Rule M081 = stops("description/M081",
            "A level that reads columns has a table, named by its hierarchy or by the cube.");

    public static final Rule M083 = stops("description/M083",
            "Every column a level, hierarchy or measure reads lies in one of the tables it is read from.");

    public static final Rule M085 = stops("description/M085",
            "An aggregation table given by name has a fact count column.");

    public static final Rule M086 = stops("description/M086",
            "A writeback table writes only dimensions that the cube uses.");

    public static final Rule M087 = stops("description/M087", "A writeback table writes only measures of its cube.");

    public static final Rule M099 = stops("description/M099", "A shared dimension is declared once in a catalog.");

    public static final Rule M100 = stops("description/M100",
            "A united role combines only roles that the catalog declares.");

    public static final Rule M103 = stops("description/M103", "A parent-child hierarchy has exactly one level.");

    public static final Rule M104 = stops("description/M104", "A parent-child hierarchy is not ragged.");

    public static final Rule M107 = stops("description/M107", "Only a parent-child hierarchy shows data members.");

    public static final Rule M108 = stops("description/M108", "Only a parent-child hierarchy names a parent column.");

    public static final Rule M111 = stops("description/M111",
            "A cube that names aggregation tables or a writeback table names its facts.");

    public static final Rule M112 = stops("description/M112",
            "A dimension usage of a cube uses a shared dimension the catalog declares.");

    public static final Rule O3 = stops("olap/O3",
            "The granularity of a regular dimension link is an attribute of the linked dimension.");

    public static final Rule O4 = stops("olap/O4",
            "The default measure of a cube is one of the measures of that cube.");

    public static final Rule O5 = stops("olap/O5",
            "A measure group links each dimension use of its own cube exactly once.");

    public static final Rule O8 = stops("olap/O8",
            "A role named as default role or as part of a union is a role of the catalog and is united at most once, "
            + "never with itself.");

    public static final Rule O10 = stops("olap/O10",
            "A cube has either measure groups or base cubes, not both and not neither.");

    public static final Rule O15 = stops("olap/O15", "The dimension uses of a cube have different names.");

    /** Every rule above, in the order of their codes within each family. */
    public static final List<Rule> ALL = List.of(
            M001, M010, M011, M012, M013, M014, M016, M017, M018, M019, M020, M021, M023, M024, M025, M026, M027, M028,
            M029, M031, M032, M033, M034, M035, M036, M039, M040, M041, M045, M046, M047, M048, M049, M050, M051, M052,
            M077, M078, M079, M080, M081, M083, M085, M086, M087, M099, M100, M103, M104, M107, M108, M111, M112, O3,
            O4, O5, O8, O10, O15);

    /** The families whose rules the description knows all of. */
    public static final List<String> FAMILIES = List.of("description", "olap");

    private DescriptionRules() {
    }

    private static Rule stops(String id, String says) {
        return new Rule(RuleId.of(id), Severity.ERROR, EnumSet.of(Consequence.ABORT), Consequence.ABORT, says);
    }
}
