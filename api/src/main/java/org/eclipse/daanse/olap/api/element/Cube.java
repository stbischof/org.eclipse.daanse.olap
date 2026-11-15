/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1999-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * Copyright (C) 2021 Sergei Semenkov
 * All Rights Reserved.
 *
 * Contributors:
 *  SmartCity Jena - refactor, clean API
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.api.element;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.result.AllocationPolicy;

/**
 * Cube.
 *
 * @author jhyde, 2 March, 1999
 */
public interface Cube extends OlapElement, MetaElement {

    Catalog getCatalog();

    /**
     * Returns the dimensions of this cube.
     */
    List<? extends Dimension> getDimensions();

    /**
     * Returns the named sets of this cube.
     */
    NamedSet[] getNamedSets();

    List<Member> getMeasures();

    /**
     * Finds a hierarchy whose name (or unique name, if unique is true) equals s.
     */
    Hierarchy lookupHierarchy(NameSegment s, boolean unique);

    /**
     * Returns a CatalogReader for which this cube is the context for lookup up
     * members. If role is null, the returned schema reader also obeys the
     * access-control profile of role.
     */
    CatalogReader getCatalogReader(Role role);

    /**
     * Finds out non joining dimensions for this cube.
     *
     * @param tuple array of members
     * @return Set of dimensions that do not exist (non joining) in this cube
     */
    Set<Dimension> nonJoiningDimensions(Member[] tuple);

    /**
     * Finds out non joining dimensions for this cube.
     *
     * @param otherDims Set of dimensions to be tested for existence in this cube
     * @return Set of dimensions that do not exist (non joining) in this cube
     */
    Set<Dimension> nonJoiningDimensions(Set<Dimension> otherDims);

    Member createCalculatedMember(Formula formula);

    void createNamedSet(Formula formula);

    DrillThroughAction getDefaultDrillThroughAction();

    List<DrillThroughAction> getDrillThroughActions();

    /**
     * Returns the members of a level, optionally including calculated members.
     */
    List<Member> getLevelMembers(Level level, boolean includeCalculated);

    /**
     * Returns the number of members in a level, returning an approximation if
     * acceptable.
     *
     * @param level       Level
     * @param approximate Whether an approximation is acceptable
     * @param materialize Whether to go to disk if no approximation for the count is
     *                    available and the members are not in cache. If false,
     *                    returns Integer#MIN_VALUE if value is not in cache.
     */
    int getLevelCardinality(Level level, boolean approximate, boolean materialize);

    List<? extends KPI> getKPIs();

    /**
     * Returns a list of all hierarchies in this cube, in order of dimension.
     *
     *
     * TODO: Make this method return RolapCubeHierarchy, when the measures hierarchy
     * is a RolapCubeHierarchy.
     *
     * @return List of hierarchies
     */
    List<Hierarchy> getHierarchies();

    /**
     * Whether cells of this cube can be written back to.
     * <p>
     * True exactly when the cube declares a writeback table: without one there is
     * nowhere for a committed value to go, and {@link #commit} would silently do
     * nothing. This is what {@code MDSCHEMA_CUBES.IS_WRITE_ENABLED} reports, and
     * clients decide by it whether to offer writeback at all.
     */
    default boolean isWriteEnabled() {
        return false;
    }

    /**
     * Runs {@code work} with these pending writeback rows in effect on this cube.
     * <p>
     * The bracket exists because {@link #modifyFact} rewrites state the whole cube
     * shares - the fact source and the star built from it - so two callers doing it
     * at once would read each other's uncommitted values. Everything that queries a
     * writeback-enabled cube goes through here, and an implementation is free to
     * make the span exclusive; the default one, for cubes that cannot be written
     * to, only guarantees the restore.
     * <p>
     * Note that a cube with a writeback table needs this even when
     * {@code sessionValues} is empty: the rows already committed live in that table
     * and only reach a query through the rewritten fact.
     */
    default <T> T withPendingRows(List<Map<String, Entry<DataTypeJdbc, Object>>> sessionValues,
            Supplier<T> work) {
        modifyFact(sessionValues);
        try {
            return work.get();
        } finally {
            restoreFact();
        }
    }

    void modifyFact(List<Map<String, Entry<DataTypeJdbc, Object>>> sessionValues);

    void restoreFact();

    void commit(List<Map<String, Map.Entry<DataTypeJdbc, Object>>> sessionValues, String userId);

    List<Map<String, Entry<DataTypeJdbc, Object>>> getAllocationValues(String tupleString, Object value,
            AllocationPolicy allocationPolicy, Role role);

    /**
     * Returns the time hierarchy for this cube. If there is no time hierarchy,
     * throws.
     */
    Hierarchy getTimeHierarchy(String name);

    boolean isLoadInProgress();

    /**
     * Whether this cube takes its measures from other cubes instead of holding facts of its own. A
     * cube that does has dimensions a given measure never reaches, so a cell of such a pairing is
     * empty rather than wrong.
     *
     * <p>Asked rather than tested for a type, so that a catalog which groups its measures some other
     * way can answer it too.
     */
    default boolean drawsFromOtherCubes() {
        return false;
    }

    /**
     * Whether a dimension that the cube holding the measure does not reach is left as it stands instead
     * of emptying the cell. Only a cube that draws from others is ever asked.
     *
     * <p>
     * Asked with the measure itself rather than with the cube it comes from: a catalog whose measures are
     * not grouped by cube has no such cube to name, and the caller holds the measure anyway.
     */
    default boolean unrelatedDimensionsStayFor(Measure measure) {
        return false;
    }

    /**
     * Whether a client may write into this cube's cells at all. A catalog that keeps nowhere to put a
     * written value says no, which is what a client is told before it tries.
     */
    default boolean takesWrites() {
        return false;
    }
}
