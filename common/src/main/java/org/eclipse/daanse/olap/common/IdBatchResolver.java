/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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

package org.eclipse.daanse.olap.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.HierarchyExpression;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.query.component.IdImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to collect and resolve identifiers in groups of children
 * where possible.  For example, if an enumerated set within an MDX
 * query includes references to 10 stores under the parent
 *
 *   [USA].[CA].[San Francisco]
 *
 * the class will attempt to identify those 10 identifiers
 * and issue a single lookup, resulting in fewer and more efficient
 * SQL queries.
 * The resulting collection of resolved identifiers is returned in a
 * map of QueryPart, QueryPart, where the unresolved Exp object acts
 * as the key.
 *
 * This class makes no assurances that all identifiers will be resolved.
 * The map returned by .resolve() will include only those identifiers
 * successfully resolved.
 *
 */
public final class IdBatchResolver {
    static final Logger LOGGER = LoggerFactory.getLogger(IdBatchResolver.class);

    private final Query query;
    private final Formula[] formulas;
    private final QueryAxis[] axes;
    private final Cube cube;

    // dimension and hierarchy unique names are collected during init
    // to assist in classifying Ids as potentially resolvable to members.
    private final Collection<String> dimensionUniqueNames =
        new ArrayList<>();
    private final Collection<String> hierarchyUniqueNames =
        new ArrayList<>();
    // level names are checked against the identifiers to avoid incorrectly
    // interpreting a Dimension.Level reference as Dimension.Member.
    private final Collection<String> levelNames =
        new ArrayList<>();

    // Set of identifiers, sorted via IdComparator, which orders based
    // first on segment length (shortest to longest), then alphabetically.
    private  SortedSet<Id> identifiers = new TreeSet<>(new IdComparator());

    public IdBatchResolver(Query query) {
        this.query = query;
        formulas = query.getFormulas();
        axes = query.getAxes();
        cube = query.getCube();
        initOlapElementNames();
        initIdentifiers();
    }

    /**
     * Initializes the dimensionUniqueNames, hierarchyUniqueNames and
     * levelNames collections based on the contents of cube.  These collections
     * will be used to help determine whether identifiers correspond to
     * a dimension/hierarchy/level.
     */
    private void initOlapElementNames() {
        dimensionUniqueNames.addAll(
            getOlapElementNames(cube.getDimensions(), true));
        for (Dimension dim : cube.getDimensions()) {
            hierarchyUniqueNames.addAll(
                getOlapElementNames(dim.getHierarchies(), true));
            for (Hierarchy hier : dim.getHierarchies()) {
                levelNames.addAll(getOlapElementNames(hier.getLevels(), false));
            }
        }
    }

    /**
     * Initializes the identifiers collection by walking the axes
     * and formulas in the query and adding each encountered Id.
     * Finally, expands the set of identifiers to include parents.  E.g.
     * if the identifier
     *   [Store].[All Store].[USA].[CA]
     * is encountered, this will be expanded to include
     *   [Store].[All Store].[USA]
     *   [Store].[All Store]
     */
    private void initIdentifiers() {
        QueryComponentVisitor identifierVisitor = new IdentifierVisitor(identifiers);
        for (QueryAxis axis : axes) {
            axis.accept(identifierVisitor);
        }
        if (query.getSlicerAxis() != null) {
            query.getSlicerAxis().accept(identifierVisitor);
        }
        for (Formula formula : formulas) {
            formula.accept(identifierVisitor);
        }
        expandIdentifiers(identifiers);
    }

    /**
     * Attempts to resolve the identifiers contained in the query in
     * batches based on the parent, e.g. looking up and resolving the
     * states in the set:
     *   { [Store].[USA].[CA], [Store].[USA].[OR] }
     * together rather than individually.
     * Note that there is no guarantee that all identifiers will be
     * resolved.  Calculated members, for example, are explicitly not
     * handled here.  The purpose of this class is to improve efficiency
     * of resolution of non-calculated members, but must be followed
     * by more thorough expression resolution.
     *
     * @return  a Map of the expressions Id elements mapped to their
     * respective resolved Exp.
     */
    public Map<QueryComponent, QueryComponent> resolve() {
        return resolveInParentGroupings(identifiers);
    }

    /**
     *  Loops through the SortedSet of Ids, attempting to load sets of
     *  children of parent Ids.
     *  The loop below assumes the the SortedSet is ordered by segment
     *  size from smallest to largest, such that parent identifiers will
     *  occur before their children.
     */
    private  Map<QueryComponent, QueryComponent> resolveInParentGroupings(
        SortedSet<Id> identifiers)
    {
        final Map<QueryComponent, QueryComponent> resolvedIdentifiers =
            new HashMap<>();

        while (!identifiers.isEmpty()) {
            Id parent = identifiers.first();
            identifiers.remove(parent);

            if (!supportedIdentifier(parent)) {
                continue;
            }
            Expression exp = (Expression)resolvedIdentifiers.get(parent);
            if (exp == null) {
                exp = lookupExp(parent);
            }
            Member parentMember = getMemberFromExp(exp);
            if (!supportedMember(parentMember)) {
                continue;
            }
            resolvedIdentifiers.put(parent, (QueryComponent)exp);
            batchResolveChildren(
                parent, parentMember, identifiers, resolvedIdentifiers);
        }
        return resolvedIdentifiers;
    }

    /**
     * Find the children of Id parent in the identifiers set and resolves
     * all supported children together, adding them to the resolvedIdentifiers
     * map.
     */
    private void batchResolveChildren(
        Id parent, Member parentMember, SortedSet<Id> identifiers,
        Map<QueryComponent, QueryComponent> resolvedIdentifiers)
    {
        final List<Id> children = findChildIds(parent, identifiers);
        final List<NameSegment> childNameSegments =
            collectChildrenNameSegments(parentMember, children);

        if (!childNameSegments.isEmpty()) {
            List<Member> childMembers =
                lookupChildrenByNames(parentMember, childNameSegments);
            addChildrenToResolvedMap(
                resolvedIdentifiers, children, childMembers);
        }
    }

    private Expression lookupExp(Id parent)
    {
        try {
            Expression exp = Util.lookup(query, parent.getSegments(), false);
            return exp;
        } catch (Exception exception) {
            LOGGER.info(
                    "Failed to resolve '{}' during batch ID resolution.",
                    parent);
        }
        return null;
    }

    /**
     * Correlates each child Id we started with to it's associated
     * Member, if present.  Updates the resolvedIdentifiers map with
     * the association.
     */
    private void addChildrenToResolvedMap(
        Map<QueryComponent, QueryComponent> resolvedIdentifiers, List<Id> children,
        List<Member> childMembers)
    {
        for (Member child : childMembers) {
            for (Id childId : children) {
                if (!resolvedIdentifiers.containsKey(childId)
                    && getLastSegment(childId).matches(child.getName()))
                {
                    resolvedIdentifiers.put(
                        childId, (QueryComponent)Util.createExpr(child));
                }
            }
        }
    }

    /**
     * Performs a lookup of a set of children under parentMember.
     */
    private List<Member> lookupChildrenByNames(
        Member parentMember,
        List<NameSegment> childNameSegments)
    {
        try {
            return query.getCatalogReader(true)
                .lookupMemberChildrenByNames(
                    parentMember,
                    childNameSegments, MatchType.EXACT);
        } catch (Exception e) {
            LOGGER.info(
                String.format(
                    "Failure while looking up children of '%s' during  batch member resolution.  Child member refs:  %s",
                    parentMember,
                    Arrays.toString(childNameSegments.toArray())), e);
        }
        // don't want to fail at this point.  Member resolution still has
        // another chance to succeed.
        return List.of();
    }

    /**
     * Filters the children list to those that contain identifiers
     * we think we can batch resolve, then transforms the Id list
     * to the corresponding NameSegment.
     */
    private List<NameSegment> collectChildrenNameSegments(
        final Member parentMember, List<Id> children)
    {
		return children.stream().filter(id -> !Util.matches(parentMember, id.getSegments()) && supportedIdentifier(id))
				.map(this::getLastSegment).map(s -> (NameSegment) s).toList();
    }

    private Segment getLastSegment(Id id) {
        int segSize = id.getSegments().size();
        return id.getSegments().get(segSize - 1);
    }

    /**
     * Checks various conditions to determine whether
     * the given identifier is likely to be resolvable at this point.
     */
    private boolean supportedIdentifier(Id id) {
        Segment seg = getLastSegment(id);
        if (!(seg instanceof NameSegment)) {
            // we can't batch resolve members identified by key
            return false;
        }
        return (isPossibleMemberRef(id))
            && !segmentIsCalcMember(id.getSegments())
            && !id.getSegments().get(0).matches("Measures");
    }

    private boolean supportedMember(Member member) {
        return !(member == null
            || member.equals(
                member.getHierarchy().getNullMember())
            || member.isMeasure());
    }

    /**
     * Returns the [All] member from HierarchyExpr and DimensionExpr
     * associated with hierarchies that have an All member.
     * Returns the member associated with a MemberExpr.
     * For all other Exp returns null.
     */
    private Member getMemberFromExp(Expression exp) {
        if (exp instanceof DimensionExpression dimensionExpr) {
            Hierarchy hier = dimensionExpr
                .getDimension().getHierarchy();
            if (hier.hasAll()) {
                return hier.getAllMember();
            }
        } else if (exp instanceof HierarchyExpression hierarchyExpr) {
            Hierarchy hier = hierarchyExpr
                .getHierarchy();
            if (hier.hasAll()) {
                return hier.getAllMember();
            }
        } else if (exp instanceof MemberExpression memberExpr) {
            return memberExpr.getMember();
        }
        return null;
    }

    /**
     * Returns a collection of strings corresponding to the name
     * or uniqueName of each OlapElement in olapElements, based on the
     * flag uniqueName.
     */
    private Collection<String> getOlapElementNames(
        List<? extends OlapElement> olapElements, final boolean uniqueName)
    {

		return olapElements.stream()
				.map(olapElement -> uniqueName ? olapElement.getUniqueName() : olapElement.getName()).toList();

    }

    /**
     * Returns true if the Id is something that will potentially translate into
     * either the All/Default member of a dimension/hierarchy,
     * or a specific member.
     * This filters out references that we'd be unlikely to effectively
     * handle.
     */
    private boolean isPossibleMemberRef(Id id) {
        int size = id.getSegments().size();

        if (size == 1) {
            return segListMatchInUniqueNames(
                id.getSegments(), dimensionUniqueNames)
                || segListMatchInUniqueNames(
                    id.getSegments(), hierarchyUniqueNames);
        }
        if (size == 2)
        {
            return segListMatchInUniqueNames(
                id.getSegments(), hierarchyUniqueNames);
        }
        if (segMatchInNames(getLastSegment(id), levelNames)) {
            // conservative.  false on any match of any level name
            return false;
        }
        // don't support "shortcut" member references references
        return size > 1;
    }

    private boolean segListMatchInUniqueNames(
        List<Segment> segments, Collection<String> names)
    {
        String segUniqueName = Util.implode(segments);
        for (String name : names) {
           if (Util.equalName(segUniqueName, name)) {
               return true;
           }
        }
        return false;
    }

    private boolean segMatchInNames(
        Segment seg, Collection<String> names)
    {
        for (String name : names) {
            if (seg.matches(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean segmentIsCalcMember(final List<Segment> checkSegments) {
        return query.getCatalogReader(true)
            .getCalculatedMember(checkSegments) != null;
    }

    private List<Id> findChildIds(Id parent, SortedSet<Id> identifiers) {
        List<Id> childIds = new ArrayList<>();
        for (Id id : identifiers) {
            final List<Segment> idSeg = id.getSegments();
            final List<Segment> parentSegments = parent.getSegments();
            final int parentSegSize = parentSegments.size();
            if (idSeg.size() == parentSegSize + 1
                && parent.getSegments().equals(
                    idSeg.subList(0, parentSegSize)))
            {
                childIds.add(id);
            }
        }
        return childIds;
    }

    /**
     * Adds each parent segment to the set.
     */
    private void expandIdentifiers(Set<Id> identifiers) {
        Set<Id> expandedIdentifiers = new HashSet<>();
        for (Id id : identifiers) {
            for (int i = 1; i < id.getSegments().size(); i++) {
                expandedIdentifiers.add(new IdImpl(id.getSegments().subList(0, i)));
            }
        }
        identifiers.addAll(expandedIdentifiers);
    }

    /**
     * Sorts shorter segments first, then by string compare.
     * This allows processing parents first during the lookup loop,
     * which is required by the algorithm.
     */
    private static class IdComparator implements Comparator<Id> {
        @Override
		public int compare(Id o1, Id o2) {
            List<Segment> o1Seg = o1.getSegments();
            List<Segment> o2Seg = o2.getSegments();

            if (o1Seg.size() > o2Seg.size()) {
                return 1;
            } else if (o1Seg.size() < o2Seg.size()) {
                return -1;
            } else {
                return o1Seg.toString()
                    .compareTo(o2Seg.toString());
            }
        }
    }
}
