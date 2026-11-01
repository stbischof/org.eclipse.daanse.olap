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
package org.eclipse.daanse.olap.provider.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.relational.RelationalContext;
import org.eclipse.daanse.olap.api.result.NullValue;
import org.eclipse.daanse.olap.api.result.ObjectValue;
import org.eclipse.daanse.olap.api.result.CellReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The catalog every function contract refers to, built here once and checked by hand.
 *
 * <pre>
 * [Geo]  All Geo (150)
 *          North (30)   -&gt; A (10), B (20)
 *          South (120)  -&gt; C (30), D (40), E (50)
 * </pre>
 */
class MemoryCatalogTest {

    private MemoryCatalog catalog;
    private MemoryCube cube;
    private MemoryHierarchy geo;
    private MapCellReader cells;

    @BeforeEach
    void buildCatalog() {
        MemoryCatalogBuilder builder = MemoryCatalog.builder("Test");
        cube = builder.cube("Test");

        geo = cube.dimension("Geo").hierarchy();
        geo.level("(All)").level("Region").level("City");
        MemoryMember allGeo = geo.member("All Geo");
        allGeo.child("North").children("A", "B");
        allGeo.child("South").children("C", "D", "E");

        MemoryHierarchy measures = cube.measures().hierarchy("Measures", false);
        measures.level("MeasuresLevel");
        measures.member("Amount");

        catalog = builder.build();

        String m = "[Measures].[Amount]";
        cells = new MapCellReader()
                .at(10, "[Geo].[All Geo].[North].[A]", m)
                .at(20, "[Geo].[All Geo].[North].[B]", m)
                .at(30, "[Geo].[All Geo].[South].[C]", m)
                .at(40, "[Geo].[All Geo].[South].[D]", m)
                .at(50, "[Geo].[All Geo].[South].[E]", m);
    }

    @Test
    @DisplayName("each hierarchy of a cube gets its own place in the evaluation context")
    void hierarchiesHaveDistinctOrdinalsInTheCube() {
        MemoryHierarchy measures = (MemoryHierarchy) cube.measures().getHierarchies().get(0);

        assertThat(geo.getOrdinalInCube()).isNotEqualTo(measures.getOrdinalInCube());
        assertThat(cube.getHierarchies()).extracting(h -> h.getOrdinalInCube())
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("every member lands in its level, and the ordinals ascend")
    void membersAreRegisteredInTheirLevel() {
        List<Member> regions = geo.getLevels().get(1).getMembers();
        assertThat(regions).extracting(Member::getName).containsExactly("North", "South");

        List<Member> cities = geo.getLevels().get(2).getMembers();
        assertThat(cities).extracting(Member::getName).containsExactly("A", "B", "C", "D", "E");
        assertThat(cities).extracting(Member::getOrdinal).containsExactly(0, 1, 2, 3, 4);

        assertThat(geo.getRootMembers()).extracting(Member::getName).containsExactly("All Geo");
        assertThat(cube.getMeasures()).extracting(Member::getName).containsExactly("Amount");
    }

    @Test
    @DisplayName("a cell above the leaves is summed over the leaves below it")
    void cellValuesRollUpOverTheLeaves() {
        // A value that was written down comes back as it was written; a total comes back
        // as a whole number. Both are numbers, and that is what a caller compares.
        assertThat(numberAt("[Geo].[All Geo].[North].[A]")).isEqualTo(10L);
        assertThat(numberAt("[Geo].[All Geo].[North]")).isEqualTo(30L);
        assertThat(numberAt("[Geo].[All Geo].[South]")).isEqualTo(120L);
        assertThat(numberAt("[Geo].[All Geo]")).isEqualTo(150L);

        // A whole total stays whole. Getting a Double here would be the conditional
        // operator promoting long and double together, which it does silently.
        assertThat(valueAt("[Geo].[All Geo]")).isInstanceOf(Long.class);
    }

    @Test
    @DisplayName("a coordinate nobody wrote down is an empty cell, not a zero")
    void unknownCoordinateIsEmpty() {
        MemoryMember lonely = geo.member("Elsewhere");
        assertThat(cells.valueAt(List.of(lonely, measureAmount()))).isNull();
    }

    @Test
    @DisplayName("the catalog reader walks the tree without consulting a backend")
    void catalogReaderNavigatesTheTree() {
        CatalogReader reader = catalog.getCatalogReaderWithDefaultRole();

        assertThat(reader.getCubes()).extracting(c -> c.getName()).containsExactly("Test");
        assertThat(reader.getHierarchyRootMembers(geo)).hasSize(1);

        Member allGeo = reader.getHierarchyRootMembers(geo).get(0);
        assertThat(reader.getMemberChildren(allGeo)).extracting(Member::getName)
                .containsExactly("North", "South");

        Member north = reader.getMemberChildren(allGeo).get(0);
        assertThat(reader.getMemberParent(north)).isEqualTo(allGeo);
        assertThat(reader.getMemberDepth(north)).isEqualTo(1);
        assertThat(reader.isDrillable(north)).isTrue();
        assertThat(reader.getLevelCardinality(north.getLevel(), false, false)).isEqualTo(2);
    }

    @Test
    @DisplayName("a context answers metadata and says plainly what it cannot do yet")
    void contextAnswersMetadata() {
        MemoryContext context = MemoryContext.over(catalog, null, null);

        assertThat(context.getName()).isEqualTo("Test");
        assertThat(context.getAccessRoles()).isEmpty();
        assertThat(context.getExpressionCompilerFactory()).isNotNull();
        assertThat(context.getCatalogReader().getCubes()).hasSize(1);

        // Not relational: the engine reaches SQL only through RelationalContext, so a
        // provider without a database never gets asked for a data source.
        assertThat(context).isNotInstanceOf(RelationalContext.class);
        // Running a query end to end is checked in the function module instead. It needs
        // the MDX function library on the classpath, and that module depends on this one.
        assertThat(context.getConnectionWithDefaultRole()).isNotNull();
    }

    @Test
    @DisplayName("a computed reader answers without a single stored cell")
    void computedReaderStoresNothing() {
        // This is the shape an embedder uses: the value is produced on demand, so the
        // provider holds no cells at all. Here it is the length of the member's name.
        CellReader computed = new ComputedCellReader(
                coordinate -> coordinate.get(0).getName().length());

        Evaluator evaluator = mock(Evaluator.class);
        when(evaluator.getMembers()).thenReturn(new Member[] { memberByUniqueName("[Geo].[All Geo].[North]") });

        assertThat(computed.get(evaluator)).isEqualTo(new ObjectValue(5));
        assertThat(computed.getMissCount()).isZero();
        assertThat(computed.isDirty()).isFalse();
    }

    @Test
    @DisplayName("a map reader answers through the same contract, and empty stays empty")
    void mapReaderAnswersThroughTheContract() {
        Evaluator evaluator = mock(Evaluator.class);
        when(evaluator.getMembers()).thenReturn(new Member[] {
                memberByUniqueName("[Geo].[All Geo].[South]"), measureAmount() });
        assertThat(cells.get(evaluator)).isEqualTo(new ObjectValue(120L));

        Evaluator elsewhere = mock(Evaluator.class);
        when(elsewhere.getMembers()).thenReturn(new Member[] {
                geo.member("Nowhere"), measureAmount() });
        assertThat(cells.get(elsewhere)).isEqualTo(NullValue.INSTANCE);
    }

    private long numberAt(String geoUniqueName) {
        return ((Number) valueAt(geoUniqueName)).longValue();
    }

    private Object valueAt(String geoUniqueName) {
        return cells.valueAt(List.of(memberByUniqueName(geoUniqueName), measureAmount()));
    }

    private Member measureAmount() {
        return cube.getMeasures().get(0);
    }

    private Member memberByUniqueName(String uniqueName) {
        for (Member m : allGeoMembers()) {
            if (m.getUniqueName().equals(uniqueName)) {
                return m;
            }
        }
        throw new IllegalArgumentException("no member " + uniqueName + "; have "
                + allGeoMembers().stream().map(Member::getUniqueName).toList());
    }

    private List<Member> allGeoMembers() {
        List<Member> all = new java.util.ArrayList<>();
        for (org.eclipse.daanse.olap.api.element.Level level : geo.getLevels()) {
            all.addAll(level.getMembers());
        }
        return all;
    }
}
