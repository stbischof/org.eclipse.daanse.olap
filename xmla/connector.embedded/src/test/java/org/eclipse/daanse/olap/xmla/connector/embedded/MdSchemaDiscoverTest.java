/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.xmla.connector.embedded;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.KPI;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.LevelType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.result.Property;
import org.eclipse.daanse.olap.element.PropertyBase;
import org.eclipse.daanse.olap.xmla.connector.ContextListSupplyer;
import org.eclipse.daanse.xmla.api.RowsetProvider;
import org.eclipse.daanse.xmla.api.RowsetScope;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaCubesRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaDimensionsRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaHierarchiesRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaKpisRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaLevelsRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaMeasuregroupDimensionsRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaMeasuregroupsRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaMeasuresRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaMembersRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaPropertiesRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaInputDatasourcesRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaSetsRow;
import org.eclipse.daanse.xmla.api.XmlaRequest;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * The bridge's {@code MDSchemaDiscoverServiceTest}, ported onto the connector:
 * the identical OLAP mock arrangements, the requests as model {@code Discover}
 * objects, the expected values asserted on the typed EMF rows.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MdSchemaDiscoverTest {

    @Mock
    private Catalog catalog;
    @Mock
    private KPI kpi1;
    @Mock
    private KPI kpi2;
    @Mock
    private Cube cube1;
    @Mock
    private Cube cube2;
    @Mock
    private Dimension dimension1;
    @Mock
    private Dimension dimension2;
    @Mock
    private Hierarchy hierarchy1;
    @Mock
    private Hierarchy hierarchy2;
    @Mock
    private Level level1;
    @Mock
    private Level level2;
    @Mock
    private Member measure1;
    @Mock
    private Member measure2;
    @Mock
    private ContextListSupplyer contexts;
    @Mock
    private Connection connection;
    @Mock
    private CatalogReader catalogReader;

    private final XmlaRequest anonymous = XmlaRequest.anonymous();

    @BeforeEach
    void setup() {
        when(contexts.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        when(contexts.getConnection(any(), any())).thenReturn(connection);
        when(connection.getCatalogReader()).thenReturn(catalogReader);
        when(catalog.getName()).thenReturn("schema2Name");
    }

    /**
     * One rowset through its provider - the whiteboard's dispatch, minus OSGi: the
     * provider for this request type, called with the scope the connector would
     * have built.
     */
    private List<EObject> discover(String requestType, Map<String, String> restrictions) {
        RowsetProvider<ContextListSupplyer> provider = Providers.of(requestType);
        return provider.rows(
                RowsetScope.of(Requests.discover(requestType, restrictions), anonymous, contexts, Providers.served()));
    }

    private void twoCubes() {
        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");
        when(catalogReader.getCubes()).thenAnswer(invocation -> List.of(cube1, cube2));
        when(catalog.getCubes()).thenAnswer(invocation -> List.of(cube1, cube2));
    }

    @Test
    void mdSchemaCubes() {
        twoCubes();
        when(cube2.getDescription()).thenReturn("cube2description");
        when(cube1.isVisible()).thenReturn(true);
        when(cube2.isVisible()).thenReturn(true);

        List<EObject> rows = discover("MDSCHEMA_CUBES", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(2);
        MdschemaCubesRow row = (MdschemaCubesRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("schema2Name");
        assertThat(row.getSchemaName()).isNull();
        assertThat(row.getDescription()).isEqualTo("schema2Name Schema - cube1Name Cube");
        assertThat(((MdschemaCubesRow) rows.get(1)).getDescription()).isEqualTo("cube2description");
    }

    /**
     * The column a client reads before it offers writeback at all. It used to say
     * FALSE for every cube, including the ones that declare a writeback table, so
     * the feature was there and could not be found.
     */
    @Test
    void mdSchemaCubesReportsWhichCubesCanBeWrittenTo() {
        twoCubes();
        when(cube1.isVisible()).thenReturn(true);
        when(cube2.isVisible()).thenReturn(true);
        when(cube2.isWriteEnabled()).thenReturn(true);

        List<EObject> rows = discover("MDSCHEMA_CUBES", Map.of("CATALOG_NAME", "foo"));

        assertThat(((MdschemaCubesRow) rows.get(0)).getIsWriteEnabled()).isFalse();
        assertThat(((MdschemaCubesRow) rows.get(1)).getIsWriteEnabled()).isTrue();
    }

    @Test
    void mdSchemaMeasureGroupsReportsTheSame() {
        twoCubes();
        when(cube2.isWriteEnabled()).thenReturn(true);

        List<EObject> rows = discover("MDSCHEMA_MEASUREGROUPS", Map.of("CATALOG_NAME", "foo"));

        assertThat(((MdschemaMeasuregroupsRow) rows.get(0)).getIsWriteEnabled()).isFalse();
        assertThat(((MdschemaMeasuregroupsRow) rows.get(1)).getIsWriteEnabled()).isTrue();
    }

    @Test
    void mdSchemaDimensions() {
        twoCubes();
        when(hierarchy1.getUniqueName()).thenReturn("hierarchy1UniqueName");
        when(hierarchy2.getUniqueName()).thenReturn("hierarchy2UniqueName");
        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(hierarchy2.getName()).thenReturn("hierarchy2Name");
        when(hierarchy1.getLevels()).thenAnswer(invocation -> List.of(level1));
        when(hierarchy2.getLevels()).thenAnswer(invocation -> List.of(level1));
        when(catalogReader.getHierarchyLevels(hierarchy1)).thenAnswer(invocation -> List.of(level1));
        when(catalogReader.getHierarchyLevels(hierarchy2)).thenAnswer(invocation -> List.of(level1));
        when(level1.getLevelType()).thenReturn(LevelType.REGULAR);
        when(dimension1.getName()).thenReturn("dimension1Name");
        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        when(dimension1.getCaption()).thenReturn("dimension1Caption");
        when(dimension2.getName()).thenReturn("dimension2Name");
        when(dimension2.getUniqueName()).thenReturn("dimension2UniqueName");
        when(dimension2.getCaption()).thenReturn("dimension2Caption");
        for (Dimension dimension : List.of(dimension1, dimension2)) {
            when(catalogReader.getDimensionHierarchies(dimension))
                    .thenAnswer(invocation -> List.of(hierarchy1, hierarchy2));
        }
        for (Cube cube : List.of(cube1, cube2)) {
            when(catalogReader.getCubeDimensions(cube)).thenAnswer(invocation -> List.of(dimension1, dimension2));
        }

        List<EObject> rows = discover("MDSCHEMA_DIMENSIONS", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(4);
        MdschemaDimensionsRow row = (MdschemaDimensionsRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("schema2Name");
        assertThat(row.getCubeName()).isEqualTo("cube1Name");
        assertThat(row.getDimensionName()).isEqualTo("dimension1Name");
        assertThat(row.getDimensionUniqueName()).isEqualTo("dimension1UniqueName");
        assertThat(row.getDimensionCaption()).isEqualTo("dimension1Caption");
        assertThat(row.getDimensionOrdinal()).isEqualTo(0L);
        assertThat(row.getDimensionCardinality()).isEqualTo(1L);
        assertThat(row.getDefaultHierarchy()).isEqualTo("hierarchy1UniqueName");
        assertThat(row.getDescription()).isEqualTo("cube1Name Cube - dimension1Name Dimension");
        MdschemaDimensionsRow last = (MdschemaDimensionsRow) rows.get(3);
        assertThat(last.getCubeName()).isEqualTo("cube2Name");
        assertThat(last.getDimensionName()).isEqualTo("dimension2Name");
        assertThat(last.getDimensionOrdinal()).isEqualTo(1L);
    }

    @Test
    void mdSchemaHierarchies() {
        mdSchemaDimensionsSetupForHierarchies();

        List<EObject> rows = discover("MDSCHEMA_HIERARCHIES", Map.of("CATALOG_NAME", "schema2Name"));
        assertThat(rows).hasSize(8);
        MdschemaHierarchiesRow row = (MdschemaHierarchiesRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("schema2Name");
        assertThat(row.getCubeName()).isEqualTo("cube1Name");
        assertThat(row.getDimensionUniqueName()).isEqualTo("dimension1UniqueName");
        assertThat(row.getHierarchyName()).isEqualTo("hierarchy1Name");
        assertThat(row.getHierarchyUniqueName()).isEqualTo("hierarchy1UniqueName");
        assertThat(row.getHierarchyOrdinal()).isEqualTo(0L);
        assertThat(row.getDescription()).isEqualTo("cube1Name Cube - dimension1Name.hierarchy1Name Hierarchy");
        MdschemaHierarchiesRow second = (MdschemaHierarchiesRow) rows.get(1);
        assertThat(second.getDimensionUniqueName()).isEqualTo("dimension2UniqueName");
        assertThat(second.getHierarchyOrdinal()).isEqualTo(2L);
    }

    private void mdSchemaDimensionsSetupForHierarchies() {
        twoCubes();
        when(hierarchy1.getUniqueName()).thenReturn("hierarchy1UniqueName");
        when(hierarchy2.getUniqueName()).thenReturn("hierarchy2UniqueName");
        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(hierarchy2.getName()).thenReturn("hierarchy2Name");
        when(hierarchy1.getLevels()).thenAnswer(invocation -> List.of(level1));
        when(hierarchy2.getLevels()).thenAnswer(invocation -> List.of(level1));
        when(catalogReader.getHierarchyLevels(hierarchy1)).thenAnswer(invocation -> List.of(level1));
        when(catalogReader.getHierarchyLevels(hierarchy2)).thenAnswer(invocation -> List.of(level1));
        when(level1.getLevelType()).thenReturn(LevelType.REGULAR);
        when(dimension1.getName()).thenReturn("dimension1Name");
        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        when(dimension2.getName()).thenReturn("dimension2Name");
        when(dimension2.getUniqueName()).thenReturn("dimension2UniqueName");
        for (Dimension dimension : List.of(dimension1, dimension2)) {
            when(catalogReader.getDimensionHierarchies(dimension))
                    .thenAnswer(invocation -> List.of(hierarchy1, hierarchy2));
            when(dimension.getHierarchies()).thenAnswer(invocation -> List.of(hierarchy1, hierarchy2));
        }
        for (Cube cube : List.of(cube1, cube2)) {
            when(catalogReader.getCubeDimensions(cube)).thenAnswer(invocation -> List.of(dimension1, dimension2));
            when(cube.getLevelCardinality(any(), eq(true), eq(true))).thenReturn(1);
        }
    }

    @Test
    void mdSchemaKpis() {
        twoCubes();
        when(catalog.getName()).thenReturn("foo");
        for (Cube cube : List.of(cube1, cube2)) {
            when(cube.getKPIs()).thenAnswer(invocation -> List.of(kpi1, kpi2));
        }
        when(kpi1.getName()).thenReturn("kpi1Name");
        when(kpi2.getName()).thenReturn("kpi2Name");
        when(kpi2.getDescription()).thenReturn("kpi2Description");
        when(kpi2.getDisplayFolder()).thenReturn("kpi2DisplayFolder");
        when(kpi2.getValue()).thenReturn("kpi2Value");
        when(kpi2.getGoal()).thenReturn("kpi2Goal");
        when(kpi2.getStatus()).thenReturn("kpi2Status");
        when(kpi2.getTrend()).thenReturn("kpi2Trend");
        when(kpi2.getWeight()).thenReturn("kpi2Weight");
        when(kpi2.getTrendGraphic()).thenReturn("kpi2TrendGraphic");
        when(kpi2.getStatusGraphic()).thenReturn("kpi2StatusGraphic");
        when(kpi2.getCurrentTimeMember()).thenReturn("kpi2CurrentTimeMember");
        when(kpi2.getParentKpi()).thenReturn(kpi1);

        List<EObject> rows = discover("MDSCHEMA_KPIS", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(4);
        MdschemaKpisRow row = (MdschemaKpisRow) rows.get(1);
        assertThat(row.getCatalogName()).isEqualTo("foo");
        assertThat(row.getCubeName()).isEqualTo("cube1Name");
        assertThat(row.getKpiName()).isEqualTo("kpi2Name");
        assertThat(row.getKpiDescription()).isEqualTo("kpi2Description");
        assertThat(row.getKpiValue()).isEqualTo("kpi2Value");
        assertThat(row.getKpiParentKpiName()).isEqualTo("kpi1Name");
    }

    @Test
    void mdSchemaLevels() {
        twoCubes();
        when(level1.getName()).thenReturn("level1Name");
        when(level2.getName()).thenReturn("level2Name");
        when(level1.getUniqueName()).thenReturn("level1UniqueName");
        when(level2.getUniqueName()).thenReturn("level2UniqueName");
        when(level1.isAll()).thenReturn(true);
        when(level2.isAll()).thenReturn(true);
        when(level1.getCaption()).thenReturn("level1Caption");
        when(level2.getCaption()).thenReturn("level2Caption");
        when(level2.getDescription()).thenReturn("level2Description");
        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(hierarchy2.getName()).thenReturn("hierarchy2Name");
        when(hierarchy1.getUniqueName()).thenReturn("hierarchy1UniqueName");
        when(hierarchy2.getUniqueName()).thenReturn("hierarchy2UniqueName");
        when(catalogReader.getHierarchyLevels(hierarchy1)).thenAnswer(invocation -> List.of(level1, level2));
        when(catalogReader.getHierarchyLevels(hierarchy2)).thenAnswer(invocation -> List.of(level1, level2));
        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        when(dimension2.getUniqueName()).thenReturn("dimension2UniqueName");
        for (Dimension dimension : List.of(dimension1, dimension2)) {
            when(catalogReader.getDimensionHierarchies(dimension))
                    .thenAnswer(invocation -> List.of(hierarchy1, hierarchy2));
        }
        for (Cube cube : List.of(cube1, cube2)) {
            when(catalogReader.getCubeDimensions(cube)).thenAnswer(invocation -> List.of(dimension1, dimension2));
            when(cube.getLevelCardinality(any(), eq(true), eq(true))).thenReturn(1);
        }

        List<EObject> rows = discover("MDSCHEMA_LEVELS", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(16);
        MdschemaLevelsRow row = (MdschemaLevelsRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("schema2Name");
        assertThat(row.getCubeName()).isEqualTo("cube1Name");
        assertThat(row.getDimensionUniqueName()).isEqualTo("dimension1UniqueName");
        assertThat(row.getHierarchyUniqueName()).isEqualTo("hierarchy1UniqueName");
        assertThat(row.getLevelName()).isEqualTo("level1Name");
        assertThat(row.getLevelUniqueName()).isEqualTo("level1UniqueName");
        assertThat(row.getLevelCaption()).isEqualTo("level1Caption");
        MdschemaLevelsRow second = (MdschemaLevelsRow) rows.get(1);
        assertThat(second.getDescription()).isEqualTo("level2Description");
        MdschemaLevelsRow last = (MdschemaLevelsRow) rows.get(15);
        assertThat(last.getCubeName()).isEqualTo("cube2Name");
        assertThat(last.getDimensionUniqueName()).isEqualTo("dimension2UniqueName");
        assertThat(last.getLevelName()).isEqualTo("level2Name");
    }

    @Test
    void mdSchemaMeasureGroupDimensions() {
        twoCubes();
        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        when(dimension2.getUniqueName()).thenReturn("dimension2UniqueName");
        for (Cube cube : List.of(cube1, cube2)) {
            when(cube.getDimensions()).thenAnswer(invocation -> List.of(dimension1, dimension2));
        }

        List<EObject> rows = discover("MDSCHEMA_MEASUREGROUP_DIMENSIONS", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(4);
        MdschemaMeasuregroupDimensionsRow row = (MdschemaMeasuregroupDimensionsRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("schema2Name");
        assertThat(row.getCubeName()).isEqualTo("cube1Name");
        assertThat(row.getMeasureGroupCardinality()).isEqualTo("ONE");
        assertThat(row.getDimensionUniqueName()).isEqualTo("dimension1UniqueName");
        MdschemaMeasuregroupDimensionsRow last = (MdschemaMeasuregroupDimensionsRow) rows.get(3);
        assertThat(last.getCubeName()).isEqualTo("cube2Name");
        assertThat(last.getDimensionUniqueName()).isEqualTo("dimension2UniqueName");
    }

    @Test
    void mdSchemaMeasureGroups() {
        twoCubes();
        when(catalog.getName()).thenReturn("foo");

        List<EObject> rows = discover("MDSCHEMA_MEASUREGROUPS", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(2);
        MdschemaMeasuregroupsRow row = (MdschemaMeasuregroupsRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("foo");
        assertThat(row.getCubeName()).isEqualTo("cube1Name");
        assertThat(row.getIsWriteEnabled()).isFalse();
        assertThat(((MdschemaMeasuregroupsRow) rows.get(1)).getCubeName()).isEqualTo("cube2Name");
    }

    @Test
    void mdSchemaMeasures() {
        twoCubes();
        for (Dimension dimension : List.of(dimension1, dimension2)) {
            when(dimension.getHierarchies()).thenAnswer(invocation -> List.of(hierarchy1, hierarchy2));
        }
        when(level1.getUniqueName()).thenReturn("name");
        when(hierarchy1.getLevels()).thenAnswer(invocation -> List.of(level1));
        when(hierarchy2.getLevels()).thenAnswer(invocation -> List.of(level1));
        when(measure1.getName()).thenReturn("measure1Name");
        when(measure2.getName()).thenReturn("measure2Name");
        when(measure1.getUniqueName()).thenReturn("measure1UniqueName");
        when(measure2.getUniqueName()).thenReturn("measure2UniqueName");
        when(measure1.getCaption()).thenReturn("measure1Caption");
        when(measure2.getCaption()).thenReturn("measure2Caption");
        when(measure1.getPropertyValue("$visible")).thenReturn(Boolean.TRUE);
        when(measure2.getPropertyValue("$visible")).thenReturn(Boolean.TRUE);
        when(measure1.getPropertyValue(Property.StandardCellProperty.FORMAT_STRING.getName()))
                .thenReturn("formatString1");
        when(measure2.getPropertyValue(Property.StandardCellProperty.FORMAT_STRING.getName()))
                .thenReturn("formatString2");
        for (Cube cube : List.of(cube1, cube2)) {
            when(cube.getDimensions()).thenAnswer(invocation -> List.of(dimension1, dimension2));
        }
        when(catalogReader.getLevelMembers(any(Level.class), any(Boolean.class)))
                .thenAnswer(invocation -> List.of(measure1, measure2));

        List<EObject> rows = discover("MDSCHEMA_MEASURES", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(4);
        MdschemaMeasuresRow row = (MdschemaMeasuresRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("schema2Name");
        assertThat(row.getCubeName()).isEqualTo("cube1Name");
        assertThat(row.getMeasureName()).isEqualTo("measure1Name");
        assertThat(row.getMeasureUniqueName()).isEqualTo("measure1UniqueName");
        assertThat(row.getMeasureCaption()).isEqualTo("measure1Caption");
        assertThat(row.getDescription()).isEqualTo("cube1Name Cube - measure1Name Member");
        assertThat(row.getLevelsList()).isEqualTo("name,name,name,name");
        assertThat(row.getDefaultFormatString()).isEqualTo("formatString1");
        MdschemaMeasuresRow last = (MdschemaMeasuresRow) rows.get(3);
        assertThat(last.getCubeName()).isEqualTo("cube2Name");
        assertThat(last.getMeasureName()).isEqualTo("measure2Name");
        assertThat(last.getDefaultFormatString()).isEqualTo("formatString2");
    }

    @Test
    void mdSchemaMembers() {
        twoCubes();
        when(level1.getUniqueName()).thenReturn("level1UniqueName");
        when(hierarchy1.getUniqueName()).thenReturn("hierarchy1UniqueName");
        when(hierarchy1.getDimension()).thenReturn(dimension1);
        when(hierarchy1.getLevels()).thenAnswer(invocation -> List.of(level1, level2));
        when(hierarchy2.getLevels()).thenAnswer(invocation -> List.of(level1, level2));
        for (Dimension dimension : List.of(dimension1, dimension2)) {
            when(dimension.getHierarchies()).thenAnswer(invocation -> List.of(hierarchy1, hierarchy2));
        }
        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        Member member = mock(Member.class);
        when(member.getUniqueName()).thenReturn("memberUniqueName");
        when(member.getName()).thenReturn("measure1Name");
        when(member.getDescription()).thenReturn("measure1Description");
        when(member.getCaption()).thenReturn("measure1Caption");
        when(member.getLevel()).thenReturn(level1);
        when(level1.getHierarchy()).thenReturn(hierarchy1);
        for (Cube cube : List.of(cube1, cube2)) {
            when(cube.getDimensions()).thenAnswer(invocation -> List.of(dimension1, dimension2));
            when(cube.getLevelMembers(any(), eq(true))).thenAnswer(invocation -> List.of(member));
        }

        List<EObject> rows = discover("MDSCHEMA_MEMBERS",
                Map.of("CATALOG_NAME", "foo", "MEMBER_UNIQUE_NAME", "memberUniqueName"));
        assertThat(rows).hasSize(8);
        MdschemaMembersRow row = (MdschemaMembersRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("schema2Name");
        assertThat(row.getCubeName()).isEqualTo("cube1Name");
        assertThat(row.getDimensionUniqueName()).isEqualTo("dimension1UniqueName");
        assertThat(row.getHierarchyUniqueName()).isEqualTo("hierarchy1UniqueName");
        assertThat(row.getLevelUniqueName()).isEqualTo("level1UniqueName");
        assertThat(row.getMemberName()).isEqualTo("measure1Name");
        assertThat(row.getMemberUniqueName()).isEqualTo("memberUniqueName");
        assertThat(row.getMemberCaption()).isEqualTo("measure1Caption");
        assertThat(row.getDescription()).isEqualTo("measure1Description");
        assertThat(((MdschemaMembersRow) rows.get(7)).getCubeName()).isEqualTo("cube2Name");
    }

    @Test
    void mdSchemaProperties() {
        twoCubes();
        PropertyBase property1 = mock(PropertyBase.class);
        PropertyBase property2 = mock(PropertyBase.class);
        when(property1.getName()).thenReturn("property1Name");
        when(property2.getName()).thenReturn("property2Name");
        when(property1.getCaption()).thenReturn("property1Caption");
        when(property2.getCaption()).thenReturn("property2Caption");
        when(level1.getUniqueName()).thenReturn("level1UniqueName");
        when(level1.getHierarchy()).thenReturn(hierarchy1);
        when(level1.getProperties()).thenReturn(new PropertyBase[] { property1, property2 });
        when(level1.getName()).thenReturn("level1Name");
        when(level2.getUniqueName()).thenReturn("level2UniqueName");
        when(level2.getHierarchy()).thenReturn(hierarchy2);
        when(level2.getProperties()).thenReturn(new PropertyBase[] { property1, property2 });
        when(level2.getName()).thenReturn("level2Name");
        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(hierarchy1.getUniqueName()).thenReturn("hierarchy1UniqueName");
        when(hierarchy1.getDimension()).thenReturn(dimension1);
        when(hierarchy1.getLevels()).thenAnswer(invocation -> List.of(level1, level2));
        when(hierarchy2.getName()).thenReturn("hierarchy2Name");
        when(hierarchy2.getUniqueName()).thenReturn("hierarchy2UniqueName");
        when(hierarchy2.getDimension()).thenReturn(dimension2);
        when(hierarchy2.getLevels()).thenAnswer(invocation -> List.of(level1, level2));
        when(dimension1.getName()).thenReturn("dimension1Name");
        when(dimension2.getName()).thenReturn("dimension2Name");
        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        when(dimension2.getUniqueName()).thenReturn("dimension2UniqueName");
        for (Dimension dimension : List.of(dimension1, dimension2)) {
            when(dimension.getHierarchies()).thenAnswer(invocation -> List.of(hierarchy1, hierarchy2));
        }
        for (Cube cube : List.of(cube1, cube2)) {
            when(cube.getDimensions()).thenAnswer(invocation -> List.of(dimension1, dimension2));
        }

        List<EObject> rows = discover("MDSCHEMA_PROPERTIES", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(32);
        MdschemaPropertiesRow row = (MdschemaPropertiesRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("schema2Name");
        assertThat(row.getCubeName()).isEqualTo("cube1Name");
        assertThat(row.getDimensionUniqueName()).isEqualTo("dimension1UniqueName");
        assertThat(row.getHierarchyUniqueName()).isEqualTo("hierarchy1UniqueName");
        assertThat(row.getLevelUniqueName()).isEqualTo("level1UniqueName");
        assertThat(row.getPropertyName()).isEqualTo("property1Name");
        assertThat(row.getPropertyCaption()).isEqualTo("property1Caption");
        assertThat(row.getDescription()).isEqualTo("cube1Name Cube - dimension1Name."
                + "hierarchy1Name Hierarchy - level1Name Level - property1Name Property");
        MdschemaPropertiesRow last = (MdschemaPropertiesRow) rows.get(31);
        assertThat(last.getCubeName()).isEqualTo("cube2Name");
        assertThat(last.getPropertyName()).isEqualTo("property2Name");
    }

    @Test
    void mdSchemaSets() {
        twoCubes();
        NamedSet namedSet1 = mock(NamedSet.class);
        NamedSet namedSet2 = mock(NamedSet.class);
        when(namedSet1.getName()).thenReturn("set1Name");
        when(namedSet2.getName()).thenReturn("set2Name");
        when(namedSet1.getDescription()).thenReturn("set1Description");
        when(namedSet2.getDescription()).thenReturn("set2Description");
        when(namedSet1.getCaption()).thenReturn("set1Caption");
        when(namedSet2.getCaption()).thenReturn("set2Caption");
        for (Cube cube : List.of(cube1, cube2)) {
            when(cube.getNamedSets()).thenReturn(new NamedSet[] { namedSet1, namedSet2 });
        }

        List<EObject> rows = discover("MDSCHEMA_SETS", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(4);
        MdschemaSetsRow row = (MdschemaSetsRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("schema2Name");
        assertThat(row.getCubeName()).isEqualTo("cube1Name");
        assertThat(row.getSetName()).isEqualTo("set1Name");
        assertThat(row.getDescription()).isEqualTo("set1Description");
        assertThat(row.getSetCaption()).isEqualTo("set1Caption");
        MdschemaSetsRow last = (MdschemaSetsRow) rows.get(3);
        assertThat(last.getCubeName()).isEqualTo("cube2Name");
        assertThat(last.getSetName()).isEqualTo("set2Name");
    }

    // --- MDSCHEMA_INPUT_DATASOURCES ---

    /**
     * One row per catalog, naming the store behind it. This was the last rowset a
     * recorded Analysis Services advertises that carries metadata rather than
     * administration, and that this server did not answer.
     */
    @Test
    void inputDatasourcesDescribeTheStoreBehindTheCatalog() {
        org.eclipse.daanse.olap.api.relational.RelationalContext<?> context = org.mockito.Mockito
                .mock(org.eclipse.daanse.olap.api.relational.RelationalContext.class);
        org.eclipse.daanse.sql.dialect.api.Dialect dialect = org.mockito.Mockito
                .mock(org.eclipse.daanse.sql.dialect.api.Dialect.class);
        when(dialect.name()).thenReturn("DUCKDB");
        when(context.getDialect()).thenReturn(dialect);
        when(context.getDescription()).thenReturn(Optional.of("the sales store"));
        when(contexts.getContext("schema2Name")).thenReturn(Optional.of(context));

        List<EObject> rows = discover("MDSCHEMA_INPUT_DATASOURCES", Map.of("CATALOG_NAME", "foo"));

        assertThat(rows).hasSize(1);
        MdschemaInputDatasourcesRow row = (MdschemaInputDatasourcesRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("schema2Name");
        assertThat(row.getDatasourceName()).isEqualTo("schema2Name");
        assertThat(row.getDatasourceType()).isEqualTo("Relational");
        assertThat(row.getDescription()).isEqualTo("the sales store");
        assertThat(row.getDbmsName()).isEqualTo("DUCKDB");
        // Absent, not "now": a client caches metadata on these, and a moving
        // timestamp makes it re-read everything after every query.
        assertThat(row.getCreatedOn()).isNull();
        assertThat(row.getLastSchemaUpdate()).isNull();
    }

    /**
     * A restriction narrows or it matches nothing - it never widens. SCHEMA_NAME is
     * a column this server never fills, so any value for it matches no row.
     */
    @Test
    void inputDatasourcesHonourTheirRestrictions() {
        org.eclipse.daanse.olap.api.Context<?> context = org.mockito.Mockito
                .mock(org.eclipse.daanse.olap.api.Context.class);
        when(contexts.getContext("schema2Name")).thenReturn(Optional.of(context));

        assertThat(discover("MDSCHEMA_INPUT_DATASOURCES", Map.of("CATALOG_NAME", "foo", "SCHEMA_NAME", "dbo")))
                .as("a column this server never fills matches nothing").isEmpty();
        assertThat(discover("MDSCHEMA_INPUT_DATASOURCES", Map.of("CATALOG_NAME", "foo", "DATASOURCE_TYPE", "Olap")))
                .as("the other of the two values the specification allows").isEmpty();
        assertThat(discover("MDSCHEMA_INPUT_DATASOURCES", Map.of("CATALOG_NAME", "foo", "DATASOURCE_NAME", "elsewhere")))
                .isEmpty();
        assertThat(discover("MDSCHEMA_INPUT_DATASOURCES", Map.of("CATALOG_NAME", "foo", "DATASOURCE_TYPE", "relational")))
                .as("case is not what distinguishes Relational from Olap").hasSize(1);
        assertThat(
                discover("MDSCHEMA_INPUT_DATASOURCES", Map.of("CATALOG_NAME", "foo", "DATASOURCE_NAME", "schema2Name")))
                .hasSize(1);
    }
}
