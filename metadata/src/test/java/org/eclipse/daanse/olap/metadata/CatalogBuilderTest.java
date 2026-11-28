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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Locale;

import org.eclipse.daanse.olap.findings.FindingException;
import org.eclipse.daanse.olap.metadata.model.Access;
import org.eclipse.daanse.olap.metadata.model.AggregatorDef;
import org.eclipse.daanse.olap.metadata.model.CatalogDef;
import org.eclipse.daanse.olap.metadata.model.DimensionDef;
import org.eclipse.daanse.olap.metadata.model.DimensionKind;
import org.eclipse.daanse.olap.metadata.model.DimensionUsageDef;
import org.eclipse.daanse.olap.metadata.model.ElementInfo;
import org.eclipse.daanse.olap.metadata.model.ExplicitHierarchyDef;
import org.eclipse.daanse.olap.metadata.model.LevelKind;
import org.eclipse.daanse.olap.metadata.model.LocalizedText;
import org.eclipse.daanse.olap.metadata.model.ParentChildHierarchyDef;
import org.eclipse.daanse.olap.metadata.model.PhysicalCubeDef;
import org.eclipse.daanse.olap.metadata.model.RoleDef;
import org.eclipse.daanse.olap.metadata.model.VirtualCubeDef;
import org.junit.jupiter.api.Test;

class CatalogBuilderTest {

    private static CatalogDef sales() {
        return Olap.catalog("Sales")
                // the cube comes first: the order of the calls does not matter
                .cube("Sales", c -> c
                        .dimension("Order Date", "Time")
                        .dimension("Ship Date", "Time")
                        .dimension("Store", d -> d.hierarchy(h -> h.level("Country").level("City")))
                        .measure("Amount", m -> m.formatString("#,##0.00"))
                        .measure("Orders", m -> m.aggregator(AggregatorDef.Standard.COUNT))
                        .calculatedMember("Average", "[Measures].[Amount] / [Measures].[Orders]")
                        .namedSet("Top Cities", "TopCount([Store].[City].Members, 3, [Measures].[Amount])")
                        .kpi("Growth", k -> k.value("[Measures].[Amount]").goal("[Measures].[Amount] * 1.1")))
                .cube("Returns", c -> c
                        .dimension("Time")
                        .measure("Returned"))
                .virtualCube("Net", v -> v
                        .cube("Sales").cube("Returns", true)
                        .dimension("Order Date", "Time")
                        .measure("Sales", "Amount")
                        .measure("Returns", "Returned")
                        .calculatedMember("Net Amount", "[Measures].[Amount] - [Measures].[Returned]"))
                .dimension("Time", d -> d.time().caption(Locale.GERMAN, "Zeit").hierarchy(h -> h
                        .level("Year", l -> l.kind(LevelKind.TIME_YEARS).uniqueMembers())
                        .level("Month", l -> l.kind(LevelKind.TIME_MONTHS))))
                .role("Clerk", r -> r.cube("Sales", Access.Cube.CUSTOM, g -> g
                        .hierarchy("[Store]", Access.Hierarchy.CUSTOM, h -> h
                                .rollup(Access.Rollup.PARTIAL)
                                .member("[Store].[Germany]", Access.Member.ALL))))
                .role("Auditor", r -> r.catalog(Access.Catalog.ALL))
                .unionRole("Both", "Clerk", "Auditor")
                .defaultRole("Clerk")
                .build();
    }

    @Test
    void aDimensionUsedTwiceIsOneObject() {
        CatalogDef catalog = sales();
        PhysicalCubeDef cube = (PhysicalCubeDef) catalog.cube("Sales").orElseThrow();
        List<DimensionUsageDef> usages = cube.dimensions();

        assertThat(usages).extracting(DimensionUsageDef::name).containsExactly("Order Date", "Ship Date", "Store");
        DimensionDef time = catalog.sharedDimensions().get(0);
        assertThat(usages.get(0).dimension()).isSameAs(time);
        assertThat(usages.get(1).dimension()).isSameAs(time);
        // and the same object in the other cube
        assertThat(catalog.cube("Returns").orElseThrow().dimensions().get(0).dimension()).isSameAs(time);
        assertThat(time.kind()).isEqualTo(DimensionKind.TIME);
        assertThat(time.info().caption().resolve(Locale.GERMANY)).isEqualTo("Zeit");
        assertThat(time.info().caption().resolve(Locale.FRENCH)).isEqualTo("Time");
    }

    @Test
    void defaultsOfTheBuilder() {
        PhysicalCubeDef cube = (PhysicalCubeDef) sales().cube("Sales").orElseThrow();
        ExplicitHierarchyDef store = (ExplicitHierarchyDef) cube.dimensions().get(2).dimension().hierarchies().get(0);

        assertThat(store.name()).isEqualTo("Store");
        assertThat(store.allMemberName()).isEqualTo("All Store");
        assertThat(store.ragged()).isFalse();
        assertThat(cube.measures().get(0).aggregator()).isEqualTo(AggregatorDef.Standard.SUM);
        assertThat(cube.measureGroups()).singleElement().satisfies(g -> assertThat(g.name()).isEqualTo("Sales"));
        assertThat(cube.measureNames()).containsExactly("Amount", "Orders");
        assertThat(cube.calculatedMembers().get(0).hierarchy()).isEqualTo("[Measures]");
    }

    @Test
    void aVirtualCubePointsAtTheMeasureItself() {
        CatalogDef catalog = sales();
        VirtualCubeDef net = (VirtualCubeDef) catalog.cube("Net").orElseThrow();
        PhysicalCubeDef salesCube = (PhysicalCubeDef) catalog.cube("Sales").orElseThrow();

        assertThat(net.measures().get(0).measure()).isSameAs(salesCube.measures().get(0));
        assertThat(net.measures().get(0).cube()).isSameAs(salesCube);
        assertThat(net.cubes().get(1).ignoreUnrelatedDimensions()).isTrue();
        assertThat(net.measureNames()).containsExactly("Amount", "Returned");
    }

    @Test
    void aUnionRolePointsAtItsRoles() {
        CatalogDef catalog = sales();
        RoleDef both = catalog.roles().get(2);

        assertThat(both.unionOf()).containsExactly(catalog.roles().get(0), catalog.roles().get(1));
        assertThat(catalog.roles().get(0).cubeGrants().get(0).hierarchies().get(0).rollup())
                .isEqualTo(Access.Rollup.PARTIAL);
    }

    @Test
    void parentChildHierarchy() {
        CatalogDef catalog = Olap.catalog("HR")
                .cube("HR", c -> c
                        .dimension("Employee", d -> d.hierarchy(h -> h.parentChild().level("Employee")))
                        .measure("Salary"))
                .build();
        assertThat(catalog.cubes().get(0).dimensions().get(0).dimension().hierarchies().get(0))
                .isInstanceOfSatisfying(ParentChildHierarchyDef.class,
                        h -> assertThat(h.level().name()).isEqualTo("Employee"));
    }

    @Test
    void everyProblemIsReportedAtOnce() {
        FindingException e = catchThrowableOfType(FindingException.class, () -> Olap.catalog("Broken")
                .cube("Sales", c -> c
                        .dimension("Time")                       // not declared
                        .dimension("Geo", d -> d.hierarchy(h -> h.level("City").level("City")))
                        .dimension("Staff", d -> d.hierarchy(h -> h.parentChild().level("A").level("B")))
                        .dimension("Flat", d -> d.hierarchy(h -> h.level("Day", l -> l.kind(LevelKind.TIME_DAYS))))
                        .measure("Amount").measure("Amount")
                        .calculatedMember("Empty", " ")
                        .kpi("NoValue", k -> k.goal("1"))
                        .defaultMeasure("Missing"))
                .cube("NoMeasure", c -> c.dimension("Geo2", d -> d.hierarchy(h -> { })))
                .unionRole("Both", "Ghost")
                .defaultRole("Nobody")
                .build());

        assertThat(e.findings()).as("every finding, by rule and place")
                .extracting(f -> f.rule().toString(), f -> f.place().text()).containsExactlyInAnyOrder(
                        tuple("description/M112", "catalog Broken / cube Sales / dimension Time"),
                        tuple("description/M103", "catalog Broken / cube Sales / dimension Staff / hierarchy Staff"),
                        tuple("description/M029", "catalog Broken / cube Sales / dimension Geo / hierarchy Geo"),
                        tuple("description/M034", "catalog Broken / cube Sales / dimension Flat / hierarchy Flat / level Day"),
                        tuple("description/M025", "catalog Broken / cube Sales"),
                        tuple("description/M012", "catalog Broken / cube Sales / calculated member Empty"),
                        tuple("description/M014", "catalog Broken / cube Sales / KPI NoValue"),
                        tuple("olap/O4", "catalog Broken / cube Sales"),
                        tuple("description/M033", "catalog Broken / cube NoMeasure / dimension Geo2 / hierarchy Geo2"),
                        tuple("description/M039", "catalog Broken / cube NoMeasure"),
                        tuple("description/M100", "catalog Broken / role Both"),
                        tuple("olap/O8", "catalog Broken"));
        // in Mondrian's words: levels must be unique, a hierarchy needs a level
        assertThat(e.findings()).as("the messages in Mondrian's words").extracting(f -> f.message())
                .contains("Level names within hierarchy '[Geo].[Geo]' are not unique; there is more than one level"
                        + " with name 'City'.", "Hierarchy '[Geo2].[Geo2]' must have at least one level.");
        assertThat(e).as("the message counts what stops the build").hasMessageContaining("12 finding(s)");
    }

    @Test
    void aDescriptionBuiltByHandIsValidatedTheSameWay() {
        CatalogDef empty = new CatalogDef("Empty", List.of(), List.of(), List.of(), List.of(), null, List.of(), List.of(),
                ElementInfo.DEFAULT);
        // a catalog without a cube describes a database only; nothing to blame
        assertThat(Olap.validate(empty)).isEmpty();
        assertThat(Olap.validate(sales())).isEmpty();
    }

    @Test
    void theNameMeasuresIsTaken() {
        assertThatThrownBy(() -> Olap.catalog("C")
                .cube("C", c -> c.dimension("Measures", d -> d.hierarchy(h -> h.level("L"))).measure("M")).build())
                .isInstanceOf(FindingException.class).hasMessageContaining("the name Measures is taken");
    }

    @Test
    void localizedTextFallsBackToTheLanguageThenToTheText() {
        LocalizedText text = LocalizedText.of("Time").in(Locale.GERMAN, "Zeit");
        assertThat(text.resolve(Locale.of("de", "AT"))).isEqualTo("Zeit");
        assertThat(text.resolve(null)).isEqualTo("Time");
    }
}
