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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.writeback;

import java.util.List;

import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.result.AllocationPolicy;
import org.eclipse.daanse.olap.api.result.WritebackCell;

/**
 * One cell a client wrote, remembered as it was written: which cell it was, what it held, what it should
 * hold, and by which rule the difference is spread. Nothing here is written anywhere -- a what-if value is
 * applied while a cell is evaluated, so the data stays as it was loaded.
 *
 * <p>How this cell relates to another one is the whole of the arithmetic: a cell that stands at the same
 * place takes the written value, one below it takes its share, one above it takes the difference. Which of
 * the three it is follows from the members alone, so every art works it out the same way.
 */
public class WritebackCellBase implements WritebackCell {

    private final List<Member> members;
    private final Member[] membersByOrdinal;
    private final double newValue;
    private final double currentValue;
    private final AllocationPolicy allocationPolicy;
    private final double atomicCellCount;

    /**
     * @param cube             the cube the cell belongs to, which says how many hierarchies there are
     * @param members          the cell's coordinate, as far as the query named it
     * @param newValue         what the client wrote
     * @param currentValue     what the cell held before
     * @param allocationPolicy how the difference is spread over what stands below
     * @param atomicCellCount  how many atomic cells this cell covered when it was written
     */
    protected WritebackCellBase(Cube cube, List<Member> members, double newValue, double currentValue,
            AllocationPolicy allocationPolicy, double atomicCellCount) {
        this.members = List.copyOf(members);
        this.membersByOrdinal = byOrdinal(cube, this.members);
        this.newValue = newValue;
        this.currentValue = currentValue;
        this.allocationPolicy = allocationPolicy;
        this.atomicCellCount = atomicCellCount;
    }

    /**
     * The coordinate laid out by the place each hierarchy takes in its cube, so that comparing two
     * coordinates is comparing member against member at the same index.
     *
     * <p>A hierarchy the write did not name stands at its <em>default</em> member, not at nothing: a cell
     * that differs only there is still above or below this one, and leaving a gap would call it the same
     * cell.
     */
    private static Member[] byOrdinal(Cube cube, List<Member> members) {
        List<Hierarchy> hierarchies = cube.getHierarchies();
        Member[] byOrdinal = new Member[hierarchies.size()];
        for (int i = 0; i < byOrdinal.length; i++) {
            byOrdinal[i] = hierarchies.get(i).getDefaultMember();
        }
        for (Member member : members) {
            byOrdinal[member.getHierarchy().getOrdinalInCube()] = member;
        }
        return byOrdinal;
    }

    /**
     * A written cell for an art that has nothing to add to one.
     *
     * @param cube             the cube the cell belongs to
     * @param members          the cell's coordinate, as far as the query named it
     * @param newValue         what the client wrote
     * @param currentValue     what the cell held before
     * @param allocationPolicy how the difference is spread over what stands below
     * @param atomicCellCount  how many atomic cells this cell covered when it was written
     * @return the written cell
     */
    public static WritebackCellBase of(Cube cube, List<Member> members, double newValue, double currentValue,
            AllocationPolicy allocationPolicy, double atomicCellCount) {
        return new WritebackCellBase(cube, members, newValue, currentValue, allocationPolicy, atomicCellCount);
    }

    /** The cell's coordinate, as it was written. */
    public List<Member> getMembers() {
        return members;
    }

    @Override
    public double getNewValue() {
        return newValue;
    }

    @Override
    public double getCurrentValue() {
        return currentValue;
    }

    @Override
    public AllocationPolicy getAllocationPolicy() {
        return allocationPolicy;
    }

    @Override
    public double getAtomicCellCount() {
        return atomicCellCount;
    }

    @Override
    public double getOffset() {
        return newValue - currentValue;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Read member by member: as long as every member of the one coordinate is this cell's member or
     * below it, this cell stands above; as long as it is the other way round, below; the same all the way
     * means the same cell; and a coordinate that is partly above and partly below is unrelated, because no
     * rule would say what to do with it.
     */
    @Override
    public CellRelation getRelationTo(Member[] coordinates) {
        int aboveCount = 0;
        int belowCount = 0;
        for (int i = 0; i < coordinates.length; i++) {
            Member that = coordinates[i];
            Member mine = membersByOrdinal[i];
            if (that.isChildOrEqualTo(mine)) {
                if (!that.equals(mine)) {
                    ++aboveCount;
                    if (belowCount > 0) {
                        return CellRelation.NONE;
                    }
                }
            } else if (mine.isChildOrEqualTo(that)) {
                ++belowCount;
                if (aboveCount > 0) {
                    return CellRelation.NONE;
                }
            } else {
                return CellRelation.NONE;
            }
        }
        if (aboveCount > 0) {
            return CellRelation.ABOVE;
        }
        return belowCount > 0 ? CellRelation.BELOW : CellRelation.EQUAL;
    }
}
