/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2003-2005 Julian Hyde
 * Copyright (C) 2004-2005 TONBELLER AG
 * Copyright (C) 2005-2017 Hitachi Vantara
 * Copyright (C) 2021 Sergei Semenkov
 * All Rights Reserved.
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
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.component.Expression;

/**
 * DelegatingCatalogReader implements {@link CatalogReader} by
 * delegating all methods to an underlying {@link CatalogReader}.
 *
 * It is a convenient base class if you want to override just a few of
 * {@link CatalogReader}'s methods.
 *
 * @author jhyde
 * @since Feb 26, 2003
 */
public abstract class DelegatingCatalogReader implements CatalogReader {
    protected final CatalogReader schemaReader;

    /**
     * Creates a DelegatingCatalogReader.
     *
     * @param schemaReader Parent reader to delegate unhandled calls to
     */
    protected DelegatingCatalogReader(CatalogReader schemaReader) {
        this.schemaReader = schemaReader;
    }

    @Override
	public Catalog getCatalog() {
        return schemaReader.getCatalog();
    }

    @Override
	public Role getRole() {
        return schemaReader.getRole();
    }

    @Override
	public List<Dimension> getCubeDimensions(Cube cube) {
        return schemaReader.getCubeDimensions(cube);
    }

    @Override
	public List<Hierarchy> getDimensionHierarchies(Dimension dimension) {
        return schemaReader.getDimensionHierarchies(dimension);
    }

    @Override
	public List<Member> getHierarchyRootMembers(Hierarchy hierarchy) {
        return schemaReader.getHierarchyRootMembers(hierarchy);
    }

    @Override
	public Member getMemberParent(Member member) {
        return schemaReader.getMemberParent(member);
    }

    @Override
	public Member substitute(Member member) {
        return schemaReader.substitute(member);
    }

    @Override
	public List<Member> getMemberChildren(Member member) {
        return schemaReader.getMemberChildren(member);
    }

    @Override
	public List<Member> getMemberChildren(List<Member> members) {
        return schemaReader.getMemberChildren(members);
    }

    @Override
	public void getParentChildContributingChildren(
        Member dataMember, Hierarchy hierarchy, List<Member> list)
    {
        schemaReader.getParentChildContributingChildren(
            dataMember, hierarchy, list);
    }

    @Override
	public int getMemberDepth(Member member) {
        return schemaReader.getMemberDepth(member);
    }

    @Override
	public final Member getMemberByUniqueName(
        List<Segment> uniqueNameParts,
        boolean failIfNotFound)
    {
        return getMemberByUniqueName(
            uniqueNameParts, failIfNotFound, MatchType.EXACT);
    }

    @Override
	public Member getMemberByUniqueName(
        List<Segment> uniqueNameParts,
        boolean failIfNotFound,
        MatchType matchType)
    {
        return schemaReader.getMemberByUniqueName(
            uniqueNameParts, failIfNotFound, matchType);
    }

    @Override
	public final OlapElement lookupCompound(
        OlapElement parent, List<Segment> names,
        boolean failIfNotFound, DataType category)
    {
        return lookupCompound(
            parent, names, failIfNotFound, category, MatchType.EXACT);
    }

    @Override
	public final OlapElement lookupCompound(
        OlapElement parent,
        List<Segment> names,
        boolean failIfNotFound,
        DataType category,
        MatchType matchType)
    {
            return new NameResolverImpl().resolve(
                parent,
                Util.toOlap4j(names),
                failIfNotFound,
                category,
                matchType,
                getNamespaces());
    }

    @Override
	public List<NameResolver.Namespace> getNamespaces() {
        return schemaReader.getNamespaces();
    }

    public OlapElement lookupCompoundInternal(
        OlapElement parent, List<Segment> names,
        boolean failIfNotFound, DataType category, MatchType matchType)
    {
        return schemaReader.lookupCompound(
            parent, names, failIfNotFound, category, matchType);
    }

    @Override
	public Member getCalculatedMember(List<Segment> nameParts) {
        return schemaReader.getCalculatedMember(nameParts);
    }

    @Override
	public NamedSet getNamedSet(List<Segment> nameParts) {
        return schemaReader.getNamedSet(nameParts);
    }

    @Override
	public void getMemberRange(
        Level level,
        Member startMember,
        Member endMember,
        List<Member> list)
    {
        schemaReader.getMemberRange(level, startMember, endMember, list);
    }

    @Override
	public Member getLeadMember(Member member, int n) {
        return schemaReader.getLeadMember(member, n);
    }

    @Override
	public int compareMembersHierarchically(Member m1, Member m2) {
        return schemaReader.compareMembersHierarchically(m1, m2);
    }

    @Override
	public OlapElement getElementChild(OlapElement parent, Segment name) {
        return getElementChild(parent, name, MatchType.EXACT);
    }

    @Override
	public OlapElement getElementChild(
            OlapElement parent, Segment name, MatchType matchType)
    {
        return schemaReader.getElementChild(parent, name, matchType);
    }

    @Override
	public List<Member> getLevelMembers(
            Level level, boolean includeCalculated, Evaluator context)
    {
        return schemaReader.getLevelMembers(level, includeCalculated, context);
    }

    @Override
	public List<Member> getLevelMembers(
        Level level, boolean includeCalculated)
    {
        return getLevelMembers(level, includeCalculated, null);
    }

    @Override
	public List<Level> getHierarchyLevels(Hierarchy hierarchy) {
        return schemaReader.getHierarchyLevels(hierarchy);
    }

    @Override
	public Member getHierarchyDefaultMember(Hierarchy hierarchy) {
        return schemaReader.getHierarchyDefaultMember(hierarchy);
    }

    @Override
	public boolean isDrillable(Member member) {
        return schemaReader.isDrillable(member);
    }

    @Override
	public boolean isVisible(Member member) {
        return schemaReader.isVisible(member);
    }

    @Override
	public List<Cube> getCubes() {
        return schemaReader.getCubes();
    }

    @Override
	public List<Member> getCalculatedMembers(Hierarchy hierarchy) {
        return schemaReader.getCalculatedMembers(hierarchy);
    }

    @Override
	public List<Member> getCalculatedMembers(Level level) {
        return schemaReader.getCalculatedMembers(level);
    }

    @Override
	public List<Member> getCalculatedMembers() {
        return schemaReader.getCalculatedMembers();
    }

    @Override
	public int getChildrenCountFromCache(Member member) {
        return schemaReader.getChildrenCountFromCache(member);
    }

    @Override
	public int getLevelCardinality(
        Level level, boolean approximate, boolean materialize)
    {
        return schemaReader.getLevelCardinality(
            level, approximate, materialize);
    }

    @Override
	public List<Member> getLevelMembers(Level level, Evaluator context) {
        return schemaReader.getLevelMembers(level, context);
    }

    @Override
	public List<Member> getMemberChildren(Member member, Evaluator context) {
        return schemaReader.getMemberChildren(member, context);
    }

    @Override
	public List<Member> getMemberChildren(
        List<Member> members, Evaluator context)
    {
        return schemaReader.getMemberChildren(members, context);
    }

    @Override
	public void getMemberAncestors(Member member, List<Member> ancestorList) {
        schemaReader.getMemberAncestors(member, ancestorList);
    }

    @Override
	public Member lookupMemberChildByName(
            Member member, Segment memberName, MatchType matchType)
    {
        return schemaReader.lookupMemberChildByName(
            member, memberName, matchType);
    }

    @Override
	public List<Member> lookupMemberChildrenByNames(
        Member parent, List<NameSegment> childNames, MatchType matchType)
    {
        return schemaReader.lookupMemberChildrenByNames(
            parent, childNames, matchType);
    }

    @Override
	public NativeEvaluator getNativeSetEvaluator(
        FunctionDefinition fun, Expression[] args, Evaluator evaluator, Calc calc)
    {
        return schemaReader.getNativeSetEvaluator(fun, args, evaluator, calc);
    }

    @Override
	public Parameter getParameter(String name) {
        return schemaReader.getParameter(name);
    }

    @Override
	public CatalogReader withoutAccessControl() {
        return schemaReader.withoutAccessControl();
    }

    @Override
	public CatalogReader withLocus() {
        return Util.executionCatalogReader(
            schemaReader.getCatalog().getInternalConnection(),
            this);
    }

    @Override
	public Map<? extends Member, AccessMember> getMemberChildrenWithDetails(
        Member member,
        Evaluator evaluator)
    {
        return schemaReader.getMemberChildrenWithDetails(member, evaluator);
    }
    @Override
    public Context getContext() {
        return schemaReader.getContext();
    }

    @Override
    public List<? extends DatabaseSchema> getDatabaseSchemas() {
        return schemaReader.getDatabaseSchemas();
    }
}
