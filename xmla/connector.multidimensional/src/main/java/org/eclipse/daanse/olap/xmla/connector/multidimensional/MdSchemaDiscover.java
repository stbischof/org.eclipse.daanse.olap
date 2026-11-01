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
package org.eclipse.daanse.olap.xmla.connector.multidimensional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.xmla.api.XmlaRequest;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.DimensionType;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.KPI;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.xmla.connector.ContextListSupplyer;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.StoredMeasure;
import org.eclipse.daanse.olap.api.result.Property;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaCubesRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaDimensionsRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaHierarchiesRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaLevelsRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaMeasuresRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaMeasuregroupDimensionsRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaInputDatasourcesRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaMeasuregroupsRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaMembersRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaPropertiesRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaKpisRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.MdschemaSetsRow;
import org.eclipse.daanse.xmla.model.rowset.multidimensional.RowsetMultidimensionalFactory;
import org.eclipse.daanse.xmla.api.RestrictionValues;
import org.eclipse.emf.ecore.EObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.daanse.olap.xmla.connector.common.DiscoverScope;
import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.eclipse.daanse.olap.xmla.connector.common.OleDbType;
import org.eclipse.daanse.olap.api.relational.RelationalContext;

/**
 * The MDSCHEMA_* rowsets: cubes, dimensions, hierarchies, levels, measures,
 * members and their kin, read from the OLAP metadata.
 * <p>
 * Ported from the bridge's {@code MDSchemaDiscoverService}. The port is in
 * progress; a rowset not yet carried over answers empty, which the transport
 * serves as a rowset with no rows.
 */
public class MdSchemaDiscover {

    private static final Logger LOGGER = LoggerFactory.getLogger(MdSchemaDiscover.class);
    private static final RowsetMultidimensionalFactory FACTORY = RowsetMultidimensionalFactory.eINSTANCE;

    /** MDSCHEMA_CUBES' CUBE_SOURCE mask: 1 = cubes, 2 = dimensions. */
    private static final int CUBE_SOURCE_CUBE = 1;
    private static final int CUBE_SOURCE_DIMENSION = 2;

    private final ContextListSupplyer contexts;
    private final org.eclipse.daanse.olap.api.function.FunctionTextService functionTexts = new org.eclipse.daanse.olap.function.core.text.FunctionTextServiceImpl();

    /**
     * The action services the connector has been handed; empty when none are
     * registered.
     */
    private final java.util.List<org.eclipse.daanse.olap.api.action.Action> actions;

    public MdSchemaDiscover(ContextListSupplyer contexts,
            java.util.List<org.eclipse.daanse.olap.api.action.Action> actions) {
        this.actions = actions;
        this.contexts = contexts;
    }

    // --- MDSCHEMA_ACTIONS, ported from the bridge's ActionServiceImpl ---

    public List<EObject> actions(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            ActionRows.collect(catalog, actions, restrictions, result);
        }
        return result;
    }

    // --- MDSCHEMA_CUBES, ported from Utils.getMdSchemaCubesResponseRow ---

    public List<EObject> cubes(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");
        Optional<String> cubeSource = restrictions.value("CUBE_SOURCE");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            Connection connection = contexts.getConnection(caller, catalog.getName());
            List<Cube> cubes = connection.getCatalogReader().getCubes();

            int source = CUBE_SOURCE_CUBE;
            if (cubeSource.isPresent()) {
                source = Integer.parseInt(cubeSource.get());
            }
            if ((source & CUBE_SOURCE_CUBE) != 0) {
                for (Cube cube : filterByName(cubes, cubeName)) {
                    EObject row = cubeRow(catalog.getName(), cube);
                    if (row != null) {
                        result.add(row);
                    }
                }
            }
            if ((source & CUBE_SOURCE_DIMENSION) != 0) {
                // A dimension row is named $<dimension>, so CUBE_NAME is matched against
                // that name - not against the cube the dimension is found in.
                for (Cube cube : cubes) {
                    for (Dimension dimension : connection.getCatalogReader().getCubeDimensions(cube)) {
                        if (!dimension.isMeasures()
                                && cubeName.map(name -> name.equals("$" + dimension.getName())).orElse(true)) {
                            EObject row = dimensionAsCubeRow(catalog.getName(), dimension);
                            if (row != null) {
                                result.add(row);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * One MDSCHEMA_CUBES row for a real cube, or {@code null} for an invisible one.
     */
    private static EObject cubeRow(String catalogName, Cube cube) {
        if (cube == null || !cube.isVisible()) {
            return null;
        }
        String description = cube.getDescription();
        if (description == null) {
            description = catalogName + " Schema - " + cube.getName() + " Cube";
        }
        MdschemaCubesRow row = FACTORY.createMdschemaCubesRow();
        row.setCatalogName(catalogName);
        row.setCubeName(cube.getName());
        row.setCubeType("CUBE");
        row.setCreatedOn(startOfToday());
        row.setLastSchemaUpdate(startOfToday());
        row.setLastDataUpdate(startOfToday());
        row.setDescription(description);
        row.setIsDrillThroughEnabled(Boolean.TRUE);
        row.setIsLinkable(Boolean.TRUE);
        row.setIsWriteEnabled(cube.isWriteEnabled());
        row.setIsSqlEnabled(Boolean.TRUE);
        row.setCubeCaption(cube.getCaption() == null ? cube.getName() : cube.getCaption());
        //row.setBaseCubeName(cube.getName());
        row.setCubeSource(CUBE_SOURCE_CUBE);
        row.setPreferredQueryPatterns(1);
        return row;
    }

    /**
     * One MDSCHEMA_CUBES row for a shared dimension, named {@code $Dimension} — the
     * convention by which dimensions appear as queryable cubes when CUBE_SOURCE
     * asks for them.
     */
    private static EObject dimensionAsCubeRow(String catalogName, Dimension dimension) {
        if (dimension == null || !dimension.isVisible()) {
            return null;
        }
        String description = dimension.getDescription();
        if (description == null) {
            description = catalogName + " Schema - " + dimension.getName() + " Dimension";
        }
        MdschemaCubesRow row = FACTORY.createMdschemaCubesRow();
        row.setCatalogName(catalogName);
        row.setCubeName("$" + dimension.getName());
        row.setCubeType("CUBE");
        row.setCreatedOn(startOfToday());
        row.setLastDataUpdate(startOfToday());
        row.setDescription(description);
        row.setIsDrillThroughEnabled(Boolean.TRUE);
        row.setIsLinkable(Boolean.TRUE);
        row.setIsWriteEnabled(Boolean.FALSE);
        row.setIsSqlEnabled(Boolean.TRUE);
        row.setCubeCaption(dimension.getCaption() == null ? "$" + dimension.getName() : dimension.getCaption());
        row.setCubeSource(CUBE_SOURCE_DIMENSION);
        row.setPreferredQueryPatterns(1);
        return row;
    }

    // --- MDSCHEMA_DIMENSIONS, ported from Utils.getMdSchemaDimensionsResponseRow
    // ---

    public List<EObject> dimensions(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");
        Optional<String> dimensionName = restrictions.value("DIMENSION_NAME");
        Optional<String> dimensionUniqueName = restrictions.value("DIMENSION_UNIQUE_NAME");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            Connection connection = contexts.getConnection(caller, catalog.getName());
            CatalogReader reader = connection.getCatalogReader();
            for (Cube cube : filterByName(reader.getCubes(), cubeName)) {
                List<? extends Dimension> dimensions = reader.getCubeDimensions(cube);
                for (int index = 0; index < dimensions.size(); index++) {
                    Dimension dimension = dimensions.get(index);
                    if (dimensionUniqueName.isPresent()
                            && !dimensionUniqueName.get().equals(dimension.getUniqueName())) {
                        continue;
                    }
                    if (dimensionName.isPresent() && !dimensionName.get().equals(dimension.getName())) {
                        continue;
                    }
                    result.add(dimensionRow(reader, catalog.getName(), cube, dimension, index));
                }
            }
        }
        // Sorted by unique name, as the bridge sorted and the recorded servers answer.
        result.sort((left, right) -> {
            String a = ((MdschemaDimensionsRow) left).getDimensionUniqueName();
            String b = ((MdschemaDimensionsRow) right).getDimensionUniqueName();
            return (a == null ? "" : a).compareTo(b == null ? "" : b);
        });
        return result;
    }

    private static EObject dimensionRow(CatalogReader reader, String catalogName, Cube cube, Dimension dimension,
            int index) {
        String description = dimension.getDescription();
        if (description == null) {
            description = cube.getName() + " Cube - " + dimension.getName() + " Dimension";
        }

        // The first hierarchy is the default one, and the key attribute's cardinality
        // is the
        // last level of that hierarchy plus the all member - the same reckoning the
        // bridge
        // inherited from the SQL Server comparison runs.
        String firstHierarchyUniqueName = null;
        Level lastLevel = null;
        List<Hierarchy> hierarchies = reader.getDimensionHierarchies(dimension);
        if (hierarchies != null && !hierarchies.isEmpty()) {
            Hierarchy first = hierarchies.getFirst();
            firstHierarchyUniqueName = first.getUniqueName();
            if (first.getLevels() != null && !first.getLevels().isEmpty()) {
                lastLevel = first.getLevels().getLast();
            }
        }
        int cardinality = 1 + (lastLevel == null ? 0 : cube.getLevelCardinality(lastLevel, true, true));

        MdschemaDimensionsRow row = FACTORY.createMdschemaDimensionsRow();
        row.setCatalogName(catalogName);
        row.setSchemaName(catalogName);
        row.setCubeName(cube.getName());
        row.setDimensionName(dimension.getName());
        row.setDimensionUniqueName(dimension.getUniqueName());
        if (dimension.getCaption() != null) {
            row.setDimensionCaption(dimension.getCaption());
        }
        row.setDimensionOrdinal((long) index);
        Short type = dimensionType(dimension.getDimensionType());
        if (type != null) {
            row.setDimensionType(type);
        }
        row.setDimensionCardinality((long) cardinality);
        if (firstHierarchyUniqueName != null) {
            row.setDefaultHierarchy(firstHierarchyUniqueName);
        }
        row.setDescription(description);
        row.setIsVirtual(Boolean.FALSE);
        // SQL Server always returns false.
        row.setIsReadWrite(Boolean.FALSE);
        row.setDimensionUniqueSettings(0);
        row.setDimensionIsVisible(dimension.isVisible());
        return row;
    }

    /** MD_DIMTYPE: 1 = time, 2 = measure, 3 = other. */
    private static Short dimensionType(DimensionType dimensionType) {
        if (dimensionType == null) {
            return null;
        }
        switch (dimensionType) {
        case TIME_DIMENSION:
            return Short.valueOf((short) 1);
        case MEASURES_DIMENSION:
            return Short.valueOf((short) 2);
        case STANDARD_DIMENSION:
            return Short.valueOf((short) 3);
        default:
            throw new IllegalArgumentException("Wrong dimension type " + dimensionType);
        }
    }

    // --- MDSCHEMA_HIERARCHIES, ported from Utils.getMdSchemaHierarchiesResponseRow
    // ---

    public List<EObject> hierarchies(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");
        Optional<String> dimensionUniqueName = restrictions.value("DIMENSION_UNIQUE_NAME");
        Optional<String> hierarchyName = restrictions.value("HIERARCHY_NAME");
        Optional<String> hierarchyUniqueName = restrictions.value("HIERARCHY_UNIQUE_NAME");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            Connection connection = contexts.getConnection(caller, catalog.getName());
            CatalogReader reader = connection.getCatalogReader();
            for (Cube cube : filterByName(reader.getCubes(), cubeName)) {
                // The ordinal numbers hierarchies across the whole cube, dimension by
                // dimension, and keeps counting past filtered-out dimensions - the position is
                // a property of the cube, not of the answer.
                int ordinal = 0;
                for (Dimension dimension : reader.getCubeDimensions(cube)) {
                    if (dimensionUniqueName.isEmpty() || dimensionUniqueName.get().equals(dimension.getUniqueName())) {
                        int inDimension = 0;
                        for (Hierarchy hierarchy : reader.getDimensionHierarchies(dimension)) {
                            if (hierarchyName.isPresent() && !hierarchyName.get().equals(hierarchy.getName())) {
                                inDimension++;
                                continue;
                            }
                            if (hierarchyUniqueName.isPresent()
                                    && !hierarchyUniqueName.get().equals(hierarchy.getUniqueName())) {
                                inDimension++;
                                continue;
                            }
                            result.add(hierarchyRow(reader, cube, catalog.getName(), dimension, hierarchy,
                                    ordinal + inDimension));
                            inDimension++;
                        }
                    }
                    ordinal += dimension.getHierarchies().size();
                }
            }
        }
        result.sort((left, right) -> {
            String a = ((MdschemaHierarchiesRow) left).getHierarchyUniqueName();
            String b = ((MdschemaHierarchiesRow) right).getHierarchyUniqueName();
            return (a == null ? "" : a).compareTo(b == null ? "" : b);
        });
        return result;
    }

    private static EObject hierarchyRow(CatalogReader reader, Cube cube, String catalogName, Dimension dimension,
            Hierarchy hierarchy, int ordinal) {
        String description = hierarchy.getDescription();
        if (description == null) {
            description = cube.getName() + " Cube - " + DiscoverScope.hierarchyName(hierarchy.getName(), dimension.getName())
                    + " Hierarchy";
        }

        // HIERARCHY_ORIGIN bitmask: 1 user defined, 2 attribute, 4 key attribute, 8
        // internal.
        int origin;
        if (Dimension.MEASURES_UNIQUE_NAME.equals(dimension.getUniqueName())) {
            origin = 6;
        } else {
            origin = hierarchy.origin() != null ? Integer.parseInt(hierarchy.origin()) : 1;
        }

        List<Member> firstLevelMembers = reader.getLevelMembers(hierarchy.getLevels().getFirst(), true);

        MdschemaHierarchiesRow row = FACTORY.createMdschemaHierarchiesRow();
        row.setCatalogName(catalogName);
        row.setSchemaName(catalogName);
        row.setCubeName(cube.getName());
        row.setDimensionUniqueName(dimension.getUniqueName());
        row.setHierarchyName(hierarchy.getName());
        row.setHierarchyUniqueName(hierarchy.getUniqueName());
        if (hierarchy.getCaption() != null) {
            row.setHierarchyCaption(hierarchy.getCaption());
        }
        Short type = dimensionType(dimension.getDimensionType());
        if (type != null) {
            row.setDimensionType(type);
        }
        row.setHierarchyCardinality((long) hierarchyCardinality(cube, hierarchy));
        if (hierarchy.getDefaultMember() != null) {
            row.setDefaultMember(hierarchy.getDefaultMember().getUniqueName());
        }
        if (hierarchy.hasAll() && firstLevelMembers != null && !firstLevelMembers.isEmpty()) {
            row.setAllMember(firstLevelMembers.get(0).getUniqueName());
        }
        row.setDescription(description);
        row.setStructure((short) 0); // MD_STRUCTURE_FULLYBALANCED
        row.setIsVirtual(Boolean.FALSE);
        row.setIsReadWrite(Boolean.FALSE);
        // NOTE that SQL Server returns 0, not 1.
        row.setDimensionUniqueSettings(0);
        row.setDimensionIsVisible(dimension.isVisible());
        row.setHierarchyOrdinal((long) ordinal);
        row.setDimensionIsShared(Boolean.TRUE);
        row.setHierarchyIsVisible(hierarchy.isVisible());
        row.setHierarchyOrigin(origin);
        row.setHierarchyDisplayFolder(hierarchy.getDisplayFolder() == null ? "" : hierarchy.getDisplayFolder());
        return row;
    }

    private static int hierarchyCardinality(Cube cube, Hierarchy hierarchy) {
        int cardinality = 0;
        if (hierarchy.getLevels() != null) {
            for (Level level : hierarchy.getLevels()) {
                cardinality += cube.getLevelCardinality(level, true, true);
            }
        }
        return cardinality;
    }

    // --- MDSCHEMA_LEVELS, ported from Utils.getMdSchemaLevelsResponseRow ---

    public List<EObject> levels(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");
        Optional<String> dimensionUniqueName = restrictions.value("DIMENSION_UNIQUE_NAME");
        Optional<String> hierarchyUniqueName = restrictions.value("HIERARCHY_UNIQUE_NAME");
        Optional<String> levelName = restrictions.value("LEVEL_NAME");
        Optional<String> levelUniqueName = restrictions.value("LEVEL_UNIQUE_NAME");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            Connection connection = contexts.getConnection(caller, catalog.getName());
            CatalogReader reader = connection.getCatalogReader();
            List<Cube> cubes = new ArrayList<>(filterByName(reader.getCubes(), cubeName));
            cubes.sort((a, b) -> a.getName().compareTo(b.getName()));
            for (Cube cube : cubes) {
                for (Dimension dimension : reader.getCubeDimensions(cube)) {
                    if (dimensionUniqueName.isPresent()
                            && !dimensionUniqueName.get().equals(dimension.getUniqueName())) {
                        continue;
                    }
                    for (Hierarchy hierarchy : reader.getDimensionHierarchies(dimension)) {
                        if (hierarchyUniqueName.isPresent()
                                && !hierarchyUniqueName.get().equals(hierarchy.getUniqueName())) {
                            continue;
                        }
                        for (Level level : reader.getHierarchyLevels(hierarchy)) {
                            if (levelName.isPresent() && !levelName.get().equals(level.getName())) {
                                continue;
                            }
                            if (levelUniqueName.isPresent() && !levelUniqueName.get().equals(level.getUniqueName())) {
                                continue;
                            }
                            result.add(levelRow(cube, catalog.getName(), dimension.getUniqueName(), hierarchy, level));
                        }
                    }
                }
            }
        }
        return result;
    }

    private static EObject levelRow(Cube cube, String catalogName, String dimensionUniqueName, Hierarchy hierarchy,
            Level level) {
        String description = level.getDescription();
        if (description == null) {
            description = cube.getName() + " Cube - " + DiscoverScope.hierarchyName(hierarchy.getName(), dimensionUniqueName)
                    + " Hierarchy - " + level.getName() + " Level";
        }

        // LEVEL_UNIQUE_SETTINGS bitmask: 1 key uniqueness, 2 name uniqueness.
        int uniqueSettings = 0;
        if (level.isAll()) {
            uniqueSettings |= 2;
        }
        if (level.isUnique()) {
            uniqueSettings |= 1;
        }

        MdschemaLevelsRow row = FACTORY.createMdschemaLevelsRow();
        row.setCatalogName(catalogName);
        row.setSchemaName(catalogName);
        row.setCubeName(cube.getName());
        row.setDimensionUniqueName(dimensionUniqueName);
        row.setHierarchyUniqueName(hierarchy.getUniqueName());
        row.setLevelName(level.getName());
        row.setLevelUniqueName(level.getUniqueName());
        if (level.getCaption() != null) {
            row.setLevelCaption(level.getCaption());
        }
        row.setLevelNumber((long) level.getDepth());
        row.setLevelCardinality((long) cube.getLevelCardinality(level, true, true));
        row.setLevelType(levelType(level));
        row.setDescription(description);
        row.setCustomRollupSettings(0);
        row.setLevelUniqueSettings(uniqueSettings);
        row.setLevelIsVisible(level.isVisible());
        row.setLevelOrigin(0);
        return row;
    }

    /** The LEVEL_TYPE constants, as the api's LevelTypeEnum numbered them. */
    private static int levelType(Level level) {
        if (level.isAll()) {
            return 0x0001;
        }
        switch (level.getLevelType()) {
        case REGULAR:
            return 0x0000;
        case TIME_YEARS:
            return 0x0014;
        case TIME_HALF_YEARS:
            return 0x0024;
        case TIME_QUARTERS:
            return 0x0044;
        case TIME_MONTHS:
            return 0x0004; // the api mapped months to plain TIME
        case TIME_WEEKS:
            return 0x0104;
        case TIME_DAYS:
            return 0x0204;
        case TIME_HOURS:
            return 0x0004;
        case TIME_MINUTES:
            return 0x0004;
        case TIME_SECONDS:
            return 0x0804;
        case TIME_UNDEFINED:
            return 0x1004;
        case GEO_CONTINENT:
            return 0x2001;
        case GEO_REGION:
            return 0x2002;
        case GEO_COUNTRY:
            return 0x2003;
        case GEO_STATE_OR_PROVINCE:
            return 0x2004;
        case GEO_COUNTY:
            return 0x2005;
        case GEO_CITY:
            return 0x2006;
        case GEO_POSTALCODE:
            return 0x2007;
        case GEO_POINT:
            return 0x2008;
        case ORG_UNIT:
            return 0x1011;
        case BOM_RESOURCE:
            return 0x1012;
        case QUANTITATIVE:
            return 0x1013;
        case ACCOUNT:
            return 0x1014;
        case CUSTOMER:
            return 0x1021;
        case CUSTOMER_GROUP:
            return 0x1022;
        case CUSTOMER_HOUSEHOLD:
            return 0x1023;
        case PRODUCT:
            return 0x1031;
        case PRODUCT_GROUP:
            return 0x1032;
        case SCENARIO:
            return 0x1015;
        default:
            return 0x0000;
        }
    }

    // --- MDSCHEMA_MEASURES, ported from Utils.getMdSchemaMeasuresResponseRow ---

    public List<EObject> measures(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");
        Optional<String> measureName = restrictions.value("MEASURE_NAME");
        Optional<String> measureUniqueName = restrictions.value("MEASURE_UNIQUE_NAME");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            Connection connection = contexts.getConnection(caller, catalog.getName());
            CatalogReader reader = connection.getCatalogReader();
            List<Cube> cubes = catalog.getCubes() == null ? List.of() : catalog.getCubes();
            for (Cube cube : filterByName(cubes, cubeName)) {
                measuresOfCube(reader, catalog.getName(), cube, measureName, measureUniqueName, result);
            }
        }
        return result;
    }

    private static void measuresOfCube(CatalogReader reader, String catalogName, Cube cube,
            Optional<String> measureName, Optional<String> measureUniqueName, List<EObject> result) {
        // LEVELS_LIST names the deepest level of every non-measures hierarchy - the
        // grain a
        // stored measure aggregates over. A calculated member has no grain and gets
        // none.
        StringBuilder levelList = new StringBuilder();
        for (Dimension dimension : cube.getDimensions()) {
            if (DimensionType.MEASURES_DIMENSION.equals(dimension.getDimensionType())) {
                continue;
            }
            for (Hierarchy hierarchy : dimension.getHierarchies()) {
                List<? extends Level> levels = hierarchy.getLevels();
                if (levels != null && !levels.isEmpty()) {
                    if (levelList.length() > 0) {
                        levelList.append(',');
                    }
                    levelList.append(levels.getLast().getUniqueName());
                }
            }
        }

        Level measuresLevel = cube.getDimensions().getFirst().getHierarchies().getFirst().getLevels().getFirst();
        List<Member> members = reader.getLevelMembers(measuresLevel, true);
        for (Member member : members) {
            if (measureName.isPresent() && !measureName.get().equals(member.getName())) {
                continue;
            }
            if (measureUniqueName.isPresent() && !measureUniqueName.get().equals(member.getUniqueName())) {
                continue;
            }
            EObject row = measureRow(catalogName, cube.getName(), member.isCalculated() ? null : levelList.toString(),
                    member);
            if (row != null) {
                result.add(row);
            }
        }
    }

    private static EObject measureRow(String catalogName, String cubeName, String levelList, Member member) {
        Boolean visible = (Boolean) member.getPropertyValue(Property.StandardMemberProperty.$visible.getName());
        if (visible == null) {
            visible = Boolean.TRUE;
        }
        if (!visible) {
            return null;
        }
        String description = member.getDescription();
        if (description == null) {
            description = cubeName + " Cube - " + member.getName() + " Member";
        }
        String formatString = (String) member.getPropertyValue(Property.StandardCellProperty.FORMAT_STRING.getName());

        // DATA_TYPE: a DBType guess from the member's declared datatype, string
        // otherwise.
        OleDbType dbType = OleDbType.WSTR;
        String datatype = (String) member.getPropertyValue(Property.StandardCellProperty.DATATYPE.getName());
        if ("Integer".equals(datatype)) {
            dbType = OleDbType.I4;
        } else if ("Numeric".equals(datatype)) {
            dbType = OleDbType.R8;
        }
        String displayFolder = "";
        if (member.getPropertyValue(StandardProperty.DISPLAY_FOLDER.getName()) != null) {
            displayFolder = (String) member.getPropertyValue(StandardProperty.DISPLAY_FOLDER.getName());
        }

        MdschemaMeasuresRow row = FACTORY.createMdschemaMeasuresRow();
        row.setCatalogName(catalogName);
        row.setSchemaName(catalogName);
        row.setCubeName(cubeName);
        row.setMeasureName(member.getName());
        row.setMeasureUniqueName(member.getUniqueName());
        if (member.getCaption() != null) {
            row.setMeasureCaption(member.getCaption());
        }
        row.setMeasureAggregator(aggregator(member));
        row.setDataType(dbType.dbTypeOrdinal());
        row.setDescription(description);
        row.setMeasureIsVisible(visible);
        if (levelList != null) {
            row.setLevelsList(levelList);
        }
        row.setMeasureGroupName(cubeName);
        row.setMeasureDisplayFolder(displayFolder);
        if (formatString != null) {
            row.setDefaultFormatString(formatString);
        }
        return row;
    }

    /**
     * MDMEASURE_AGGR_*: 1 sum, 2 count, 3 min, 4 max, 5 avg, 8 distinct count, 0
     * unknown.
     */
    private static int aggregator(Member member) {
        if (member instanceof StoredMeasure storedMeasure) {
            String function = storedMeasure.getAggregateFunction();
            if (function != null) {
                if (function.equalsIgnoreCase("Sum")) {
                    return 1;
                }
                if (function.equalsIgnoreCase("Count")) {
                    return 2;
                }
                if (function.equalsIgnoreCase("Min")) {
                    return 3;
                }
                if (function.equalsIgnoreCase("Max")) {
                    return 4;
                }
                if (function.equalsIgnoreCase("Avg")) {
                    return 5;
                }
                if (function.equalsIgnoreCase("DistinctCount")) {
                    return 8;
                }
            }
        }
        return 0;
    }

    // --- MDSCHEMA_MEASUREGROUPS: one group per cube, named like it ---

    public List<EObject> measureGroups(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            for (Cube cube : filterByName(catalog.getCubes(), cubeName)) {
                MdschemaMeasuregroupsRow row = FACTORY.createMdschemaMeasuregroupsRow();
                row.setCatalogName(catalog.getName());
                row.setCubeName(cube.getName());
                row.setMeasureGroupName(cube.getName());
                row.setDescription("");
                // One group per cube here, so the cube's own answer is the group's.
                row.setIsWriteEnabled(cube.isWriteEnabled());
                row.setMeasureGroupCaption(cube.getName());
                result.add(row);
            }
        }
        return result;
    }

    // --- MDSCHEMA_MEASUREGROUP_DIMENSIONS ---

    public List<EObject> measureGroupDimensions(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");
        Optional<String> dimensionUniqueName = restrictions.value("DIMENSION_UNIQUE_NAME");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            List<Cube> cubes = catalog.getCubes() == null ? List.of() : catalog.getCubes();
            for (Cube cube : filterByName(cubes, cubeName)) {
                List<? extends Dimension> dimensions = cube.getDimensions() == null ? List.of() : cube.getDimensions();
                for (Dimension dimension : dimensions) {
                    if (dimensionUniqueName.isPresent()
                            && !dimensionUniqueName.get().equals(dimension.getUniqueName())) {
                        continue;
                    }
                    MdschemaMeasuregroupDimensionsRow row = FACTORY.createMdschemaMeasuregroupDimensionsRow();
                    row.setCatalogName(catalog.getName());
                    row.setCubeName(cube.getName());
                    row.setMeasureGroupName(cube.getName());
                    row.setMeasureGroupCardinality("ONE");
                    row.setDimensionUniqueName(dimension.getUniqueName());
                    row.setDimensionCardinality("MANY");
                    row.setDimensionIsVisible(dimension.isVisible());
                    row.setDimensionIsFactDimension(Boolean.FALSE);
                    row.setDimensionGranularity("");
                    result.add(row);
                }
            }
        }
        return result;
    }

    // --- MDSCHEMA_MEMBERS, ported from Utils.getMdSchemaMembersResponseRow ---

    public List<EObject> members(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");
        Optional<String> dimensionUniqueName = restrictions.value("DIMENSION_UNIQUE_NAME");
        Optional<String> hierarchyUniqueName = restrictions.value("HIERARCHY_UNIQUE_NAME");
        Optional<String> levelUniqueName = restrictions.value("LEVEL_UNIQUE_NAME");
        Optional<String> levelNumber = restrictions.value("LEVEL_NUMBER");
        Optional<String> memberUniqueName = restrictions.value("MEMBER_UNIQUE_NAME");

        // The bridge answered nothing without a member restriction, wary of dumping
        // whole
        // hierarchies; the live surveys showed real servers answer them - kept as it
        // was,
        // one finding at a time.
        if (memberUniqueName.isEmpty()) {
            LOGGER.warn("MEMBER_UNIQUE_NAME is missing; answering no rows");
            return List.of();
        }

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            for (Cube cube : filterByName(catalog.getCubes(), cubeName)) {
                membersOfCube(catalog.getName(), cube, dimensionUniqueName, hierarchyUniqueName, levelUniqueName,
                        levelNumber, memberUniqueName, result);
            }
        }
        return result;
    }

    private static void membersOfCube(String catalogName, Cube cube, Optional<String> dimensionUniqueName,
            Optional<String> hierarchyUniqueName, Optional<String> levelUniqueName, Optional<String> levelNumber,
            Optional<String> memberUniqueName, List<EObject> result) {
        if (levelUniqueName.isPresent()) {
            Level level = lookupLevel(cube, levelUniqueName.get());
            if (level != null) {
                membersOfLevelList(catalogName, cube, cube.getLevelMembers(level, true), memberUniqueName, result);
            }
            return;
        }
        List<? extends Dimension> dimensions = cube.getDimensions() == null ? List.of() : cube.getDimensions();
        for (Dimension dimension : dimensions) {
            if (dimensionUniqueName.isPresent() && !dimensionUniqueName.get().equals(dimension.getUniqueName())) {
                continue;
            }
            List<? extends Hierarchy> hierarchies = dimension.getHierarchies() == null ? List.of()
                    : dimension.getHierarchies();
            for (Hierarchy hierarchy : hierarchies) {
                if (hierarchyUniqueName.isPresent() && !hierarchyUniqueName.get().equals(hierarchy.getUniqueName())) {
                    continue;
                }
                membersOfHierarchy(catalogName, cube, hierarchy, levelNumber, memberUniqueName, result);
            }
        }
    }

    private static void membersOfHierarchy(String catalogName, Cube cube, Hierarchy hierarchy,
            Optional<String> levelNumber, Optional<String> memberUniqueName, List<EObject> result) {
        if (levelNumber.isPresent()) {
            int number = Integer.parseInt(levelNumber.get());
            List<? extends Level> levels = hierarchy.getLevels();
            if (number < 0 || number >= levels.size()) {
                LOGGER.warn("LEVEL_NUMBER {} out of range for {}", number, hierarchy.getUniqueName());
                return;
            }
            membersOfLevelList(catalogName, cube, cube.getLevelMembers(levels.get(number), true), memberUniqueName,
                    result);
            return;
        }
        List<? extends Level> levels = hierarchy.getLevels() == null ? List.of() : hierarchy.getLevels();
        List<Member> members = new ArrayList<>();
        for (Level level : levels) {
            for (Member member : cube.getLevelMembers(level, true)) {
                if (!members.contains(member)) {
                    members.add(member);
                }
            }
        }
        membersOfLevelList(catalogName, cube, members, memberUniqueName, result);
    }

    private static void membersOfLevelList(String catalogName, Cube cube, List<Member> members,
            Optional<String> memberUniqueName, List<EObject> result) {
        for (Member member : members) {
            if (memberUniqueName.isPresent() && !memberUniqueName.get().equals(member.getUniqueName())) {
                continue;
            }
            Boolean visible = Boolean.TRUE;
            if (member.getPropertyValue("$visible") != null) {
                visible = (Boolean) member.getPropertyValue("$visible");
            }
            if (!visible) {
                continue;
            }
            result.add(memberRow(catalogName, cube, member));
        }
    }

    private static EObject memberRow(String catalogName, Cube cube, Member member) {
        Level level = member.getLevel();
        Hierarchy hierarchy = level.getHierarchy();
        Dimension dimension = hierarchy.getDimension();
        int depth = level.getDepth();

        String parentUniqueName = null;
        if (depth != 0 && member.getParentMember() != null) {
            parentUniqueName = member.getParentMember().getUniqueName();
        }

        MdschemaMembersRow row = FACTORY.createMdschemaMembersRow();
        row.setCatalogName(catalogName);
        row.setCubeName(cube.getName());
        row.setDimensionUniqueName(dimension.getUniqueName());
        row.setHierarchyUniqueName(hierarchy.getUniqueName());
        row.setLevelUniqueName(level.getUniqueName());
        row.setLevelNumber((long) depth);
        row.setMemberOrdinal(0L);
        row.setMemberName(member.getName());
        row.setMemberUniqueName(member.getUniqueName());
        row.setMemberType(memberType(member.getMemberType()));
        if (member.getCaption() != null) {
            row.setMemberCaption(member.getCaption());
        }
        row.setChildrenCardinality(100L);
        row.setParentLevel((long) (depth == 0 ? 0 : depth - 1));
        if (parentUniqueName != null) {
            row.setParentUniqueName(parentUniqueName);
        }
        row.setParentCount((long) (member.getParentMember() == null ? 0 : 1));
        if (member.getDescription() != null) {
            row.setDescription(member.getDescription());
        }
        return row;
    }

    /** MDMEMBER_TYPE_*: 1 regular, 2 all, 3 measure, 4 formula, 0 unknown. */
    private static int memberType(Member.MemberType memberType) {
        if (memberType == null) {
            return 1;
        }
        switch (memberType) {
        case REGULAR:
            return 1;
        case ALL:
            return 2;
        case MEASURE:
            return 3;
        case FORMULA:
            return 4;
        case UNKNOWN:
            return 0;
        default:
            return 1;
        }
    }

    private static Level lookupLevel(Cube cube, String levelUniqueName) {
        for (Dimension dimension : cube.getDimensions()) {
            for (Hierarchy hierarchy : dimension.getHierarchies()) {
                for (Level level : hierarchy.getLevels()) {
                    if (level.getUniqueName().equals(levelUniqueName)) {
                        return level;
                    }
                }
            }
        }
        return null;
    }

    // --- MDSCHEMA_PROPERTIES, ported from Utils.getMdSchemaPropertiesResponseRow
    // ---

    public List<EObject> properties(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");
        Optional<String> dimensionUniqueName = restrictions.value("DIMENSION_UNIQUE_NAME");
        Optional<String> hierarchyUniqueName = restrictions.value("HIERARCHY_UNIQUE_NAME");
        Optional<String> levelUniqueName = restrictions.value("LEVEL_UNIQUE_NAME");
        Optional<String> propertyName = restrictions.value("PROPERTY_NAME");
        Optional<String> propertyType = restrictions.value("PROPERTY_TYPE");

        // PROPERTY_TYPE: 1 member property (the default), 2 cell property.
        int type = 1;
        if (propertyType.isPresent()) {
            type = Integer.parseInt(propertyType.get());
        }
        if (type == 2) {
            return cellProperties();
        }
        if (type != 1) {
            return List.of();
        }

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            List<Cube> cubes = catalog.getCubes() == null ? List.of() : catalog.getCubes();
            for (Cube cube : filterByName(cubes, cubeName)) {
                if (levelUniqueName.isPresent()) {
                    // A level unique name names its dimension and hierarchy implicitly.
                    Level level = lookupLevel(cube, levelUniqueName.get());
                    if (level != null) {
                        propertiesOfLevel(catalog.getName(), cube, level, propertyName, result);
                    }
                    continue;
                }
                List<? extends Dimension> dimensions = cube.getDimensions() == null ? List.of() : cube.getDimensions();
                for (Dimension dimension : dimensions) {
                    if (dimensionUniqueName.isPresent()
                            && !dimensionUniqueName.get().equals(dimension.getUniqueName())) {
                        continue;
                    }
                    List<? extends Hierarchy> hierarchies = dimension.getHierarchies() == null ? List.of()
                            : dimension.getHierarchies();
                    for (Hierarchy hierarchy : hierarchies) {
                        if (hierarchyUniqueName.isPresent()
                                && !hierarchyUniqueName.get().equals(hierarchy.getUniqueName())) {
                            continue;
                        }
                        List<? extends Level> levels = hierarchy.getLevels() == null ? List.of()
                                : hierarchy.getLevels();
                        for (Level level : levels) {
                            propertiesOfLevel(catalog.getName(), cube, level, propertyName, result);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static void propertiesOfLevel(String catalogName, Cube cube, Level level, Optional<String> propertyName,
            List<EObject> result) {
        org.eclipse.daanse.olap.api.element.Property[] properties = level.getProperties();
        if (properties == null) {
            return;
        }
        for (org.eclipse.daanse.olap.api.element.Property property : properties) {
            if (property == null) {
                continue;
            }
            if (propertyName.isPresent() && !propertyName.get().equals(property.getName())) {
                continue;
            }
            result.add(memberPropertyRow(catalogName, cube, level, property));
        }
    }

    private static EObject memberPropertyRow(String catalogName, Cube cube, Level level,
            org.eclipse.daanse.olap.api.element.Property property) {
        Hierarchy hierarchy = level.getHierarchy();
        Dimension dimension = hierarchy.getDimension();
        String description = cube.getName() + " Cube - " + DiscoverScope.hierarchyName(hierarchy.getName(), dimension.getName())
                + " Hierarchy - " + level.getName() + " Level - " + property.getName() + " Property";

        MdschemaPropertiesRow row = FACTORY.createMdschemaPropertiesRow();
        row.setCatalogName(catalogName);
        row.setCubeName(cube.getName());
        row.setDimensionUniqueName(dimension.getUniqueName());
        row.setHierarchyUniqueName(hierarchy.getUniqueName());
        row.setLevelUniqueName(level.getUniqueName());
        row.setPropertyType((short) 1); // MDPROP_MEMBER
        row.setPropertyName(property.getName());
        if (property.getCaption() != null) {
            row.setPropertyCaption(property.getCaption());
        }
        row.setDataType(dbTypeOf(property));
        row.setDescription(description);
        row.setPropertyContentType((short) 0); // MD_PROPTYPE_REGULAR
        row.setPropertyIsVisible(Boolean.TRUE);
        return row;
    }

    /**
     * The cell properties every cube answers with, from the engine's standard set.
     */
    private static List<EObject> cellProperties() {
        List<EObject> result = new ArrayList<>();
        for (Property.StandardCellProperty property : Property.StandardCellProperty.values()) {
            int mask = 0;
            for (Property.TypeFlag flag : property.getType()) {
                mask |= flag.xmlaOrdinal();
            }
            MdschemaPropertiesRow row = FACTORY.createMdschemaPropertiesRow();
            row.setPropertyType((short) mask);
            row.setPropertyName(property.name());
            if (property.getCaption() != null) {
                row.setPropertyCaption(property.getCaption());
            }
            row.setDataType(property.getDatatype().xmlaOrdinal());
            row.setPropertyIsVisible(Boolean.TRUE);
            result.add(row);
        }
        return result;
    }

    /** DBTYPE constants: 130 string, 5 double, 11 boolean. */
    private static int dbTypeOf(org.eclipse.daanse.olap.api.element.Property property) {
        if (property.getType() == null) {
            return 130;
        }
        switch (property.getType()) {
        case TYPE_STRING:
            return 130;
        case TYPE_INTEGER:
        case TYPE_LONG:
        case TYPE_NUMERIC:
            return 5;
        case TYPE_BOOLEAN:
            return 11;
        default:
            return 130;
        }
    }

    // --- MDSCHEMA_KPIS, ported from Utils.getMdSchemaKpisResponseRow ---

    public List<EObject> kpis(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");
        Optional<String> kpiName = restrictions.value("KPI_NAME");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            for (Cube cube : filterByName(catalog.getCubes(), cubeName)) {
                for (KPI kpi : cube.getKPIs()) {
                    if (kpiName.isPresent() && !kpiName.get().equals(kpi.getName())) {
                        continue;
                    }
                    result.add(kpiRow(catalog.getName(), cube.getName(), kpi));
                }
            }
        }
        return result;
    }

    private static EObject kpiRow(String catalogName, String cubeName, KPI kpi) {
        MdschemaKpisRow row = FACTORY.createMdschemaKpisRow();
        row.setCatalogName(catalogName);
        row.setCubeName(cubeName);
        row.setMeasureGroupName(cubeName);
        row.setKpiName(kpi.getName());
        row.setKpiCaption(kpi.getName());
        if (kpi.getDescription() != null) {
            row.setKpiDescription(kpi.getDescription());
        }
        if (kpi.getDisplayFolder() != null) {
            row.setKpiDisplayFolder(kpi.getDisplayFolder());
        }
        if (kpi.getValue() != null) {
            row.setKpiValue(kpi.getValue());
        }
        if (kpi.getGoal() != null) {
            row.setKpiGoal(kpi.getGoal());
        }
        if (kpi.getStatus() != null) {
            row.setKpiStatus(kpi.getStatus());
        }
        if (kpi.getTrend() != null) {
            row.setKpiTrend(kpi.getTrend());
        }
        if (kpi.getStatusGraphic() != null) {
            row.setKpiStatusGraphic(kpi.getStatusGraphic());
        }
        if (kpi.getTrendGraphic() != null) {
            row.setKpiTrendGraphic(kpi.getTrendGraphic());
        }
        if (kpi.getWeight() != null) {
            row.setKpiWeight(kpi.getWeight());
        }
        if (kpi.getCurrentTimeMember() != null) {
            row.setKpiCurrentTimeMember(kpi.getCurrentTimeMember());
        }
        if (kpi.getParentKpi() != null) {
            row.setKpiParentKpiName(kpi.getParentKpi().getName());
        }
        return row;
    }

    // --- MDSCHEMA_SETS, ported from Utils.getMdSchemaSetsResponseRow ---

    public List<EObject> sets(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> cubeName = restrictions.value("CUBE_NAME");
        Optional<String> setName = restrictions.value("SET_NAME");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            List<Cube> cubes = catalog.getCubes() == null ? List.of() : catalog.getCubes();
            for (Cube cube : filterByName(cubes, cubeName)) {
                NamedSet[] sets = cube.getNamedSets();
                if (sets == null) {
                    continue;
                }
                for (NamedSet set : sets) {
                    if (setName.isPresent() && !setName.get().equals(set.getName())) {
                        continue;
                    }
                    result.add(setRow(catalog.getName(), cube.getName(), set));
                }
            }
        }
        return result;
    }

    private static EObject setRow(String catalogName, String cubeName, NamedSet set) {
        StringBuilder dimensions = new StringBuilder();
        for (Hierarchy hierarchy : set.getHierarchies()) {
            if (dimensions.length() > 0) {
                dimensions.append(',');
            }
            dimensions.append(hierarchy.getUniqueName());
        }

        MdschemaSetsRow row = FACTORY.createMdschemaSetsRow();
        row.setCatalogName(catalogName);
        row.setCubeName(cubeName);
        row.setSetName(set.getName());
        row.setScope(1); // MDSET_SCOPE_GLOBAL
        if (set.getDescription() != null) {
            row.setDescription(set.getDescription());
        }
        if (set.getExp() != null) {
            row.setExpression(set.getExp().toString());
        }
        row.setDimensions(dimensions.toString());
        if (set.getCaption() != null) {
            row.setSetCaption(set.getCaption());
        }
        if (set.getDisplayFolder() != null) {
            row.setSetDisplayFolder(set.getDisplayFolder());
        }
        row.setSetEvaluationContext(1); // MDSET_RESOLUTION_STATIC
        return row;
    }

    // --- MDSCHEMA_FUNCTIONS: the function catalogue is context independent ---

    public List<EObject> functions(RestrictionValues restrictions) {
        List<org.eclipse.daanse.olap.api.Context<?>> all = contexts.getContexts();
        if (all.isEmpty()) {
            return List.of();
        }
        String lcid = restrictions.properties() == null ? null
                : restrictions.properties().getLocaleIdentifier() == null ? null
                        : String.valueOf(restrictions.properties().getLocaleIdentifier());
        return FunctionRows.rows(all.get(0).getFunctionService(), functionTexts, FunctionRows.localeOf(lcid),
                restrictions.value("FUNCTION_NAME"), restrictions.value("ORIGIN"), restrictions.value("INTERFACE_NAME"),
                restrictions.value("LIBRARY_NAME"));
    }

    // --- shared helpers, the connector's counterpart of the bridge's Utils filters
    // ---

    /**
     * The catalogs in scope: the named one when the server has it, all of them
     * otherwise.
     */
    private static List<Cube> filterByName(List<Cube> cubes, Optional<String> cubeName) {
        if (cubeName.isEmpty()) {
            return cubes;
        }
        List<Cube> filtered = new ArrayList<>();
        for (Cube cube : cubes) {
            if (cubeName.get().equals(cube.getName())) {
                filtered.add(cube);
            }
        }
        return filtered;
    }

    /**
     * UTC, not the server's own zone. A client converts these timestamps to local
     * time itself once it has seen a provider version of 9 or above — ADOMD does it
     * for CREATED_ON, LAST_SCHEMA_UPDATE, LAST_DATA_UPDATE and DATE_MODIFIED — so a
     * local time sent from here is shifted a second time and lands hours away.
     */
    private static LocalDateTime startOfToday() {
        return LocalDateTime.now(java.time.ZoneOffset.UTC).toLocalDate().atStartOfDay();
    }

    // --- MDSCHEMA_INPUT_DATASOURCES ---

    /** The two values [MS-SSAS] allows for DATASOURCE_TYPE; this server is the first. */
    private static final String RELATIONAL = "Relational";

    /**
     * The data source each catalog reads from, one row per catalog.
     * <p>
     * "The data source objects defined within the database", as [MS-SSAS] puts it.
     * Analysis Services keeps a data source view holding several named sources; a
     * context here has exactly one, so the row describes the context's own store and
     * carries its name. {@code DATASOURCE_TYPE} is {@code Relational}, the other of
     * the two values the specification allows being {@code Olap}.
     * <p>
     * Four columns stay absent rather than plausible. {@code SCHEMA_NAME} is
     * product-specific and a real server leaves it empty. {@code CREATED_ON} and
     * {@code LAST_SCHEMA_UPDATE} would have to be invented, and a timestamp of "now"
     * is worse than none - a client caching on it re-reads the metadata after every
     * query. {@code TIMEOUT} and {@code DBMS_VERSION} would need a live JDBC
     * connection, which is more than a metadata request should open; the dialect
     * names the store without one.
     */
    public List<EObject> inputDatasources(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> datasourceName = restrictions.value("DATASOURCE_NAME");
        Optional<String> datasourceType = restrictions.value("DATASOURCE_TYPE");

        // A restriction on a column this server never fills matches nothing. Returning
        // every row instead would do the one thing a restriction must never do: widen.
        if (restrictions.value("SCHEMA_NAME").isPresent()) {
            return List.of();
        }
        if (datasourceType.isPresent() && !RELATIONAL.equalsIgnoreCase(datasourceType.get())) {
            return List.of();
        }

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            if (datasourceName.isPresent() && !datasourceName.get().equals(catalog.getName())) {
                continue;
            }
            result.add(inputDatasourceRow(catalog));
        }
        return result;
    }

    private EObject inputDatasourceRow(Catalog catalog) {
        MdschemaInputDatasourcesRow row = FACTORY.createMdschemaInputDatasourcesRow();
        row.setCatalogName(catalog.getName());
        row.setDatasourceName(catalog.getName());
        row.setDatasourceType(RELATIONAL);
        contexts.getContext(catalog.getName()).ifPresent(context -> {
            context.getDescription().ifPresent(row::setDescription);
            if (context instanceof RelationalContext<?> relational && relational.getDialect() != null) {
                row.setDbmsName(relational.getDialect().name());
            }
        });
        return row;
    }
}
