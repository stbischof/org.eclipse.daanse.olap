/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api.result;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Member;

/**
 * Context for a set of writeback operations.
 *
 * An analyst performing a what-if analysis would first create a scenario, then
 * modify a sequence of cell values.
 *
 * This is an engine concept, not a protocol one: XMLA has no scenario. Where
 * [MS-SSAS] names a handle for pending writeback at all it is the {@code ResultId}
 * of the {@code KeepResult}/{@code Result}/{@code ClearResult} headers, and the
 * specification never ties that handle's lifetime to a session. Who holds a
 * scenario, and for how long, is therefore the caller's decision.
 *
 * Two kinds of pending state live here and they behave differently. The rows
 * added through {@link #addPendingRows} are what a commit writes to a cube's
 * writeback table; they belong to the cube they were produced for, which is why
 * they are kept per cube rather than in one list. The cells recorded by
 * {@link #setCellValue}'s numeric path are in-memory what-if only and are never
 * made permanent - see {@link #getWritebackCells()}.
 *
 * Of jhyde's original description, the identity half is not implemented here: a
 * scenario cannot be named, saved, or reopened.
 *
 * @see AllocationPolicy
 *
 * @author jhyde
 * @since 24 April, 2009
 */
public interface Scenario {
    /**
     * Returns the unique identifier of this Scenario.
     *
     * The format of the string returned is implementation defined. Client
     * applications must not make any assumptions about the structure or contents of
     * such strings. Nothing can be looked up by it.
     *
     * @return Unique identifier of this Scenario.
     */
    String getId();

    /**
     * The cells the numeric what-if path recorded.
     *
     * These are held in memory and never written to the database - a commit does
     * not look at them. They are read by the {@code [Scenario]} member evaluator,
     * which no catalog can currently reach.
     */
    List<WritebackCell> getWritebackCells();

    /**
     * Pushes a writeback value into the scenario.
     *
     * Historically both {@code newValue} and {@code currentValue} were {@code double}
     * primitives. They are widened to {@link Object} so non-numeric writeback
     * (e.g. comments backed by a {@code TextAggMeasure}) can pass through. Numeric
     * call-sites can keep passing {@link Number}s; the implementation re-narrows them
     * only when the target {@code RolapWritebackMeasure} declares a numeric datatype.
     */
    void setCellValue(Connection connection, List<Member> members, Object newValue, Object currentValue,
            AllocationPolicy allocationPolicy, Object[] allocationArgs);

    /**
     * The rows pending for one cube, in the order they were produced.
     *
     * Empty when nothing is pending for it. These are the rows a commit writes to
     * that cube's writeback table, and the rows its fact source is rewritten with
     * while the values are still uncommitted.
     */
    List<Map<String, Map.Entry<DataTypeJdbc, Object>>> pendingRows(Cube cube);

    /**
     * Records rows produced for one cube.
     *
     * The cube is part of the record because a row only means anything against the
     * cube it was allocated for: handing it to another one rewrites that cube's
     * facts with values that were never meant for it.
     */
    void addPendingRows(Cube cube, List<Map<String, Map.Entry<DataTypeJdbc, Object>>> rows);

    /** Every cube something is pending for. */
    Set<Cube> pendingCubes();

    /**
     * Forgets the pending rows of ONE cube - called after that cube's rows
     * were made permanent, so a retry of a partially failed multi-cube
     * commit does not write them a second time.
     */
    void clearPendingRows(Cube cube);

    /**
     * Whether anything is pending, of either kind. While this is {@code true}
     * the session reads cells through a private cache overlay instead of the
     * shared segment caches.
     */
    boolean hasPendingChanges();

    /** Forgets everything pending, of both kinds. */
    void clear();

    /**
     * Makes what this stand holds permanent, for an art that keeps it somewhere of its own rather than as
     * rows per cube.
     *
     * <p>Answering {@code false} means "not mine to do": the caller then walks {@link #pendingCubes()} and
     * hands each cube its own rows, which is how an art with a writeback table per cube works. An art that
     * hands what it holds to a sink of its own does that here and answers {@code true}.
     *
     * @param userId who is writing, for an art that records it
     * @return whether this stand made itself permanent
     */
    default boolean makePermanent(String userId) {
        return false;
    }

    /**
     * Drops what this stand holds, for an art that keeps it somewhere of its own. Answers {@code false} when
     * there is nothing of its own to drop, and the caller forgets the stand as before.
     *
     * @return whether this stand took its values back itself
     */
    default boolean takeBack() {
        return false;
    }
}
