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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.NameResolver;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.provider.api.AbstractRefusingCatalogReader;

/**
 * Answers navigation questions by walking the element tree the catalog holds.
 *
 * <p>Every method here is pure navigation: children, parents, level members, name lookup.
 * Nothing consults a backend, because the backend is the tree itself. What this reader
 * does not implement it refuses by name rather than answering {@code null}.
 */
public class MemoryCatalogReader extends AbstractRefusingCatalogReader
        implements NameResolver.Namespace {

    private final MemoryCatalog catalog;
    private final Role role;
    private final Context<?> context;

    MemoryCatalogReader(MemoryCatalog catalog, Role role, Context<?> context) {
        this.catalog = catalog;
        this.role = role;
        this.context = context;
    }

    // ---- identity -------------------------------------------------------------

    @Override
    public Catalog getCatalog() {
        return catalog;
    }

    @Override
    public Context<?> getContext() {
        return context;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public CatalogReader withLocus() {
        return this;
    }

    @Override
    public CatalogReader withoutAccessControl() {
        return this;
    }

    /**
     * This reader is its own namespace: a name is resolved by walking the same tree it
     * navigates, so there is nothing else to consult.
     */
    @Override
    public List<NameResolver.Namespace> getNamespaces() {
        return List.of(this);
    }

    @Override
    public OlapElement lookupCompound(OlapElement parent, List<Segment> names,
            boolean failIfNotFound, org.eclipse.daanse.olap.api.DataType category) {
        return lookupCompound(parent, names, failIfNotFound, category, MatchType.EXACT);
    }

    @Override
    public OlapElement lookupCompound(OlapElement parent, List<Segment> names,
            boolean failIfNotFound, org.eclipse.daanse.olap.api.DataType category,
            MatchType matchType) {
        return new org.eclipse.daanse.olap.common.NameResolverImpl().resolve(parent,
                org.eclipse.daanse.olap.common.Util.toOlap4j(names), failIfNotFound, category,
                matchType, getNamespaces());
    }

    @Override
    public OlapElement lookupChild(OlapElement parent,
            org.eclipse.daanse.olap.api.query.IdentifierSegment segment) {
        return lookupChild(parent, segment, MatchType.EXACT);
    }

    @Override
    public OlapElement lookupChild(OlapElement parent,
            org.eclipse.daanse.olap.api.query.IdentifierSegment segment, MatchType matchType) {
        return parent.lookupChild(this, org.eclipse.daanse.olap.common.Util.convert(segment), matchType);
    }

    // ---- structure ------------------------------------------------------------

    @Override
    public List<Cube> getCubes() {
        return catalog.getCubes();
    }

    @Override
    public List<Dimension> getCubeDimensions(Cube cube) {
        return List.copyOf(cube.getDimensions());
    }

    @Override
    public List<Hierarchy> getDimensionHierarchies(Dimension dimension) {
        return List.copyOf(dimension.getHierarchies());
    }

    @Override
    public List<Level> getHierarchyLevels(Hierarchy hierarchy) {
        return List.copyOf(hierarchy.getLevels());
    }

    // ---- members --------------------------------------------------------------

    @Override
    public List<Member> getHierarchyRootMembers(Hierarchy hierarchy) {
        return hierarchy.getRootMembers();
    }

    @Override
    public Member getHierarchyDefaultMember(Hierarchy hierarchy) {
        return hierarchy.getDefaultMember();
    }

    @Override
    public List<Member> getLevelMembers(Level level, boolean includeCalculated) {
        return level.getMembers();
    }

    @Override
    public List<Member> getLevelMembers(Level level, Evaluator evaluator) {
        return level.getMembers();
    }

    @Override
    public List<Member> getLevelMembers(Level level, boolean includeCalculated, Evaluator evaluator) {
        return level.getMembers();
    }

    @Override
    public int getLevelCardinality(Level level, boolean approximate, boolean materialize) {
        return level.getMembers().size();
    }

    @Override
    public List<Member> getMemberChildren(Member member) {
        return member instanceof MemoryMember m ? List.copyOf(m.childMembers()) : List.of();
    }

    @Override
    public List<Member> getMemberChildren(Member member, Evaluator evaluator) {
        return getMemberChildren(member);
    }

    @Override
    public List<Member> getMemberChildren(List<Member> members) {
        List<Member> all = new ArrayList<>();
        for (Member m : members) {
            all.addAll(getMemberChildren(m));
        }
        return all;
    }

    @Override
    public List<Member> getMemberChildren(List<Member> members, Evaluator evaluator) {
        return getMemberChildren(members);
    }

    @Override
    public Member getMemberParent(Member member) {
        return member.getParentMember();
    }

    @Override
    public int getMemberDepth(Member member) {
        return member.getDepth();
    }

    /**
     * Every member above this one, nearest first.
     *
     * <p>Walked here rather than asked of the member: a member answers that question by
     * asking its catalogue reader, so delegating back to it is an infinite recursion that
     * ends as a stack overflow rather than as an answer.
     */
    @Override
    public void getMemberAncestors(Member member, List<Member> ancestors) {
        for (Member parent = member.getParentMember(); parent != null;
                parent = parent.getParentMember()) {
            ancestors.add(parent);
        }
    }

    @Override
    public Member getLeadMember(Member member, int n) {
        if (n == 0) {
            return member;
        }
        List<Member> siblings = member.getLevel().getMembers();
        int target = siblings.indexOf(member) + n;
        return target < 0 || target >= siblings.size()
                ? member.getHierarchy().getNullMember()
                : siblings.get(target);
    }

    @Override
    public int compareMembersHierarchically(Member m1, Member m2) {
        if (m1 == m2) {
            return 0;
        }
        List<Member> path1 = pathFromRoot(m1);
        List<Member> path2 = pathFromRoot(m2);
        int common = Math.min(path1.size(), path2.size());
        for (int i = 0; i < common; i++) {
            Member a = path1.get(i);
            Member b = path2.get(i);
            if (a != b) {
                List<Member> siblings = i == 0 ? getHierarchyRootMembers(a.getHierarchy())
                        : getMemberChildren(path1.get(i - 1));
                return Integer.compare(siblings.indexOf(a), siblings.indexOf(b));
            }
        }
        // one is an ancestor of the other: the ancestor comes first
        return Integer.compare(path1.size(), path2.size());
    }

    private List<Member> pathFromRoot(Member member) {
        List<Member> path = new ArrayList<>();
        for (Member m = member; m != null; m = getMemberParent(m)) {
            path.add(0, m);
        }
        return path;
    }

    /** Every member is in memory, so the count is always known. */
    @Override
    public int getChildrenCountFromCache(Member member) {
        return getMemberChildren(member).size();
    }

    /** Without roles every child is fully accessible. */
    @Override
    public java.util.Map<? extends Member, org.eclipse.daanse.olap.api.access.AccessMember> getMemberChildrenWithDetails(
            Member member, Evaluator evaluator) {
        java.util.Map<Member, org.eclipse.daanse.olap.api.access.AccessMember> children = new java.util.LinkedHashMap<>();
        for (Member child : getMemberChildren(member, evaluator)) {
            children.put(child, org.eclipse.daanse.olap.api.access.AccessMember.ALL);
        }
        return children;
    }

    @Override
    public List<Member> lookupMemberChildrenByNames(Member parent,
            List<org.eclipse.daanse.olap.api.query.NameSegment> names, MatchType matchType) {
        List<Member> found = new ArrayList<>();
        for (org.eclipse.daanse.olap.api.query.NameSegment name : names) {
            Member child = lookupMemberChildByName(parent, name, matchType);
            if (child != null) {
                found.add(child);
            }
        }
        return found;
    }

    @Override
    public Member getMemberByUniqueName(List<Segment> uniqueNameParts, boolean failIfNotFound) {
        return getMemberByUniqueName(uniqueNameParts, failIfNotFound, MatchType.EXACT);
    }

    @Override
    public Member getMemberByUniqueName(List<Segment> uniqueNameParts, boolean failIfNotFound, MatchType matchType) {
        for (Cube cube : getCubes()) {
            OlapElement element = lookupCompound(cube, uniqueNameParts, false,
                    org.eclipse.daanse.olap.api.DataType.MEMBER, matchType);
            if (element instanceof Member member) {
                return member;
            }
        }
        if (failIfNotFound) {
            throw new org.eclipse.daanse.olap.api.exception.OlapRuntimeException(
                    "Member not found: " + org.eclipse.daanse.olap.common.Util.implode(uniqueNameParts));
        }
        return null;
    }

    /** The cubes of this provider define no calculated members, so none is found. */
    @Override
    public Member getCalculatedMember(List<Segment> nameParts) {
        return null;
    }

    /** The cubes of this provider define no named sets, so none is found. */
    @Override
    public org.eclipse.daanse.olap.api.element.NamedSet getNamedSet(List<Segment> nameParts) {
        return null;
    }

    @Override
    public Member substitute(Member member) {
        return member;
    }

    @Override
    public boolean isVisible(Member member) {
        return member != null && member.isVisible();
    }

    @Override
    public boolean isDrillable(Member member) {
        return !getMemberChildren(member).isEmpty();
    }

    @Override
    public List<Member> getCalculatedMembers() {
        return List.of();
    }

    @Override
    public List<Member> getCalculatedMembers(Level level) {
        return List.of();
    }

    @Override
    public List<Member> getCalculatedMembers(Hierarchy hierarchy) {
        return List.of();
    }

    // ---- name lookup ----------------------------------------------------------

    @Override
    public Member lookupMemberChildByName(Member parent, Segment segment, MatchType matchType) {
        for (Member child : getMemberChildren(parent)) {
            if (segment.matches(child.getName())) {
                return child;
            }
        }
        return null;
    }

    @Override
    public OlapElement getElementChild(OlapElement parent, Segment segment) {
        return getElementChild(parent, segment, MatchType.EXACT);
    }

    @Override
    public OlapElement getElementChild(OlapElement parent, Segment segment, MatchType matchType) {
        return parent.lookupChild(this, segment, matchType);
    }

    /**
     * {@code null}: there is no native path here. A provider that cannot push a set
     * expression down to a backend says so by declining, and the engine then evaluates the
     * expression itself, which is the whole point of this provider.
     */
    @Override
    public org.eclipse.daanse.olap.api.evaluator.NativeEvaluator getNativeSetEvaluator(
            org.eclipse.daanse.olap.api.function.FunctionDefinition function,
            org.eclipse.daanse.olap.api.query.component.Expression[] args,
            Evaluator evaluator, org.eclipse.daanse.olap.api.calc.Calc calc) {
        return null;
    }

    /**
     * {@code null}: this provider defines no catalog parameters. A query may still declare
     * its own; those never reach here.
     */
    @Override
    public org.eclipse.daanse.olap.api.Parameter getParameter(String name) {
        return null;
    }

    /**
     * The members of a level between two bounds, inclusive, in declaration order.
     *
     * <p>An empty range when the bounds are the wrong way round, which is what a range
     * expression like {@code [a]:[b]} expects rather than an error.
     */
    @Override
    public void getMemberRange(Level level, Member startMember, Member endMember,
            List<Member> list) {
        List<Member> members = level.getMembers();
        int from = members.indexOf(startMember);
        int to = members.indexOf(endMember);
        if (from < 0 || to < 0 || from > to) {
            return;
        }
        list.addAll(members.subList(from, to + 1));
    }

}
