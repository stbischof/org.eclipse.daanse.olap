/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.execution;

import java.util.List;
import java.util.Map;
import java.util.Objects;


import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.NameResolver;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.access.AccessMember;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.agg.Segment;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
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
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.execution.ExecutionContext;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.component.Expression;

/**
 * Decorator for {@link CatalogReader} that wraps each method call with ExecutionContext.
 *
 * <p>This class replaces the InvocationHandler-based Proxy approach to provide
 * better type safety, debuggability, and performance. Each method call is
 * automatically wrapped with {@link ExecutionContext#where} to ensure proper
 * execution context propagation using ScopedValues.
 *
 * <p>Usage:
 * <pre>
 * CatalogReader reader = new ExecutionCatalogReaderWrapper(
 *     execution, "Schema reader", delegateReader);
 * </pre>
 *
 * @see org.eclipse.daanse.olap.common.Util#executionCatalogReader
 */
public class ExecutionCatalogReaderWrapper implements CatalogReader {

    private final Execution execution;
    private final String component;
    private final CatalogReader delegate;

    /**
     * Creates a wrapper that adds ExecutionContext to all CatalogReader operations.
     *
     * @param execution Execution context
     * @param component Component name for profiling (e.g., "Schema reader")
     * @param delegate The actual CatalogReader to delegate to
     * @throws NullPointerException if any parameter is null
     */
    public ExecutionCatalogReaderWrapper(
            Execution execution,
            String component,
            CatalogReader delegate) {
        this.execution = Objects.requireNonNull(execution, "execution");
        this.component = Objects.requireNonNull(component, "component");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public Catalog getCatalog() {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getCatalog());
    }

    @Override
    public Role getRole() {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getRole());
    }

    @Override
    public List<Dimension> getCubeDimensions(Cube cube) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getCubeDimensions(cube));
    }

    @Override
    public List<Hierarchy> getDimensionHierarchies(Dimension dimension) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getDimensionHierarchies(dimension));
    }

    @Override
    public List<Member> getHierarchyRootMembers(Hierarchy hierarchy) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getHierarchyRootMembers(hierarchy));
    }

    @Override
    public int getChildrenCountFromCache(Member member) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getChildrenCountFromCache(member));
    }

    @Override
    public int getLevelCardinality(Level level, boolean approximate, boolean materialize) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getLevelCardinality(level, approximate, materialize));
    }

    @Override
    public Member substitute(Member member) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.substitute(member));
    }

    @Override
    public List<Member> getMemberChildren(Member member) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getMemberChildren(member));
    }

    @Override
    public List<Member> getMemberChildren(Member member, Evaluator context) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getMemberChildren(member, context));
    }

    @Override
    public List<Member> getMemberChildren(List<Member> members) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getMemberChildren(members));
    }

    @Override
    public List<Member> getMemberChildren(List<Member> members, Evaluator context) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getMemberChildren(members, context));
    }

    @Override
    public void getParentChildContributingChildren(Member dataMember, Hierarchy hierarchy, List<Member> list) {
        ExecutionContext.where(execution.asContext(), () -> {
            delegate.getParentChildContributingChildren(dataMember, hierarchy, list);
            return null;
        });
    }

    @Override
    public Member getMemberParent(Member member) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getMemberParent(member));
    }

    @Override
    public void getMemberAncestors(Member member, List<Member> ancestorList) {
        ExecutionContext.where(execution.asContext(), () -> {
            delegate.getMemberAncestors(member, ancestorList);
            return null;
        });
    }

    @Override
    public int getMemberDepth(Member member) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getMemberDepth(member));
    }

    @Override
    public Member getMemberByUniqueName(List<Segment> uniqueNameParts, boolean failIfNotFound, MatchType matchType) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getMemberByUniqueName(uniqueNameParts, failIfNotFound, matchType));
    }

    @Override
    public Member getMemberByUniqueName(List<Segment> uniqueNameParts, boolean failIfNotFound) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getMemberByUniqueName(uniqueNameParts, failIfNotFound));
    }

    @Override
    public OlapElement lookupCompound(OlapElement parent, List<Segment> names, boolean failIfNotFound, DataType category, MatchType matchType) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.lookupCompound(parent, names, failIfNotFound, category, matchType));
    }

    @Override
    public OlapElement lookupCompound(OlapElement parent, List<Segment> names, boolean failIfNotFound, DataType category) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.lookupCompound(parent, names, failIfNotFound, category));
    }

    @Override
    public Member getCalculatedMember(List<Segment> nameParts) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getCalculatedMember(nameParts));
    }

    @Override
    public NamedSet getNamedSet(List<Segment> nameParts) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getNamedSet(nameParts));
    }

    @Override
    public void getMemberRange(Level level, Member startMember, Member endMember, List<Member> list) {
        ExecutionContext.where(execution.asContext(), () -> {
            delegate.getMemberRange(level, startMember, endMember, list);
            return null;
        });
    }

    @Override
    public Member getLeadMember(Member member, int n) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getLeadMember(member, n));
    }

    @Override
    public int compareMembersHierarchically(Member m1, Member m2) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.compareMembersHierarchically(m1, m2));
    }

    @Override
    public OlapElement getElementChild(OlapElement parent, Segment name, MatchType matchType) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getElementChild(parent, name, matchType));
    }

    @Override
    public OlapElement getElementChild(OlapElement parent, Segment name) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getElementChild(parent, name));
    }

    @Override
    public List<Member> getLevelMembers(Level level, boolean includeCalculated) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getLevelMembers(level, includeCalculated));
    }

    @Override
    public List<Member> getLevelMembers(Level level, boolean includeCalculated, Evaluator context) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getLevelMembers(level, includeCalculated, context));
    }

    @Override
    public List<Member> getLevelMembers(Level level, Evaluator context) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getLevelMembers(level, context));
    }

    @Override
    public List<Level> getHierarchyLevels(Hierarchy hierarchy) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getHierarchyLevels(hierarchy));
    }

    @Override
    public Member getHierarchyDefaultMember(Hierarchy hierarchy) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getHierarchyDefaultMember(hierarchy));
    }

    @Override
    public boolean isDrillable(Member member) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.isDrillable(member));
    }

    @Override
    public boolean isVisible(Member member) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.isVisible(member));
    }

    @Override
    public List<Cube> getCubes() {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getCubes());
    }

    @Override
    public List<Member> getCalculatedMembers(Hierarchy hierarchy) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getCalculatedMembers(hierarchy));
    }

    @Override
    public List<Member> getCalculatedMembers(Level level) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getCalculatedMembers(level));
    }

    @Override
    public List<Member> getCalculatedMembers() {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getCalculatedMembers());
    }

    @Override
    public Member lookupMemberChildByName(Member parent, Segment childName, MatchType matchType) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.lookupMemberChildByName(parent, childName, matchType));
    }

    @Override
    public List<Member> lookupMemberChildrenByNames(Member parent, List<NameSegment> childNames, MatchType matchType) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.lookupMemberChildrenByNames(parent, childNames, matchType));
    }

    @Override
    public NativeEvaluator getNativeSetEvaluator(FunctionDefinition fun, Expression[] args, Evaluator evaluator, Calc calc) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getNativeSetEvaluator(fun, args, evaluator, calc));
    }

    @Override
    public Parameter getParameter(String name) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getParameter(name));
    }

    @Override
    public CatalogReader withoutAccessControl() {
        return ExecutionContext.where(execution.asContext(), () -> delegate.withoutAccessControl());
    }

    @Override
    public CatalogReader withLocus() {
        return ExecutionContext.where(execution.asContext(), () -> delegate.withLocus());
    }

    @Override
    public List<NameResolver.Namespace> getNamespaces() {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getNamespaces());
    }

    @Override
    public Map<? extends Member, AccessMember> getMemberChildrenWithDetails(Member member, Evaluator evaluator) {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getMemberChildrenWithDetails(member, evaluator));
    }

    @Override
    public Context<?> getContext() {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getContext());
    }

    @Override
    public List<? extends DatabaseSchema> getDatabaseSchemas() {
        return ExecutionContext.where(execution.asContext(), () -> delegate.getDatabaseSchemas());
    }
}
