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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.findings.FindingException;
import org.eclipse.daanse.olap.metadata.Olap;
import org.eclipse.daanse.olap.metadata.model.Access;
import org.eclipse.daanse.olap.metadata.model.AggregatorDef;
import org.eclipse.daanse.olap.metadata.model.CachePolicyDef;
import org.eclipse.daanse.olap.metadata.model.CatalogDef;
import org.eclipse.daanse.olap.metadata.model.CubeDef;
import org.eclipse.daanse.olap.metadata.model.HierarchyDef;
import org.eclipse.daanse.olap.metadata.model.LevelDef;
import org.eclipse.daanse.olap.metadata.model.LevelKind;
import org.eclipse.daanse.olap.metadata.model.LocalizedText;
import org.eclipse.daanse.olap.metadata.model.MeasureDef;
import org.eclipse.daanse.olap.metadata.model.PhysicalCubeDef;
import org.eclipse.daanse.olap.metadata.model.RoleDef;
import org.eclipse.daanse.olap.metadata.model.ValueKind;
import org.eclipse.daanse.olap.metadata.model.VirtualCubeDef;
import org.junit.jupiter.api.Test;

/**
 * What the builder puts into the model: order independence, defaults, every set value unchanged, and
 * problems reported together with their path instead of an exception from the middle of the build.
 */
class BuilderContractTest {

    static List<Finding> problems(Supplier<CatalogDef> build) {
        FindingException e = catchThrowableOfType(FindingException.class, build::get);
        return e == null ? List.of() : e.findings();
    }

    static List<String> rules(List<Finding> problems) {
        return problems.stream().map(f -> f.rule().toString()).toList();
    }

    // ---- 001 the catalog -----------------------------------------------------------------------------------

    @Test
    void theOrderOfTheCallsDoesNotMatterAndProblemsComeTogether() {
        CatalogDef catalog = Olap.catalog("Shop")
                .virtualCube("Both", v -> v.cube("Sales").cube("Stock").dimension("Geo").measure("Sales", "Amount").measure("Stock", "OnHand"))
                .cube("Sales", c -> c.dimension("Geo").measure("Amount"))
                .cube("Stock", c -> c.dimension("Geo").measure("OnHand"))
                .dimension("Geo", d -> d.hierarchy(h -> h.level("Country")))
                .unionRole("Both", "Readers", "Writers")
                .unionRole("Readers", "Clerk")
                .role("Clerk", r -> r.cube("Sales", Access.Cube.ALL))
                .role("Writers", r -> r.cube("Stock", Access.Cube.ALL))
                .parameter("Year", ValueKind.INTEGER, "2024", false)
                .defaultRole("Clerk")
                .build();
        assertThat(catalog.cubes().stream().map(CubeDef::name).toList()).containsExactly("Both", "Sales", "Stock");
        RoleDef union = catalog.roles().stream().filter(r -> r.name().equals("Both")).findFirst().orElseThrow();
        assertThat(union.unionOf().stream().map(RoleDef::name).toList()).containsExactly("Readers", "Writers");
        assertThat(catalog.parameters().get(0).kind()).isEqualTo(ValueKind.INTEGER);
        assertThat(catalog.parameters().get(0).defaultValue()).isEqualTo("2024");
        assertThat(catalog.parameters().get(0).modifiable()).isFalse();
        assertThat(catalog.defaultRole()).isEqualTo("Clerk");

        List<Finding> found = problems(() -> Olap.catalog("Bad")
                .dimension("Geo", d -> d.hierarchy(h -> h.level("Country")))
                .dimension("Geo", d -> d.hierarchy(h -> h.level("Region")))
                .cube("Sales", c -> c.dimension("Nowhere").measure("Amount"))
                .build());
        assertThat(rules(found)).as("the shared dimension declared twice, the usage of one not declared")
                .contains("description/M099", "description/M112");
        assertThat(found.size()).as("builder and validator problems together").isGreaterThanOrEqualTo(2);
    }

    // ---- 002 the cube ---------------------------------------------------------------------------------------

    @Test
    void aCubeUsesTheSharedDimensionObjectAndKeepsWhatItIsGiven() {
        CatalogDef catalog = Olap.catalog("Cube")
                .dimension("Time", d -> d.hierarchy(h -> h.level("Year")))
                .cube("Sales", c -> c.dimension("Time").dimension("Ship Date", "Time").measure("Amount")
                        .namedSet("Plain", "{}").namedSet("Filed", "{}", "Folder").defaultMeasure("Amount"))
                .build();
        PhysicalCubeDef sales = (PhysicalCubeDef) catalog.cubes().get(0);
        assertThat(sales.dimensions().stream().map(u -> u.name()).toList()).containsExactly("Time", "Ship Date");
        assertThat(sales.dimensions().get(1).dimension()).isSameAs(sales.dimensions().get(0).dimension());
        assertThat(sales.measureGroups().get(0).name()).as("measure(name) goes into the group named like the cube").isEqualTo("Sales");
        assertThat(sales.measures().get(0).aggregator()).isEqualTo(AggregatorDef.Standard.SUM);
        assertThat(sales.cachePolicy()).isEqualTo(CachePolicyDef.DEFAULT);
        assertThat(sales.defaultMeasure()).isEqualTo("Amount");
        assertThat(sales.namedSets().get(1).displayFolder().text()).isEqualTo("Folder");
        assertThat(sales.namedSets().get(0).displayFolder()).isNull();
        List<Finding> found = problems(() -> Olap.catalog("Cube").cube("Sales", c -> c.dimension("Nowhere")
                .measure("Amount")).build());
        assertThat(found).as("the usage of a shared dimension the catalog does not declare")
                .extracting(f -> f.rule().toString(), f -> f.place().text())
                .contains(org.assertj.core.api.Assertions.tuple("description/M112",
                        "catalog Cube / cube Sales / dimension Nowhere"));
    }

    // ---- 003 dimensions and hierarchies ----------------------------------------------------------------------

    @Test
    void hierarchiesHaveTheirDefaultsAndAParentChildHierarchyIsCheckedWithoutBreaking() {
        CatalogDef catalog = Olap.catalog("Dims")
                .cube("Sales", c -> c
                        .dimension("Geo", d -> d.hierarchy(h -> h.level("Country")))
                        .dimension("Time", d -> d.time().hierarchy("Calendar", h -> h.allMember("Ever").level("Year"))
                                .hierarchy("Fiscal", h -> h.noAllMember().level("FYear").defaultMember("[Time].[Fiscal].[F24]")))
                        .measure("Amount"))
                .build();
        PhysicalCubeDef sales = (PhysicalCubeDef) catalog.cubes().get(0);
        HierarchyDef geo = sales.dimensions().get(0).dimension().hierarchies().get(0);
        assertThat(geo.name()).isEqualTo("Geo");
        assertThat(geo.allMemberName()).isEqualTo("All Geo");
        assertThat(geo.allLevelName()).as("not given: null, which the runtime reads as (All)").isNull();
        List<HierarchyDef> time = sales.dimensions().get(1).dimension().hierarchies();
        assertThat(time.stream().map(HierarchyDef::name).toList()).containsExactly("Calendar", "Fiscal");
        assertThat(time.get(0).allMemberName()).isEqualTo("Ever");
        assertThat(time.get(1).hasAll()).isFalse();
        assertThat(time.get(1).defaultMember()).isEqualTo("[Time].[Fiscal].[F24]");
        assertThat(sales.dimensions().get(1).dimension().kind().name()).isEqualTo("TIME");

        assertThatCode(() -> problems(() -> Olap.catalog("PC")
                .cube("Sales", c -> c.dimension("Org", d -> d.hierarchy(h -> h.parentChild())).measure("Amount")).build()))
                .as("a parent-child hierarchy without a level is a problem, not a NullPointerException").doesNotThrowAnyException();
        List<Finding> withoutLevel = problems(() -> Olap.catalog("PC")
                .cube("Sales", c -> c.dimension("Org", d -> d.hierarchy(h -> h.parentChild())).measure("Amount")).build());
        assertThat(withoutLevel).hasSize(1);
        List<Finding> twoLevels = problems(() -> Olap.catalog("PC")
                .cube("Sales", c -> c.dimension("Org", d -> d.hierarchy(h -> h.parentChild().level("A").level("B"))).measure("Amount")).build());
        assertThat(rules(twoLevels)).as("a parent-child hierarchy of two levels").containsExactly("description/M103");
        List<Finding> ragged = problems(() -> Olap.catalog("PC")
                .cube("Sales", c -> c.dimension("Org", d -> d.hierarchy(h -> h.parentChild().ragged().level("A"))).measure("Amount")).build());
        assertThat(ragged).isNotEmpty();
    }

    // ---- 004 levels, measures, formulas, KPIs --------------------------------------------------------------------

    /** A property read from a column says what it is, as a catalog file describes it; without a text it says nothing. */
    @Test
    void aColumnPropertyKeepsItsDescription() {
        CatalogDef catalog = Olap.catalog("Described")
                .cube("Sales", c -> c
                        .dimension("Geo", d -> d.hierarchy(h -> h.level("City", l -> l
                                .propertyFormattedBy("Size", ValueKind.NUMERIC, null, null, "how big")
                                .propertyDependingOnLevelValue("Zip", ValueKind.STRING, null, null, "the post code")
                                .propertyFormattedBy("Plain", ValueKind.STRING, null, null))))
                        .measure("Amount"))
                .build();
        LevelDef city = ((PhysicalCubeDef) catalog.cubes().get(0)).dimensions().get(0).dimension().hierarchies().get(0)
                .levels().get(0);
        assertThat(city.properties()).extracting(p -> p.info().description() == null ? null : p.info().description().text())
                .containsExactly("how big", "the post code", null);
        assertThat(city.properties().get(1).dependsOnLevelValue()).isTrue();
    }

    @Test
    void theBuildersKeepTheirDefaultsAndEveryValueSet() {
        CatalogDef catalog = Olap.catalog("Defaults")
                .cube("Sales", c -> c
                        .dimension("Geo", d -> d.hierarchy(h -> h.level("Country")
                                .level("City", l -> l.keyKind(ValueKind.INTEGER).uniqueMembers().ordered()
                                        .property("Size", ValueKind.NUMERIC))))
                        .dimension("Time", d -> d.time().hierarchy(h -> h.level("Day", l -> l.kind(LevelKind.TIME_DAYS))))
                        .measure("Amount")
                        .measure("Money", m -> m.aggregator(AggregatorDef.Standard.MAX).kind(ValueKind.INTEGER).formatString("#").displayFolder("F"))
                        .calculatedMember("Twice", "[Measures].[Amount] * 2")
                        .calculatedMember("Pair", "1", m -> m.hierarchy("[Geo].[Geo]").parent("[Geo].[Geo].[DE]").formatString("0").solveOrder(4)
                                .displayFolder("X").property("A", "1").property("B", "2").property("A", "3"))
                        .kpi("Hit", k -> k.value("[Measures].[Amount]").goal("1").status("2").trend("3").weight("4")
                                .currentTimeMember("[Geo].[Geo].[DE]").statusGraphic("Gauge").trendGraphic("Arrow").displayFolder("K")))
                .build();
        PhysicalCubeDef sales = (PhysicalCubeDef) catalog.cubes().get(0);
        LevelDef country = sales.dimensions().get(0).dimension().hierarchies().get(0).levels().get(0);
        assertThat(country.kind()).isEqualTo(LevelKind.REGULAR);
        // not given: null, which an engine reads as "other", not as text
        assertThat(country.keyKind()).isNull();
        assertThat(country.uniqueMembers()).isFalse();
        assertThat(country.ordered()).isFalse();
        assertThat(country.properties()).isEmpty();
        LevelDef city = sales.dimensions().get(0).dimension().hierarchies().get(0).levels().get(1);
        assertThat(sales.dimensions().get(1).dimension().hierarchies().get(0).levels().get(0).kind()).isEqualTo(LevelKind.TIME_DAYS);
        assertThat(city.keyKind()).isEqualTo(ValueKind.INTEGER);
        assertThat(city.uniqueMembers()).isTrue();
        assertThat(city.ordered()).isTrue();
        assertThat(city.properties()).hasSize(1);
        MeasureDef amount = sales.measures().get(0);
        assertThat(amount.aggregator()).isEqualTo(AggregatorDef.Standard.SUM);
        assertThat(amount.kind()).as("not given: null, the binder derives the kind from the aggregator").isNull();
        MeasureDef money = sales.measures().get(1);
        assertThat(money.aggregator()).isEqualTo(AggregatorDef.Standard.MAX);
        assertThat(money.formatString()).isEqualTo("#");
        assertThat(money.displayFolder().text()).isEqualTo("F");
        var twice = sales.calculatedMembers().get(0);
        assertThat(twice.hierarchy()).isEqualTo("[Measures]");
        assertThat(twice.solveOrder()).isZero();
        assertThat(twice.parent()).isNull();
        var pair = sales.calculatedMembers().get(1);
        assertThat(pair.solveOrder()).isEqualTo(4);
        assertThat(pair.parent()).isEqualTo("[Geo].[Geo].[DE]");
        assertThat(pair.properties()).as("in order, the later A overwriting the first").containsExactly(
                java.util.Map.entry("A", "3"), java.util.Map.entry("B", "2"));
        var hit = sales.kpis().get(0);
        assertThat(List.of(hit.value(), hit.goal(), hit.status(), hit.trend(), hit.weight(), hit.currentTimeMember(),
                hit.statusGraphic(), hit.trendGraphic(), hit.displayFolder().text()))
                .containsExactly("[Measures].[Amount]", "1", "2", "3", "4", "[Geo].[Geo].[DE]", "Gauge", "Arrow", "K");
        assertThat(problems(() -> Olap.catalog("Kpi").cube("Sales", c -> c.measure("Amount")
                .kpi("Empty", k -> k.goal("1"))).build())).as("a KPI without a value")
                .extracting(f -> f.rule().toString(), f -> f.place().text())
                .containsExactly(org.assertj.core.api.Assertions.tuple("description/M014",
                        "catalog Kpi / cube Sales / KPI Empty"));
    }

    // ---- 005 captions and translations ---------------------------------------------------------------------

    @Test
    void aTranslationSurvivesTheBaseCaptionAndResolvesLocaleBeforeLanguageBeforeText() {
        CatalogDef catalog = Olap.catalog("Text")
                .cube("Sales", c -> c.caption(Locale.GERMAN, "Verkauf").caption(Locale.GERMANY, "Verkauf (DE)").caption("Sales figures")
                        .meta("a", "1").meta("b", "2").measure("Amount"))
                .build();
        var info = catalog.cubes().get(0).info();
        LocalizedText caption = info.caption();
        assertThat(caption.text()).isEqualTo("Sales figures");
        assertThat(caption.resolve(Locale.GERMANY)).as("the exact locale first").isEqualTo("Verkauf (DE)");
        assertThat(caption.resolve(Locale.of("de", "AT"))).as("then its language").isEqualTo("Verkauf");
        assertThat(caption.resolve(Locale.FRANCE)).isEqualTo("Sales figures");
        assertThat(info.visible()).isTrue();
        assertThat(info.meta().stream().map(m -> m.key()).toList()).containsExactly("a", "b");
        assertThat(Olap.catalog("Plain").cube("Sales", c -> c.measure("Amount")).build().cubes().get(0).info().caption())
                .as("without a caption there is none; the runtime shows the name").isNull();
    }

    // ---- 006 roles -------------------------------------------------------------------------------------------

    @Test
    void aRoleHasNoCatalogAccessByDefaultAndItsGrantsInOrder() {
        CatalogDef catalog = Olap.catalog("Roles")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("Country").level("City"))).measure("Amount"))
                .role("Plain", r -> r.cube("Sales", Access.Cube.ALL))
                .role("Narrow", r -> r.cube("Sales", Access.Cube.CUSTOM, g -> g
                        .hierarchy("[Geo]", Access.Hierarchy.CUSTOM, h -> h.topLevel("[Geo].[Geo].[Country]").bottomLevel("[Geo].[Geo].[City]")
                                .member("[Geo].[Geo].[DE]", Access.Member.ALL).member("[Geo].[Geo].[FR]", Access.Member.NONE))))
                .build();
        RoleDef plain = catalog.roles().get(0);
        assertThat(plain.catalogAccess()).isEqualTo(Access.Catalog.NONE);
        assertThat(plain.cubeGrants().get(0).dimensions()).isEmpty();
        assertThat(plain.cubeGrants().get(0).hierarchies()).isEmpty();
        var hierarchy = catalog.roles().get(1).cubeGrants().get(0).hierarchies().get(0);
        assertThat(hierarchy.topLevel()).isEqualTo("[Geo].[Geo].[Country]");
        assertThat(hierarchy.bottomLevel()).isEqualTo("[Geo].[Geo].[City]");
        assertThat(hierarchy.rollup()).isEqualTo(Access.Rollup.FULL);
        assertThat(hierarchy.members().stream().map(m -> m.member()).toList()).containsExactly("[Geo].[Geo].[DE]", "[Geo].[Geo].[FR]");
    }

    // ---- 007 virtual cubes ----------------------------------------------------------------------------------

    @Test
    void aVirtualCubePointsAtTheSourceMeasureAndReportsWhatItCannotFind() {
        CatalogDef catalog = Olap.catalog("V")
                .cube("Sales", c -> c.measure("Amount"))
                .cube("Stock", c -> c.measure("OnHand"))
                .virtualCube("Both", v -> v.cube("Sales").cube("Stock").measure("Sales", "Amount").measure("Pieces", "Stock", "OnHand"))
                .build();
        VirtualCubeDef both = (VirtualCubeDef) catalog.cube("Both").orElseThrow();
        PhysicalCubeDef sales = (PhysicalCubeDef) catalog.cube("Sales").orElseThrow();
        // as SSAS: a measure ignores a dimension it does not vary by unless its group says false
        assertThat(both.cubes().get(0).ignoreUnrelatedDimensions()).isTrue();
        assertThat(both.measures().get(0).measure()).isSameAs(sales.measures().get(0));
        assertThat(both.measures().get(0).cube()).isSameAs(sales);
        assertThat(both.measureNames()).containsExactly("Amount", "Pieces");
        List<Finding> found = problems(() -> Olap.catalog("V")
                .cube("Sales", c -> c.measure("Amount"))
                .cube("Stock", c -> c.measure("OnHand"))
                .virtualCube("Both", v -> v.cube("Sales").cube("Nowhere").measure("Sales", "Nope").measure("Stock", "OnHand"))
                .build());
        assertThat(found).as("a cube not there, a measure not there, a measure of a cube not drawn from")
                .extracting(f -> f.rule().toString(), f -> f.place().text())
                .contains(org.assertj.core.api.Assertions.tuple("description/M077", "catalog V / cube Both"),
                        org.assertj.core.api.Assertions.tuple("olap/O10", "catalog V / cube Both / measure Nope"),
                        org.assertj.core.api.Assertions.tuple("description/M078", "catalog V / cube Both / measure OnHand"));
    }

    // ---- 008 model objects without the builder ----------------------------------------------------------------

    @Test
    void modelObjectsCopyTheirListsAndAnswerAsDescribed() {
        CatalogDef catalog = Olap.catalog("M")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.noAllMember().level("Country")))
                        .measure("Plan", "Budget", m -> { }).measure("Actual", "Spent", m -> { }).measure("Plan", "Target", m -> { }))
                .build();
        assertThat(catalog.cube("Sales")).isPresent();
        assertThat(catalog.cube("Nowhere")).isEmpty();
        PhysicalCubeDef sales = (PhysicalCubeDef) catalog.cube("Sales").orElseThrow();
        assertThat(sales.dimensions().stream().map(u -> u.name()).toList()).doesNotContain("Measures");
        assertThat(sales.measures().stream().map(MeasureDef::name).toList()).as("the groups flattened in their order")
                .containsExactly("Budget", "Target", "Spent");
        assertThat(sales.dimensions().get(0).dimension().hierarchies().get(0).hasAll()).isFalse();
        List<String> names = new ArrayList<>(List.of("x"));
        var info = new org.eclipse.daanse.olap.metadata.model.ElementInfo(null, null, true,
                new ArrayList<>(List.of(new org.eclipse.daanse.olap.metadata.model.MetaItem("k", "v"))));
        assertThat(info.meta()).hasSize(1);
        assertThat(names).hasSize(1);
        assertThat(new CachePolicyDef(true, true)).isEqualTo(CachePolicyDef.DEFAULT);
    }
}
