/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1999-2005 Julian Hyde
 * Copyright (C) 2005-2020 Hitachi Vantara and others
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
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.api.element;

import java.util.List;

import org.eclipse.daanse.olap.api.query.component.Expression;

/**
 * A Member is a 'point' on a dimension of a cube. Examples are
 * [Time].[1997].[January], [Customer].[All Customers], [Customer].[USA].[CA],
 * [Measures].[Unit Sales].
 *
 * Every member belongs to a Level of a Hierarchy. Members except the root
 * member have a parent, and members not at the leaf level have one or more
 * children.
 *
 * Measures are a special kind of member. They belong to their own dimension,
 * [Measures].
 *
 * There are also special members representing the 'All' value of a hierarchy,
 * the null value, and the error value.
 *
 * Members can have member properties. Their Level#getProperties defines which
 * are allowed.
 *
 * @author jhyde, 2 March, 1999
 */
public interface Member extends OlapElement, Comparable, MetaElement {

    /**
     * Returns this member's parent, or null (not the 'null member', as returned by
     * Hierarchy#getNullMember) if it has no parent.
     *
     * In an access-control context, a member may have no <em>visible</em> parents,
     * so use CatalogReader#getMemberParent.
     */
    Member getParentMember();

    Level getLevel();

    @Override
    Hierarchy getHierarchy();

    /**
     * Returns name of parent member, or empty string (not null) if we are the root.
     */
    String getParentUniqueName();

    /**
     * Returns the type of member.
     */
    MemberType getMemberType();

    /**
     * @return True when the member is a leaf member, meaning it has no children
     */
    boolean isParentChildLeaf();

    /**
     * @return True when the member is part of a Parent-Child hierarchy and it is a
     *         physical member. In a Parent Child Hierarchy without a closure table,
     *         each member needs to be treated as calculated. We need a way to
     *         distinguish between true calculated members and physical members that
     *         exist in the source data
     */
    boolean isParentChildPhysicalMember();

    enum MemberType {
        UNKNOWN, REGULAR, // adMemberRegular
        ALL, MEASURE, FORMULA,
        /**
         * This member is its hierarchy's NULL member (such as is returned by
         * [Gender]&#46;[All Gender]&#46;PrevMember, for example).
         */
        NULL
    }

    /**
     * Only allowable if the member is part of the WITH clause of a query.
     */
    void setName(String name);

    /**
     * Returns whether this is the 'all' member.
     */
    boolean isAll();

    /**
     * Returns whether this is a member of the measures dimension.
     */
    boolean isMeasure();

    /**
     * Returns whether this is the 'null member'.
     */
    boolean isNull();

    /**
     * Returns whether member is equal to, a child, or a descendent of this Member.
     */
    boolean isChildOrEqualTo(Member member);

    /**
     * Returns whether this member is computed using either a with member clause in
     * an mdx query or a calculated member defined in cube.
     */
    boolean isCalculated();

    /**
     * Returns whether this member should be evaluated within the Evaluator.
     *
     * Normally #isCalculated and #isEvaluated should return the same value, but in
     * situations where mondrian would like to treat the two concepts separately
     * such in role based security, these values may differ.
     *
     * @return true if evaluated
     */
    boolean isEvaluated();

    int getSolveOrder();

    Expression getExpression();

    /**
     * Returns a list of the ancestor members of this member.
     *
     * @deprecated Use CatalogReader#getMemberAncestors(Member, java.util.List).
     */
    @Deprecated
    List<Member> getAncestorMembers();

    /**
     * Returns whether this member is computed from a {@code WITH MEMBER} clause in
     * an MDX query.
     */
    boolean isCalculatedInQuery();

    /**
     * Returns the value of the property named propertyName. Name match is
     * case-sensitive.
     */
    Object getPropertyValue(String propertyName);

    /**
     * Returns the value of the property named propertyName, matching according to
     * the required case-sensitivity.
     */
    Object getPropertyValue(String propertyName, boolean matchCase);

    /**
     * Returns the formatted value of the property named propertyName.
     */
    String getPropertyFormattedValue(String propertyName);

    /**
     * Sets a property of this member to a given value.
     */
    void setProperty(String name, Object value);

    /**
     * Returns the definitions of the properties this member may have.
     */
    Property[] getProperties();

    /**
     * Returns the ordinal of the member.
     */
    int getOrdinal();

    /**
     * Returns the order key of the member (relative to its siblings); null if
     * undefined or unavailable.
     */
    Comparable getOrderKey();

    /**
     * Returns whether this member is 'hidden', as per the rules which define a
     * ragged hierarchy.
     */
    boolean isHidden();

    /**
     * returns the depth of this member, which is not the level's depth in case of
     * parent child dimensions
     *
     * @return depth
     */
    int getDepth();

    /**
     * Returns the system-generated data member that is associated with a nonleaf
     * member of a dimension.
     *
     * Returns this member if this member is a leaf member, or if the nonleaf member
     * does not have an associated data member.
     */
    Member getDataMember();

    /**
     * Returns true if this member is on the same hierarchy chain as otherMember.
     *
     * @param otherMember
     * @return
     */
    boolean isOnSameHierarchyChain(Member otherMember);

    /**
     * The member this one stands for, itself when it stands for no other.
     *
     * <p>A cube shows the members of a shared hierarchy as its own, one set per use it makes of the
     * hierarchy, so the same member appears under several names. What such a member <em>means</em> is the
     * shared member behind it, and that is what anything looking for facts must follow.
     *
     * @return the member behind this one, never null
     */
    default Member standsFor() {
        return this;
    }

    /**
     * The member this one shows, itself when it shows no other.
     *
     * <p>Two relations are easy to confuse and must not be. {@link #standsFor()} is the <em>view</em> of a
     * member: a cube shows the members of a shared hierarchy as its own, and such a member is the same
     * member seen from a cube. This one is the <em>derived</em> member: a visual total, a member rolled up
     * over what a role may see, a member standing in for a set in the slicer. It is a member of its own with
     * a value of its own, and it shows another.
     *
     * <p>Whoever compares members has to say which of the two it means. Following both at once reads a
     * visual total of a cube's view of a shared member as that shared member, which is two steps and one
     * meaning too far.
     *
     * @return the member shown by this one, never null
     */
    default Member shows() {
        return this;
    }
}
