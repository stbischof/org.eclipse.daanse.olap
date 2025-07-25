/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
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
 *   Stefan Bischof (bipolis.org) - initial
 */


package org.eclipse.daanse.olap.api.calc.todo;

import java.util.List;

import org.eclipse.daanse.olap.api.element.Member;

/**
 * List of tuples.
 *
 * Design notes
 *
 * 
 *
 * Consider changing
 *  TupleCalc#evaluateTuple(org.eclipse.daanse.olap.api.Evaluator)
 * and  org.eclipse.daanse.olap.api.Evaluator.NamedSetEvaluator#currentTuple()
 * to List&lt;Member&gt;
 *
 * Search for potential uses of  TupleList#get(int, int)
 *
 * Worth creating  TupleList.addAll(TupleIterator)?
 *
 * 
 *
 * @author jhyde
 */
public interface TupleList
extends List<List<Member>>, TupleIterable
{
    /**
     * Returns a particular column of a particular row.
     *
     * Note that {@code list.get(row, column)}
     * is equivalent to {@code list.slice(column).get(row)}
     * and {@code list.get(row).get(column)}
     * but is more efficient for most implementations of TupleList.
     *
     * @param slice Column ordinal
     * @param index Row ordinal
     * @return Member at given row and column
     */
    Member get(int slice, int index);

    /**
     * Returns a list of the members at a given column.
     *
     * The list is modifiable if and only if this TupleList is modifiable.
     * Adding an element to a slice will create a tuple whose members in other
     * columns are null.
     * Removing an element from a slicer will remove a tuple.
     *
     * @param column Ordinal of the member in each tuple to project
     * @return List of members
     * @throws IllegalArgumentException if column is not less than arity
     */
    @Override
    List<Member> slice(int column);

    /**
     * Creates a copy of this list that has the same type and has a given
     * capacity.
     *
     * If capacity is negative, populates the list. A deep copy is made,
     * so that it the contents of the list are not affected to changes to any
     * backing collections.
     *
     * @param capacity Capacity
     * @return Copy of list, empty if capacity is non-negative
     */
    TupleList copyList(int capacity);

    void addTuple(Member... members);

    TupleList project(int[] destIndices);

    void addCurrent(TupleCursor tupleIter);

    // override, refining return type
    @Override
    TupleList subList(int fromIndex, int toIndex);

    TupleList withPositionCallback(PositionCallback positionCallback);

    /**
     * Fixes the tuples of this list, so that their contents will not change
     * even if elements of the list are reordered or removed. Returns this
     * list if possible.
     *
     * @return List whose tuples are invariant if the list is sorted or filtered
     */
    TupleList fix();

    interface PositionCallback {
        void onPosition(int index);
    }
}
