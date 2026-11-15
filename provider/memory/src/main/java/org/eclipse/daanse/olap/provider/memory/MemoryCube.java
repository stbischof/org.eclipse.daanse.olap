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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.DimensionType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.element.CubeBase;

/**
 * A cube the provider holds in memory.
 *
 * <p>Physical, not virtual: it stands on its own cells rather than on measures borrowed
 * from other cubes. Functions branch on this. {@code ValidMeasure} in particular passes
 * its argument straight through for a physical cube and goes looking for a virtual-cube
 * measure otherwise, which a cube like this one does not have.
 */
public class MemoryCube extends CubeBase implements org.eclipse.daanse.olap.api.element.PhysicalCube {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryCube.class);

    private final List<MemoryDimension> miniDimensions = new ArrayList<>();
    private MemoryDimension measuresDimension;
    private int hierarchyOrdinals;

    /** Hands out the next position in the evaluator's one-member-per-hierarchy array. */
    int nextHierarchyOrdinal() {
        return hierarchyOrdinals++;
    }

    public MemoryCube(String name) {
        super(name, name, true, null, new ArrayList<>());
    }

    /** Adds a regular dimension. */
    public MemoryDimension dimension(String dimensionName) {
        return dimension(dimensionName, DimensionType.STANDARD_DIMENSION);
    }

    /** Adds a dimension of an explicit type. */
    public MemoryDimension dimension(String dimensionName, DimensionType dimensionType) {
        MemoryDimension d = new MemoryDimension(this, dimensionName, dimensionType);
        if (dimensionType == DimensionType.MEASURES_DIMENSION) {
            // the engine finds the current measure at position 0 of its context
            miniDimensions.add(0, d);
            dimensions.add(0, d);
            measuresDimension = d;
        } else {
            miniDimensions.add(d);
            dimensions.add(d);
        }
        return d;
    }

    /** The measures dimension, created on first use. */
    public MemoryDimension measures() {
        if (measuresDimension == null) {
            measuresDimension = dimension("Measures", DimensionType.MEASURES_DIMENSION);
        }
        return measuresDimension;
    }

    @Override
    public List<Dimension> getDimensions() {
        return dimensions;
    }

    @Override
    public List<Member> getMeasures() {
        if (measuresDimension == null) {
            return List.of();
        }
        return measuresDimension.getHierarchies().get(0).getLevels().get(0).getMembers();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }


    @Override
    public boolean isLoadInProgress() {
        return false;
    }


    @Override
    public org.eclipse.daanse.olap.api.element.Hierarchy getTimeHierarchy(String funName) {
        for (org.eclipse.daanse.olap.api.element.Dimension d : dimensions) {
            if (d.getDimensionType() == org.eclipse.daanse.olap.api.element.DimensionType.TIME_DIMENSION) {
                return d.getHierarchies().get(0);
            }
        }
        throw new org.eclipse.daanse.olap.api.exception.OlapRuntimeException(
                "'" + funName + "' requires a time dimension, and cube " + getName() + " has none");
    }

    @Override
    public java.util.List<java.util.Map<String, java.util.Map.Entry<
            org.eclipse.daanse.olap.api.DataTypeJdbc, Object>>> getAllocationValues(
            String measureName, Object newValue,
            org.eclipse.daanse.olap.api.result.AllocationPolicy policy,
            org.eclipse.daanse.olap.api.access.Role role) {
        throw new UnsupportedOperationException(
                "MemoryCube.getAllocationValues is not implemented by the provider: writeback needs a backend");
    }

    @Override
    public void commit(java.util.List<java.util.Map<String, java.util.Map.Entry<
            org.eclipse.daanse.olap.api.DataTypeJdbc, Object>>> rows, String session) {
        throw new UnsupportedOperationException(
                "MemoryCube.commit is not implemented by the provider: writeback needs a backend");
    }

    @Override
    public void restoreFact() {
        // nothing to restore: the provider holds no fact table
    }

    @Override
    public void modifyFact(java.util.List<java.util.Map<String, java.util.Map.Entry<
            org.eclipse.daanse.olap.api.DataTypeJdbc, Object>>> rows) {
        throw new UnsupportedOperationException(
                "MemoryCube.modifyFact is not implemented by the provider: writeback needs a backend");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Hierarchy> getHierarchies() {
        java.util.List<org.eclipse.daanse.olap.api.element.Hierarchy> all = new ArrayList<>();
        for (Dimension d : dimensions) {
            all.addAll(d.getHierarchies());
        }
        return all;
    }


    private org.eclipse.daanse.olap.api.element.Catalog catalog;

    /** Attaches this cube to its catalog; called by {@link MemoryCatalog}. */
    void catalog(org.eclipse.daanse.olap.api.element.Catalog value) {
        this.catalog = value;
        // Positions follow getHierarchies(), measures first, whatever order the cube was declared in.
        int ordinal = 0;
        for (org.eclipse.daanse.olap.api.element.Hierarchy hierarchy : getHierarchies()) {
            ((MemoryHierarchy) hierarchy).ordinalInCube(ordinal++);
        }
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Catalog getCatalog() {
        return catalog;
    }

    @Override
    public org.eclipse.daanse.olap.api.element.MetaData getMetaData() {
        return org.eclipse.daanse.olap.element.OlapMetaDataBase.empty();
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Member createCalculatedMember(org.eclipse.daanse.olap.api.query.component.Formula a0) {
        throw new UnsupportedOperationException(
                "MemoryCube.createCalculatedMember is not implemented by the provider");
    }

    @Override
    public void createNamedSet(org.eclipse.daanse.olap.api.query.component.Formula a0) {
        throw new UnsupportedOperationException(
                "MemoryCube.createNamedSet is not implemented by the provider");
    }

    @Override
    public org.eclipse.daanse.olap.api.catalog.CatalogReader getCatalogReader(
            org.eclipse.daanse.olap.api.access.Role role) {
        MemoryCatalog memoryCatalog = (MemoryCatalog) getCatalog();
        return new MemoryCatalogReader(memoryCatalog,
                role == null ? memoryCatalog.getDefaultRole() : role, memoryCatalog.context());
    }

    /** No drill-through: there is nothing underneath a cell but the reader that made it. */
    @Override
    public org.eclipse.daanse.olap.api.element.DrillThroughAction getDefaultDrillThroughAction() {
        return null;
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.DrillThroughAction> getDrillThroughActions() {
        return java.util.List.of();
    }

    /**
     * No KPIs. Empty, not an exception: a cube without key performance indicators is an
     * ordinary cube, and a function that asks is entitled to the answer "none".
     */
    @Override
    public java.util.List<? extends org.eclipse.daanse.olap.api.element.KPI> getKPIs() {
        return java.util.List.of();
    }

    /**
     * How many members a level has. Every member is present in memory, so this is a count
     * rather than an estimate, and the two hints are of no use here.
     */
    @Override
    public int getLevelCardinality(org.eclipse.daanse.olap.api.element.Level level,
            boolean approximate, boolean materialize) {
        return level.getMembers().size();
    }

    /** The members of a level. This provider holds no calculated members in a level. */
    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getLevelMembers(
            org.eclipse.daanse.olap.api.element.Level level, boolean includeCalculated) {
        return level.getMembers();
    }

    /** No named sets are declared in the catalogue; a query may still define its own. */
    @Override
    public org.eclipse.daanse.olap.api.element.NamedSet[] getNamedSets() {
        return new org.eclipse.daanse.olap.api.element.NamedSet[0];
    }

    /**
     * Which of these dimensions do not reach this cube. None of them: this provider has no
     * virtual cubes, so every dimension of the catalogue belongs to the one cube.
     */
    @Override
    public java.util.Set<org.eclipse.daanse.olap.api.element.Dimension> nonJoiningDimensions(
            java.util.Set<org.eclipse.daanse.olap.api.element.Dimension> dimensions) {
        return java.util.Set.of();
    }

    @Override
    public java.util.Set<org.eclipse.daanse.olap.api.element.Dimension> nonJoiningDimensions(
            org.eclipse.daanse.olap.api.element.Member[] members) {
        return java.util.Set.of();
    }

}
