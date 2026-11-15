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
package org.eclipse.daanse.olap.updateable;

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.access.AccessCube;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Measure;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.writeback.Updateable;

/**
 * What a client may do to a cell, as the {@code UPDATEABLE} property names it.
 * <p>
 * Seven things stand in the way of a write, and they are asked in this order because the earlier ones are
 * cheaper and more absolute: a catalog that takes no writes at all, a coordinate that is worked out from a
 * formula, a measure that does not add up, a cell of a cube that draws from others where the measure does
 * not reach one of the coordinates, a dimension of a kind that cannot be written, a role that may not see
 * the cube, and finally whether the cell is a single fact or a rollup - which decides between writing and
 * spreading.
 * <p>
 * All seven are questions about the model, so they are the same for every kind of catalog. What a catalog
 * is made of shows only in how it answers {@link Cube#takesWrites()} and {@link Measure#addsUp()}.
 */
public final class UpdateabilityResolver {

    private UpdateabilityResolver() {
        // static access only
    }

    /**
     * @param coordinates the members the cell stands for
     * @param role        the role in force, empty when the cell knows none
     * @param cube        the cube the cell belongs to
     */
    public static Updateable resolve(List<Member> coordinates, Optional<Role> role, Cube cube) {
        if (!cube.takesWrites()) {
            return Updateable.CELL_UPDATE_NOT_ENABLED_CANNOTUPDATE;
        }
        if (coordinates.stream().anyMatch(Member::isCalculated)) {
            return Updateable.CELL_UPDATE_NOT_ENABLED_FORMULA;
        }
        Measure measure = measureOf(coordinates);
        if (measure == null || !measure.addsUp()) {
            return Updateable.CELL_UPDATE_NOT_ENABLED_NONSUM_MEASURE;
        }
        if (cube.drawsFromOtherCubes() && standsWhereTheMeasureDoesNotReach(measure, coordinates)) {
            return Updateable.CELL_UPDATE_NOT_ENABLED_NACELL_VIRTUALCUBE;
        }
        if (role.isEmpty() || role.get().getAccess(cube) == AccessCube.NONE) {
            return Updateable.CELL_UPDATE_NOT_ENABLED_SECURE;
        }
        return everyCoordinateIsALeaf(coordinates) ? Updateable.CELL_UPDATE_ENABLED
                : Updateable.CELL_UPDATE_ENABLED_WITH_UPDATE;
    }

    /** The measure among the coordinates; a cell always stands at one, but a formula is not one. */
    private static Measure measureOf(List<Member> coordinates) {
        return coordinates.stream().filter(Measure.class::isInstance).map(Measure.class::cast).findFirst()
                .orElse(null);
    }

    /**
     * Whether the cell stands at a coordinate its measure says nothing about. Such a cell is empty by
     * definition, and writing into it would mean writing a fact the measure cannot hold.
     */
    private static boolean standsWhereTheMeasureDoesNotReach(Measure measure, List<Member> coordinates) {
        Member[] tuple = coordinates.stream().filter(member -> !member.getDimension().isMeasures())
                .toArray(Member[]::new);
        return !measure.dimensionsNotReached(tuple).isEmpty();
    }

    /** A cell of leaves is one fact; anything above it is a rollup and is written by spreading. */
    private static boolean everyCoordinateIsALeaf(List<Member> coordinates) {
        for (Member member : coordinates) {
            if (member.isAll() || member.getLevel().getChildLevel() != null) {
                return false;
            }
        }
        return true;
    }
}
