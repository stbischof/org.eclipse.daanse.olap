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

import static org.eclipse.daanse.olap.metadata.FindingAssert.assertThatModel;

import java.util.List;

import org.eclipse.daanse.olap.metadata.model.Access;
import org.eclipse.daanse.olap.metadata.model.AggregatorDef;
import org.eclipse.daanse.olap.metadata.model.CatalogDef;
import org.eclipse.daanse.olap.metadata.model.ElementInfo;
import org.eclipse.daanse.olap.metadata.model.LevelKind;
import org.eclipse.daanse.olap.metadata.model.ValueKind;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ModelValidationTest {

    private static final String SALES = "catalog Shop / cube Sales";

    @Test
    void twoMeasuresOfOneGroupThatCountDistinctValuesAreReportedAtTheGroup() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c
                        .dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Customers", m -> m.aggregator(AggregatorDef.Standard.DISTINCT_COUNT))
                        .measure("Products", m -> m.aggregator(AggregatorDef.Standard.DISTINCT_COUNT)))
                .build())
                .fails()
                .withProblem(DescriptionRules.M041, "catalog Shop / cube Sales / measure group Sales",
                        "counts distinct values more than once (Customers, Products); measures that count"
                                + " distinct values each need a group of their own");
    }

    @Test
    void aGroupThatReachesALevelTheDimensionDoesNotHaveIsReported() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c
                        .dimension("Time", d -> d.hierarchy(h -> h.level("Year").level("Month")))
                        .measureGroup("Plan", g -> g.dimension("Time", "Week"))
                        .measure("Plan", "Goal", m -> { }))
                .build())
                .fails()
                .withProblem(DescriptionRules.O3, "catalog Shop / cube Sales / measure group Plan",
                        "reaches Week in Time, which is no level of it");
    }

    @Test
    void aGroupMayReachAnyLevelTheDimensionHas() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c
                        .dimension("Time", d -> d.hierarchy(h -> h.level("Year").level("Month")))
                        .measureGroup("Plan", g -> g.dimension("Time", "Year"))
                        .measure("Plan", "Goal", m -> { }))
                .build())
                .isValid();
    }

    @Test
    void oneDistinctCountPerGroupIsFine() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c
                        .dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Customers", m -> m.aggregator(AggregatorDef.Standard.DISTINCT_COUNT))
                        .measure("Products", "Products", m -> m.aggregator(AggregatorDef.Standard.DISTINCT_COUNT)))
                .build())
                .isValid();
    }

    @Test
    void twoElementsOfTheSameKindAndNameAreReportedOnceForEachKind() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c
                        .dimension("Geo", d -> d.hierarchy(h -> h.level("City", l -> l
                                .property("Population", ValueKind.INTEGER)
                                .property("Population", ValueKind.INTEGER))))
                        .dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Amount").measure("Amount")
                        .namedSet("Top", "{}").namedSet("Top", "{}")
                        .kpi("Growth", k -> k.value("1")).kpi("Growth", k -> k.value("1"))
                        .drillThrough("Rows", false, "City").drillThrough("Rows", false, "City"))
                .cube("Returns", c -> c.dimension("Geo2", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Returned"))
                .cube("Returns", c -> c.dimension("Geo3", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Returned"))
                .role("Clerk", r -> r.catalog(Access.Catalog.ALL))
                .role("Clerk", r -> r.catalog(Access.Catalog.ALL))
                .parameter("Today", ValueKind.STRING, "'now'", false)
                .parameter("Today", ValueKind.STRING, "'now'", false)
                .build())
                .fails()
                .withProblem(DescriptionRules.M017, "catalog Shop", "has more than one cube named Returns")
                .withProblem(DescriptionRules.M019, "catalog Shop", "has more than one role named Clerk")
                .withProblem(DescriptionRules.M020, "catalog Shop", "Duplicate parameter 'Today' in schema")
                .withProblem(DescriptionRules.O15, SALES, "has more than one dimension named Geo")
                .withProblem(DescriptionRules.M025, SALES, "has more than one measure named Amount")
                .withProblem(DescriptionRules.M018, SALES, "has more than one named set named Top")
                .withProblem(DescriptionRules.M026, SALES, "has more than one KPI named Growth")
                .withProblem(DescriptionRules.M028, SALES, "has more than one action named Rows")
                .withProblem(DescriptionRules.M024, SALES + " / dimension Geo / hierarchy Geo / level City",
                        "has more than one member property named Population");
    }

    /** Every kind of element is blamed for a missing name or formula, in the catalog and in the cube. */
    @Test
    void everyKindIsBlamedForItsMissingNameOrFormula() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c
                        .dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Amount")
                        .namedSet("Top", "")
                        .calculatedMember("", "1")
                        .calculatedMember("Twice", "1", m -> m.hierarchy(""))
                        .kpi("", k -> k.value("1"))
                        .drillThrough("", false, "City"))
                .namedSet("", "{}")
                .namedSet("Every", "")
                .function("Half", x -> x / 2)
                .function("Half", x -> x / 2)
                .role("", r -> r.catalog(Access.Catalog.ALL))
                .build())
                .fails()
                .withProblem(DescriptionRules.M010, SALES + " / named set Top", "has no formula")
                .withProblem(DescriptionRules.M001, SALES + " / calculated member ", "has no name")
                .withProblem(DescriptionRules.M013, SALES + " / calculated member Twice", "has no hierarchy")
                .withProblem(DescriptionRules.M001, SALES + " / KPI ", "has no name")
                .withProblem(DescriptionRules.M001, SALES + " / action ", "has no name")
                .withProblem(DescriptionRules.M001, "catalog Shop / named set ", "has no name")
                .withProblem(DescriptionRules.M010, "catalog Shop / named set Every", "has no formula")
                .withProblem(DescriptionRules.M021, "catalog Shop", "has more than one function named Half")
                .withProblem(DescriptionRules.M001, "catalog Shop / role ", "has no name");
    }

    /** What a drill-through returns: a stored measure, a level or a property of it, under the hierarchy's own name. */
    @Test
    void aDrillThroughReturnsStoredMeasuresLevelsAndTheirProperties() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c
                        .dimension("Geo", d -> d.hierarchy("Places", h -> h.level("City", l -> l
                                .property("Population", ValueKind.INTEGER))))
                        .measure("Amount")
                        .calculatedMember("Twice", "[Measures].[Amount] * 2")
                        .kpi("Growth", k -> k.value("1"))
                        .kpi("Sub", k -> k.value("1").parent("Growth"))
                        .drillThrough("Fine", false, "[Measures].[Amount]", "[Geo].[Places].[City]",
                                "[Geo].[Places].[City].[Population]")
                        .drillThrough("Bad", false, "[Measures].[Twice]", "[Geo].[Places].[City].[Area]",
                                "[Geo].[Geo].[City]"))
                .build())
                .fails().withProblemCount(3)
                .withProblem(DescriptionRules.M051, SALES + " / action Bad",
                        "returns the calculated measure [Measures].[Twice]")
                .withProblem(DescriptionRules.M052, SALES + " / action Bad",
                        "returns [Geo].[Places].[City].[Area], which is no level")
                .withProblem(DescriptionRules.M052, SALES + " / action Bad",
                        "returns [Geo].[Geo].[City], which is no level");
    }

    @Test
    void aNameThatDiffersOnlyInItsCaseIsTheSameName() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Amount").measure("amount"))
                .build())
                .fails().withProblem(DescriptionRules.M025, SALES, "has more than one measure named amount");
    }

    @Test
    void aNameThatCollidesTwiceIsStillOneProblem() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("A", c -> c.measure("Amount"))
                .cube("a", c -> c.measure("Amount"))
                .cube("A", c -> c.measure("Amount"))
                .build())
                .fails().withProblemCount(1);
    }

    @Test
    void anElementThatIsMissingAPartIsBlamedForThatPart() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c
                        .dimension("Geo", d -> { })
                        .dimension("Time", d -> d.hierarchy(h -> { }))
                        .measure("Amount", m -> m.aggregator(null))
                        .namedSet("", "{}"))
                .function("Twice", ValueKind.NUMERIC, List.of(ValueKind.NUMERIC), null)
                .build())
                .fails()
                .withProblem(DescriptionRules.M031, SALES + " / dimension Geo", "has no hierarchy")
                .withProblem(DescriptionRules.M033, SALES + " / dimension Time / hierarchy Time",
                        "Hierarchy '[Time].[Time]' must have at least one level.")
                .withProblem(DescriptionRules.M047, SALES + " / measure Amount", "has no aggregator")
                .withProblem(DescriptionRules.M001, SALES + " / named set ", "has no name")
                .withProblem(DescriptionRules.M011, "catalog Shop / function Twice", "has no body");
    }

    @Test
    void aParentChildHierarchyWithoutALevelIsOneProblem() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Staff", c -> c.dimension("Employee", d -> d.hierarchy(h -> h.parentChild()))
                        .measure("Salary"))
                .build())
                .fails().withProblem(DescriptionRules.M032,
                        "catalog Shop / cube Staff / dimension Employee / hierarchy Employee",
                        "a parent-child hierarchy has exactly one level");
    }

    @Test
    void aDimensionUsedUnderTheNameMeasuresIsRefusedToo() {
        assertThatModel(() -> Olap.catalog("Shop")
                .dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                .cube("Sales", c -> c.dimension("Measures", "Geo").measure("Amount"))
                .build())
                .fails().withProblem(DescriptionRules.M035, SALES + " / dimension Measures",
                        "the name Measures is taken");
    }

    @Test
    void aRawNameMayCarryAClosingBracket() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Amo]unt"))
                .build())
                .isValid();
    }

    @Test
    void aCubeIsBlamedForItsMeasures() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Amount").calculatedMember("Amount", "1")
                        .defaultMeasure("Missing"))
                .cube("Returns", c -> c.dimension("Geo2", d -> d.hierarchy(h -> h.level("City"))))
                .build())
                .fails()
                .withProblem(DescriptionRules.M025, SALES, "has more than one measure named Amount")
                .withProblem(DescriptionRules.O4, SALES, "default measure Missing is not a measure of the cube")
                .withProblem(DescriptionRules.M039, "catalog Shop / cube Returns", "has no measure");
    }

    @Test
    void aCalculatedMeasureMayBeTheDefaultMeasure() {
        assertThatModel(Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Amount").calculatedMember("Double Amount", "[Measures].[Amount] * 2")
                        .defaultMeasure("Double Amount"))
                .build())
                .isValid();
    }

    @Test
    void aDefaultMeasureIsFoundTheWayAQueryFindsIt() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Amount").defaultMeasure("amount"))
                .build())
                .isValid();
    }

    @Test
    void aCalculatedMemberWithoutAFormulaIsBlamedBeforeAnyQueryRuns() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Amount").calculatedMember("Average", " "))
                .build())
                .fails().withProblem(DescriptionRules.M012, SALES + " / calculated member Average",
                        "The calculated member 'Average' in cube 'Sales' has no formula");
    }

    @Test
    @Disabled("""
            Kept out on purpose, not a defect. A parent path names a member, and a description \
            holds no members: every member of this provider comes from a member source that is \
            read when a query asks for it. The validator sees the description alone, so it cannot \
            tell a parent that is there from one that is not, and a rule that guessed would blame \
            descriptions that are sound. The lookup happens where the members are: the catalog \
            resolves the MDX of every calculated member when it starts, refuses to start when a \
            parent is missing, and names the cube and the parent path while doing so. The \
            description is therefore accepted here, and nobody's query is the first to find out.""")
    void aCalculatedMemberUnderAParentThatIsNotThereIsBlamedAtBuild() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Amount")
                        .calculatedMember("Big Cities", "1",
                                m -> m.hierarchy("[Geo]").parent("[Geo].[Nowhere]")))
                .build())
                .fails().withProblemCount(1);
    }

    @Test
    void aVirtualCubeIsBlamedForWhatItDrawsFrom() {
        assertThatModel(() -> Olap.catalog("Shop")
                .virtualCube("Empty", v -> v.measure("Sales", "Amount"))
                .virtualCube("Unknown", v -> v.cube("Nowhere").measure("Sales", "Amount"))
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City"))).measure("Amount"))
                .cube("Returns", c -> c.dimension("Geo2", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Returned"))
                .virtualCube("Net", v -> v.cube("Sales").measure("Returns", "Returned")
                        .measure("Sales", "Nothing"))
                .build())
                .fails()
                .withProblem(DescriptionRules.O10, "catalog Shop / cube Empty",
                        "a virtual cube draws from at least one cube")
                .withProblem(DescriptionRules.M077, "catalog Shop / cube Unknown",
                        "draws from cube Nowhere, which is not a physical cube of the catalog")
                .withProblem(DescriptionRules.M078, "catalog Shop / cube Net / measure Returned",
                        "comes from cube Returns, which the virtual cube does not draw from")
                .withProblem(DescriptionRules.O10, "catalog Shop / cube Net / measure Nothing",
                        "takes over measure Nothing of cube Sales, which does not exist");
    }

    @Test
    void aVirtualCubeMayBeDescribedBeforeTheCubesItDrawsFrom() {
        assertThatModel(Olap.catalog("Shop")
                .virtualCube("Net", v -> v.cube("Sales").measure("Sales", "Amount"))
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City"))).measure("Amount"))
                .build())
                .isValid();
    }

    @Test
    void aKpiIsBlamedForItsValueAndItsParent() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City"))).measure("Amount")
                        .kpi("Silent", k -> k.goal("1"))
                        .kpi("Orphan", k -> k.value("1").parent("Nowhere")))
                .build())
                .fails()
                .withProblem(DescriptionRules.M014, SALES + " / KPI Silent", "has no value")
                .withProblem(DescriptionRules.M045, SALES + " / KPI Orphan",
                        "parent KPI Nowhere is not a KPI of the cube");
    }

    @Test
    void kpisThatParentEachOtherInARingAreBlamed() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City"))).measure("Amount")
                        .kpi("First", k -> k.value("1").parent("Second"))
                        .kpi("Second", k -> k.value("1").parent("First")))
                .build())
                .fails().withProblem(DescriptionRules.M046, SALES, "First");
    }

    @Test
    void aRoleIsBlamedForWhatItPointsAt() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City"))).measure("Amount"))
                .role("Clerk", r -> r.catalog(Access.Catalog.ALL))
                .unionRole("Both", "Clerk", "Ghost")
                .defaultRole("Nobody")
                .build())
                .fails()
                .withProblem(DescriptionRules.M100, "catalog Shop / role Both",
                        "combines role Ghost, which the catalog does not declare")
                .withProblem(DescriptionRules.O8, "catalog Shop", "default role Nobody is not a role of the catalog");
    }

    @Test
    void aTimeLevelBelongsInATimeDimensionAndAPlainLevelFitsAnywhere() {
        assertThatModel(() -> Olap.catalog("Shop")
                .cube("Sales", c -> c
                        .dimension("Geo", d -> d.hierarchy(h -> h.level("Year", l -> l.kind(LevelKind.TIME_YEARS))))
                        .measure("Amount"))
                .build())
                .fails().withProblem(DescriptionRules.M034, SALES + " / dimension Geo / hierarchy Geo / level Year",
                        "is of kind TIME_YEARS, but its dimension is not a time dimension");

        assertThatModel(Olap.catalog("Shop")
                .dimension("Time", d -> d.time().hierarchy(h -> h
                        .level("Year", l -> l.kind(LevelKind.TIME_YEARS)).level("Season")))
                .cube("Sales", c -> c.dimension("Time").measure("Amount"))
                .build())
                .isValid();
    }

    @Test
    void aDescriptionBuiltByHandIsBlamedTheSameWay() {
        // a catalog without a cube is fine (it may describe a database only); a default role it does not declare is not
        CatalogDef nobody = new CatalogDef("Shop", List.of(), List.of(), List.of(), List.of(), "Nobody", List.of(),
                List.of(), ElementInfo.DEFAULT);

        assertThatModel(nobody).fails().withProblemCount(1)
                .withProblem(DescriptionRules.O8, "catalog Shop", "default role Nobody is not a role of the catalog")
                .withAMessageNamingEveryProblem();
    }

    @Test
    void allProblemsComeInOneExceptionWithTheirCountAndPathsAndAnUnchangeableList() {
        org.eclipse.daanse.olap.findings.FindingException e = org.assertj.core.api.Assertions.catchThrowableOfType(
                org.eclipse.daanse.olap.findings.FindingException.class, () -> Olap.catalog("Bad")
                        .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("A").level("A")))
                                .measure("Amount").measure("amount"))
                        .cube("Empty", c -> { })
                        .build());
        org.assertj.core.api.Assertions.assertThat(e).as("the build stops").isNotNull();
        org.assertj.core.api.Assertions.assertThat(e.findings()).as("the findings, by rule")
                .extracting(f -> f.rule().toString())
                .contains("description/M029", "description/M025", "description/M039");
        org.assertj.core.api.Assertions.assertThat(e.getMessage()).as("the message")
                .contains(e.findings().size() + " finding(s)")
                .contains("catalog Bad / cube Sales / dimension Geo / hierarchy Geo");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> e.findings().clear()).as("the list carried")
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
