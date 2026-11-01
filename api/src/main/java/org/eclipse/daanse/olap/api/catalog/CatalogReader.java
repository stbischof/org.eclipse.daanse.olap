/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2003-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara
 * Copyright (C) 2021 Sergei Semenkov
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
package org.eclipse.daanse.olap.api.catalog;

import java.util.List;
import java.util.Map;


import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.NameResolver;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.access.AccessMember;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.agg.Segment;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.element.db.DatabaseSchema;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.evaluator.NativeEvaluator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.component.Expression;

/**
 * A CatalogReader queries schema objects ( Catalog}, Cube}, Dimension},
 * Hierarchy, Level, Member).
 *
 * It is generally created using Connection#getCatalogReader, but also via
 * Cube#getCatalogReader(Role).
 *
 * CatalogReader is deprecated for code outside of mondrian. For new code, use
 * the metadata provided by olap4j, for example
 * mondrian.olap4j.MondrianOlap4jSchema#getCubes()}.
 *
 * If you use a CatalogReader from outside of a mondrian statement, you may get
 * a java.util.EmptyStackException indicating that mondrian cannot deduce the
 * current locus (statement context). If you get that error, call #withLocus()
 * to create a CatalogReader that automatically provides a locus whenever a call
 * is made.
 *
 * @author jhyde
 * @since Feb 24, 2003
 */
public interface CatalogReader {
    /**
     * Returns the schema.
     *
     * @return Schema, never null
     */
    Catalog getCatalog();

    /**
     * Returns the access-control profile that this CatalogReader is implementing.
     */
    Role getRole();

    /**
     * Returns the accessible dimensions of a cube.
     *
     * dimension != null return != null
     */
    List<Dimension> getCubeDimensions(Cube cube);

    /**
     * Returns the accessible hierarchies of a dimension.
     *
     * dimension != null return != null
     */
    List<Hierarchy> getDimensionHierarchies(Dimension dimension);

    /**
     * Returns an array of the root members of hierarchy.
     *
     * @param hierarchy Hierarchy
     * @see #getCalculatedMembers(Hierarchy)
     */
    List<Member> getHierarchyRootMembers(Hierarchy hierarchy);

    /**
     * Returns number of children parent of a member, if the information can be
     * retrieved from cache, otherwise -1.
     */
    int getChildrenCountFromCache(Member member);

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

    /**
     * Substitutes a member with an equivalent member which enforces the access
     * control policy of this CatalogReader.
     */
    Member substitute(Member member);

    /**
     * Returns direct children of member. member != null return != null
     */
    List<Member> getMemberChildren(Member member);

    /**
     * Returns direct children of member, optimized for NON EMPTY.
     *
     * If context == null then there is no context and all members are returned -
     * then its identical to #getMemberChildren(Member). If context is not null, the
     * resulting members may be restricted to those members that have a non empty
     * row in the fact table for context. Wether or not optimization is possible
     * depends on the CatalogReader implementation.
     */
    List<Member> getMemberChildren(Member member, Evaluator context);

    /**
     * Returns direct children of each element of members.
     *
     * @param members Array of members
     * @return array of child members
     *
     *         members != null return != null
     */
    List<Member> getMemberChildren(List<Member> members);

    /**
     * Returns direct children of each element of members which is not empty in
     * context.
     *
     * @param members Array of members
     * @param context Evaluation context
     * @return array of child members
     *
     *         members != null return != null
     */
    List<Member> getMemberChildren(List<Member> members, Evaluator context);

    /**
     * Returns a list of contributing children of a member of a parent-child
     * hierarchy.
     *
     * @param dataMember Data member for a member of the parent-child hierarcy
     * @param hierarchy  Hierarchy
     * @param list       List of members to populate
     */
    void getParentChildContributingChildren(Member dataMember, Hierarchy hierarchy, List<Member> list);

    /**
     * Returns the parent of member.
     *
     * @param member Member member != null
     * @return null if member is a root member
     */
    Member getMemberParent(Member member);

    /**
     * Returns a list of ancestors of member, in depth order.
     *
     * For example, for [Store].[USA].[CA], returns {[Store].[USA], [Store].[All
     * Stores]}.
     *
     * @param member       Member
     * @param ancestorList List of ancestors
     */
    void getMemberAncestors(Member member, List<Member> ancestorList);

    /**
     * Returns the depth of a member.
     *
     * This may not be the same as member. Member#getLevel getLevel().
     * Level#getDepth getDepth() for three reasons: Access control. The most senior
     * visible member has level 0. If the client is not allowed to see the "All" and
     * "Nation" levels of the "Store" hierarchy, then members of the "State" level
     * will have depth 0. Parent-child hierarchies. Suppose Fred reports to Wilma,
     * and Wilma reports to no one. "All Employees" has depth 0, Wilma has depth 1,
     * and Fred has depth 2. Fred and Wilma are both in the "Employees" level, which
     * has depth 1. Ragged hierarchies. If Israel has only one, hidden, province
     * then the depth of Tel Aviv, Israel is 2, whereas the depth of another city,
     * San Francisco, CA, USA is 3.
     *
     */
    int getMemberDepth(Member member);

    /**
     * Finds a member based upon its unique name.
     *
     * @param uniqueNameParts Unique name of member
     * @param failIfNotFound  Whether to throw an error, as opposed to returning
     *                        null, if there is no such member.
     * @param matchType       indicates the match mode; if not specified, EXACT
     * @return The member, or null if not found
     */
    Member getMemberByUniqueName(List<Segment> uniqueNameParts, boolean failIfNotFound, MatchType matchType);

    /**
     * Finds a member based upon its unique name, requiring an exact match.
     *
     * This method is equivalent to calling #getMemberByUniqueName(java.util.List,
     * boolean, MatchType) with MatchType#EXACT.
     *
     * @param uniqueNameParts Unique name of member
     * @param failIfNotFound  Whether to throw an error, as opposed to returning
     *                        null, if there is no such member.
     * @return The member, or null if not found
     */
    Member getMemberByUniqueName(List<Segment> uniqueNameParts, boolean failIfNotFound);

    /**
     * Looks up an MDX object by name, specifying how to match if no object exactly
     * matches the name.
     *
     * Resolves a name such as '[Products]&#46;[Product Department]&#46;[Produce]'
     * by resolving the components ('Products', and so forth) one at a time.
     *
     * @param parent         Parent element to search in
     * @param names          Exploded compound name, such as {"Products", "Product
     *                       Department", "Produce"}
     * @param failIfNotFound If the element is not found, determines whether to
     *                       return null or throw an error
     * @param category       Type of returned element, a DataType} value;
     *                       DataType#UNKNOWN if it doesn't matter.
     * @param matchType      indicates the match mode; if not specified, EXACT
     *
     *                       parent != null !(failIfNotFound and return == null)
     */
    OlapElement lookupCompound(OlapElement parent, List<Segment> names, boolean failIfNotFound, DataType category,
            MatchType matchType);

    /**
     * Looks up an MDX object by name.
     *
     * Resolves a name such as '[Products]&#46;[Product Department]&#46;[Produce]'
     * by resolving the components ('Products', and so forth) one at a time.
     *
     * @param parent         Parent element to search in
     * @param names          Exploded compound name, such as {"Products", "Product
     *                       Department", "Produce"}
     * @param failIfNotFound If the element is not found, determines whether to
     *                       return null or throw an error
     * @param category       Type of returned element, a DataType} value;
     *                       DataType#UNKNOWN} if it doesn't matter.
     *
     *                       parent != null !(failIfNotFound and return == null)
     */
    OlapElement lookupCompound(OlapElement parent, List<Segment> names, boolean failIfNotFound, DataType category);

    /**
     * Should only be called by implementations of #lookupCompound(OlapElement,
     * java.util.List, boolean, int, MatchType).
     *
     * @param parent         Parent element to search in
     * @param names          Exploded compound name, such as {"Products", "Product
     *                       Department", "Produce"}
     * @param failIfNotFound If the element is not found, determines whether to
     *                       return null or throw an error
     * @param category       Type of returned element, a Category} value;
     *                       Category#UNKNOWN if it doesn't matter.
     * @param matchType      indicates the match mode; if not specified, EXACT
     * @return Found element
     * 
     *         OlapElement lookupCompoundInternal( OlapElement parent,
     *         List<Id.Segment> names, boolean failIfNotFound, int category,
     *         MatchType matchType);
     */

    /**
     * Looks up a calculated member by name. If the name is not found in the current
     * scope, returns null.
     */
    Member getCalculatedMember(List<Segment> nameParts);

    /**
     * Looks up a set by name. If the name is not found in the current scope,
     * returns null.
     */
    NamedSet getNamedSet(List<Segment> nameParts);

    /**
     * Appends to list all members between startMember and endMember (inclusive)
     * which belong to level.
     */
    void getMemberRange(Level level, Member startMember, Member endMember, List<Member> list);

    /**
     * Returns a member n further along in the same level from member.
     *
     * member != null
     */
    Member getLeadMember(Member member, int n);

    /**
     * Compares a pair of Members according to their order in a prefix traversal.
     * (that is, it is an ancestor or a earlier), is a sibling, or comes later in a
     * prefix traversal.
     * 
     * @return A negative integer if m1 is an ancestor, an earlier sibling of an
     *         ancestor, or a descendent of an earlier sibling, of m2; zero if m1 is
     *         a sibling of m2; a positive integer if m1 comes later in the prefix
     *         traversal then m2.
     */
    int compareMembersHierarchically(Member m1, Member m2);

    /**
     * Looks up the child of parent called name, or an approximation according to
     * matchType, returning null if no element is found.
     *
     * @param parent    Parent element to search in
     * @param name      Compound in compound name, such as "[Product]" or "[1]"
     * @param matchType Match type
     *
     * @return Element with given name, or null
     */
    OlapElement getElementChild(OlapElement parent, Segment name, MatchType matchType);

    /**
     * Looks up the child of parent name, returning null if no element is found.
     *
     * Always equivalent to getElementChild(parent, name, MatchType.EXACT).
     *
     * @param parent Parent element to search in
     * @param name   Compound in compound name, such as "[Product]" or "[1]"
     *
     * @return Element with given name, or null
     */
    OlapElement getElementChild(OlapElement parent, Segment name);

    /**
     * Returns the members of a level, optionally including calculated members.
     */
    List<Member> getLevelMembers(Level level, boolean includeCalculated);

    List<Member> getLevelMembers(Level level, boolean includeCalculated, Evaluator context);

    /**
     * Returns the members of a level, optionally filtering out members which are
     * empty.
     *
     * @param level   Level
     * @param context Context for filtering
     * @return Members of this level
     */
    List<Member> getLevelMembers(Level level, Evaluator context);

    /**
     * Returns the accessible levels of a hierarchy.
     *
     * @param hierarchy Hierarchy
     *
     *                  hierarchy != null return.length >= 1
     */
    List<Level> getHierarchyLevels(Hierarchy hierarchy);

    /**
     * Returns the default member of a hierarchy. If the default member is in an
     * inaccessible level, returns the nearest ascendant/descendant member.
     *
     * @param hierarchy Hierarchy
     *
     * @return Default member of hierarchy
     */
    Member getHierarchyDefaultMember(Hierarchy hierarchy);

    /**
     * Returns whether a member has visible children.
     */
    boolean isDrillable(Member member);

    /**
     * Returns whether a member is visible.
     */
    boolean isVisible(Member member);

    /**
     * Returns the list of accessible cubes.
     */
    List<Cube> getCubes();

    /**
     * Returns a list of calculated members in a given hierarchy.
     */
    List<Member> getCalculatedMembers(Hierarchy hierarchy);

    /**
     * Returns a list of calculated members in a given level.
     */
    List<Member> getCalculatedMembers(Level level);

    /**
     * Returns the list of calculated members.
     */
    List<Member> getCalculatedMembers();

    /**
     * Finds a child of a member with a given name.
     */
    Member lookupMemberChildByName(Member parent, Segment childName, MatchType matchType);

    /**
     * Finds a list of child members with the given names.
     */
    List<Member> lookupMemberChildrenByNames(Member parent, List<NameSegment> childNames, MatchType matchType);

    /**
     * Returns an object which can evaluate an expression in native SQL, or null if
     * this is not possible.
     *
     * @param fun       Function
     * @param args      Arguments to the function
     * @param evaluator Evaluator, provides context
     * @param calc      the calc to be natively evaluated
     */
    NativeEvaluator getNativeSetEvaluator(FunctionDefinition fun, Expression[] args, Evaluator evaluator, Calc calc);

    /**
     * Returns the definition of a parameter with a given name, or null if not
     * found.
     */
    Parameter getParameter(String name);

    /**
     * Returns a similar schema reader that has no access control.
     *
     * @return Schema reader that has a similar perspective (e.g. cube) but no
     *         access control
     */
    CatalogReader withoutAccessControl();

    /**
     * Returns a schema reader that automatically assigns a locus to each operation.
     *
     * It is less efficient; use this only if the operation is occurring outside the
     * context of a statement. If you get the internal error "no locus", that's a
     * sign you should use this method.
     *
     * @return Schema reader that assigns a locus to each operation
     */
    CatalogReader withLocus();

    /**
     * Returns a list of namespaces to search when resolving elements by name.
     *
     * For example, a schema reader from the perspective of a cube will return cube
     * and schema namespaces.
     *
     * @return List of namespaces
     */
    List<NameResolver.Namespace> getNamespaces();

    /**
     * Similar to #getMemberChildren(Member, Evaluator) but returns a map of the
     * grand-children and their access details and costs more to invoke because of
     * the access controls.
     *
     * Called by Hierarchy when determining the lowest access level of a Role within
     * a hierarchy.
     */
    Map<? extends Member, AccessMember> getMemberChildrenWithDetails(Member member, Evaluator evaluator);

    Context<?> getContext();

    List<? extends DatabaseSchema> getDatabaseSchemas();
}
