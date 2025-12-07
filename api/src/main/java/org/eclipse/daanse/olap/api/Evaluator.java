/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
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

package org.eclipse.daanse.olap.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.olap.api.calc.compiler.ParameterSlot;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Query;

/**
 * An Evaluator holds the context necessary to evaluate an
 * expression.
 *
 * @author jhyde
 * @since 27 July, 2001
 */
public interface Evaluator{

    /**
     * Returns the current cube.
     */
    Cube getCube();

    /**
     * Returns the current query.
     */
    Query getQuery();

    /**
     * Returns the start time of the current query.
     */
    LocalDateTime getQueryStartTime();

    /**
     * Creates a savepoint encapsulating the current state of the evalutor.
     * You can restore the evaluator to this state by calling
     *  #restore(int) with the value returned by this method.
     *
     * This method is typically called before evaluating an expression which
     * is known to corrupt the evaluation context.
     *
     * Multiple savepoints may be active at the same time for the same
     * evaluator. And, it is allowable to restore to the save savepoint more
     * than once (or not at all). However, when you have rolled back to a
     * particular savepoint you may not restore to a later savepoint.
     *
     * @return Evaluator with each given member overriding the state of the
     *   current Evaluator for its hierarchy
     */
    int savepoint();

    /**
     * Creates a new Evaluator with the same context as this evaluator.
     *
     * This method is typically called before evaluating an expression which
     * may corrupt the evaluation context.
     *
     * In mondrian-3.3 and later, a more efficient way to save the state of
     * an evaluator is to call  #savepoint followed by
     *  #restore(int). We recommend using those methods most of the time.
     *
     * However, it makes sense to use this method in the constructor of an
     * iterator. It allows the iterator to modify its evaluation context without
     * affecting the evaluation context of the calling code. This behavior
     * cannot be achieved using {@code savepoint}.
     *
     * @return Evaluator with each given member overriding the state of the
     *   current Evaluator for its hierarchy
     */
    Evaluator push();

    /**
     * Restores previous evaluator.
     *
     * @param savepoint Savepoint returned by  #savepoint()
     */
    void restore(int savepoint);

    /**
     * Makes member the current member of its hierarchy.
     *
     * @param member  New member
     *
     * @return Previous member of this hierarchy
     */
    Member setContext(Member member);

    /**
     * Makes member the current member of its hierarchy.
     *
     * If {@code safe}, checks whether this is the first time that
     * a member of this hierarchy has been changed since  #savepoint()
     * was called. If so, saves the previous member. If {@code safe} is false,
     * never saves the previous member.
     *
     * Use {@code safe = false} only if you are sure that the context has
     * been set before. For example,
     *
     *
     * int n = 0;
     * for (Member member : members) {
     * &nbsp;&nbsp;evaluator.setContext(member, n++ &gt; 0);
     * }
     *
     * @param member  New member
     * @param safe    Whether to store the member of this hierarchy that was
     *                current last time that  #savepoint() was called.
     */
    void setContext(Member member, boolean safe);

    /**
     * Sets the context to a list of members.
     *
     * Equivalent to
     *
     * for (Member member : memberList) {
     * &nbsp;&nbsp;setContext(member);
     * }
     *
     * @param memberList List of members
     */
    void setContext(List<Member> memberList);

    /**
     * Sets the context to a list of members, optionally skipping the check
     * whether it is necessary to store the previous member of each hierarchy.
     *
     * Equivalent to
     *
     * for (Member member : memberList) {
     * &nbsp;&nbsp;setContext(member, safe);
     * }
     *
     * @param memberList List of members
     * @param safe    Whether to store the member of each hierarchy that was
     *                current last time that  #savepoint() was called.
     */
    void setContext(List<Member> memberList, boolean safe);

    /**
     * Sets the context to an array of members.
     *
     * Equivalent to
     *
     * for (Member member : memberList) {
     * &nbsp;&nbsp;setContext(member);
     * }
     *
     * @param members Array of members
     */
    void setContext(Member[] members);

    /**
     * Sets the context to an array of members, optionally skipping the check
     * whether it is necessary to store the previous member of each hierarchy.
     *
     * Equivalent to
     *
     * for (Member member : memberList) {
     * &nbsp;&nbsp;setContext(member, safe);
     * }
     *
     * @param members Array of members
     * @param safe    Whether to store the member of each hierarchy that was
     *                current last time that  #savepoint() was called.
     */
    void setContext(Member[] members, boolean safe);

    Member getContext(Hierarchy hierarchy);

    /**
     * Calculates and returns the value of the cell at the current context.
     */
    Object evaluateCurrent();

    /**
     * Returns the format string for this cell. This is computed by evaluating
     * the format expression in the current context, and therefore different
     * cells may have different format strings.
     */
    public String getFormatString();

    /**
     * Formats a value as a string according to the current context's
     * format.
     */
    String format(Object o);

    /**
     * Formats a value as a string according to the current context's
     * format, using a given format string.
     */
    String format(Object o, String formatString);

    /**
     * Returns the connection's locale.
     */
    Locale getConnectionLocale();

    /**
     * Retrieves the value of property name. If more than one
     * member in the current context defines that property, the one with the
     * highest solve order has precedence.
     *
     * If the property is not defined, default value is returned.
     */
    Object getProperty(String name, Object defaultValue);

    /**
     * Returns a  CatalogReader appropriate for the current
     * access-control context.
     */
    CatalogReader getCatalogReader();

    /**
     * Simple caching of the result of an Exp. The
     * key for the cache consists of all members of the current
     * context that exp depends on. Members of
     * independent hierarchies are not part of the key.
     * 
     */
    Object getCachedResult(ExpCacheDescriptor key);

    /**
     * Returns true for an axis that is NON EMPTY.
     *
     * May be used by expression
     * evaluators to optimize their result. For example, a top-level crossjoin
     * may be optimized by removing all non-empty set elements before
     * performing the crossjoin. This is possible because of the identity
     *
     * nonempty(crossjoin(a, b)) ==
     * nonempty(crossjoin(nonempty(a), nonempty(b));
     */
    boolean isNonEmpty();

    /**
     * Sets whether an expression evaluation should filter out empty cells.
     * Allows expressions to modify non empty flag to evaluate their children.
     */
    void setNonEmpty(boolean nonEmpty);

    /**
     * Creates an exception which indicates that an error has occurred during
     * the runtime evaluation of a function. The caller should then throw that
     * exception.
     */
    RuntimeException newEvalException(Object context, String s);

    /**
     * Returns an evaluator for a set.
     *
     * @param exp Expression
     * @param create Whether to create evaluator if not found
     * @return Evaluator of named set
     */
    SetEvaluator getSetEvaluator(Expression exp, boolean create);

    /**
     * Returns an evaluator for a named set.
     *
     * @param namedSet Named set
     * @param create Whether to create evaluator if not found
     * @return Evaluator of named set
     */
    NamedSetEvaluator getNamedSetEvaluator(NamedSet namedSet, boolean create);

    /**
     * Returns an array of the members which make up the current context.
     */
    Member[] getMembers();

    /**
     * Returns an array of the non-All members which make up the current
     * context.
     *
     * Notes:
     * The 0th element is a measure, but otherwise the order of the
     *     members is unspecified.
     * No hierarchy occurs more than once.
     * In rare circumstances, some of the members may be an 'All' member.
     * The list may contain calculated members.
     *
     */
    Member[] getNonAllMembers();

    /**
     * Returns the number of times that this evaluator has told a lie when
     * retrieving cell values.
     */
    int getMissCount();

    /**
     * Returns the value of a parameter, evaluating its default value if it is
     * not set.
     */
    Object getParameterValue(ParameterSlot slot);

    /**
     * @return the iteration length of the current context
     */
    int getIterationLength();

    /**
     * Sets the iteration length for the current evaluator context
     *
     * @param length length to be set
     */
    void setIterationLength(int length);

    /**
     * @return true if evaluating axes
     */
    boolean isEvalAxes();

    /**
     * Indicate whether the evaluator is evaluating the axes
     *
     * @param evalAxes true if evaluating axes
     */
    void setEvalAxes(boolean evalAxes);

    /**
     * Returns a new Aggregator whose aggregation context adds a given list of
     * tuples, and whose evaluation context is the same as this
     * Aggregator.
     *
     * @param list List of tuples
     * @return Aggregator with list added to its aggregation
     *   context
     */
    Evaluator pushAggregation(List<List<Member>> list);

    /**
     * Returns the base (non-virtual) cube that the current measure in the
     * context belongs to.
     * @return Cube
     */
    Cube getMeasureCube();

    /**
     * Returns whether it is necessary to check whether to return null for
     * an unrelated dimension. If false, we never need to check: we can assume
     * that  #needToReturnNullForUnrelatedDimension(org.eclipse.daanse.olap.api.element.Member[])
     * will always return false.
     *
     * @return whether it is necessary to check whether to return null for
     * an unrelated dimension
     */
    boolean mightReturnNullForUnrelatedDimension();

    /**
     * If IgnoreMeasureForNonJoiningDimension is set to true and one or more
     * members are on unrelated dimension for the measure in current context
     * then returns true.
     *
     * You must not call this method unless
     *  #mightReturnNullForUnrelatedDimension() has returned true.
     *
     * @param members Dimensions for the members need to be checked whether
     *     related or unrelated
     *
     * @return boolean
     */
    boolean needToReturnNullForUnrelatedDimension(Member[] members);

    /**
     * Returns whether native evaluation is enabled in this context.
     *
     * @return whether native evaluation is enabled in this context
     */
    boolean nativeEnabled();

    /**
     * Sets whether native evaluation should be used.
     *
     * @param nativeEnabled Whether native evaluation should be used
     */
   void setNativeEnabled(boolean nativeEnabled);

    /**
     * Returns whether the current context is an empty cell.
     *
     * @return Whether the current context is an empty cell
     */
    boolean currentIsEmpty();

    /**
     * Returns the member that was the current evaluation context for a
     * particular hierarchy before the most recent change in context.
     *
     * @param hierarchy Hierarchy
     * @return Previous context member for given hierarchy
     */
    Member getPreviousContext(Hierarchy hierarchy);

    /**
     * Returns the query timing context for this execution.
     *
     * @return query timing context
     */
    QueryTiming getTiming();

    /**
     * Return the list of slicer members in the current evaluator context.
     */
    List<Member> getSlicerMembers();

    Map<Hierarchy, Set<Member>> getSlicerMembersByHierarchy();

    /**
     * Interface for evaluating a particular named set.
     */
    interface NamedSetEvaluator {
        /**
         * Returns an iterator over the tuples of the named set. Applicable if
         * the named set is a set of tuples.
         *
         * The iterator from this iterable maintains the current ordinal
         * property required for the methods  #currentOrdinal() and
         *  #currentTuple().
         *
         * @param eval Evaluator for current context
         *
         * @return Iterable over the tuples of the set
         */
        TupleIterable evaluateTupleIterable(Evaluator eval);

        /**
         * Returns the ordinal of the current member or tuple in the named set.
         *
         * @return Ordinal of the current member or tuple in the named set
         */
        int currentOrdinal();

        /**
         * Returns the current member in the named set.
         *
         * Applicable if the named set is a set of members.
         *
         * @return Current member
         */
        Member currentMember();

        /**
         * Returns the current tuple in the named set.
         *
         * Applicable if the named set is a set of tuples.
         *
         * @return Current tuple.
         */
        Member[] currentTuple();
    }

    /**
     * Interface for generically evaluating a set.
     */
    interface SetEvaluator {
        /**
         * Returns an iterator over the tuples of the named set. Applicable if
         * the named set is a set of tuples.
         *
         * The iterator from this iterable maintains the current ordinal
         * property required for the methods  #currentOrdinal() and
         *  #currentTuple().
         *
         * @return Iterable over the tuples of the set
         */
        TupleIterable evaluateTupleIterable();

        /**
         * Returns the ordinal of the current member or tuple in the named set.
         *
         * @return Ordinal of the current member or tuple in the named set
         */
        int currentOrdinal();

        /**
         * Returns the current member in the named set.
         *
         * Applicable if the named set is a set of members.
         *
         * @return Current member
         */
        Member currentMember();

        /**
         * Returns the current tuple in the named set.
         *
         * Applicable if the named set is a set of tuples.
         *
         * @return Current tuple.
         */
        Member[] currentTuple();
    }
}
