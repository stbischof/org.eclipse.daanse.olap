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
package org.eclipse.daanse.olap.provider.api;

/**
 * Implements every method of {@link org.eclipse.daanse.olap.api.catalog.CatalogReader} by refusing it.
 *
 * <p>A subclass overrides only what it can answer. Anything else throws an
 * {@link UnsupportedOperationException} naming the method, so a failing test says
 * whether the provider is incomplete or the caller is wrong. It never returns
 * {@code null}, which would hide exactly the defects these tests look for.
 */
public abstract class AbstractRefusingCatalogReader implements org.eclipse.daanse.olap.api.catalog.CatalogReader {

    @Override
    public int compareMembersHierarchically(org.eclipse.daanse.olap.api.element.Member a0, org.eclipse.daanse.olap.api.element.Member a1) {
        throw refuse("compareMembersHierarchically");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Member getCalculatedMember(java.util.List<org.eclipse.daanse.olap.api.agg.Segment> a0) {
        throw refuse("getCalculatedMember");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getCalculatedMembers() {
        throw refuse("getCalculatedMembers");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getCalculatedMembers(org.eclipse.daanse.olap.api.element.Level a0) {
        throw refuse("getCalculatedMembers");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getCalculatedMembers(org.eclipse.daanse.olap.api.element.Hierarchy a0) {
        throw refuse("getCalculatedMembers");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Catalog getCatalog() {
        throw refuse("getCatalog");
    }

    @Override
    public int getChildrenCountFromCache(org.eclipse.daanse.olap.api.element.Member a0) {
        throw refuse("getChildrenCountFromCache");
    }

    @Override
    public org.eclipse.daanse.olap.api.Context<?> getContext() {
        throw refuse("getContext");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Dimension> getCubeDimensions(org.eclipse.daanse.olap.api.element.Cube a0) {
        throw refuse("getCubeDimensions");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Cube> getCubes() {
        throw refuse("getCubes");
    }

    @Override
    public java.util.List<? extends org.eclipse.daanse.olap.api.element.db.DatabaseSchema> getDatabaseSchemas() {
        throw refuse("getDatabaseSchemas");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Hierarchy> getDimensionHierarchies(org.eclipse.daanse.olap.api.element.Dimension a0) {
        throw refuse("getDimensionHierarchies");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.OlapElement getElementChild(org.eclipse.daanse.olap.api.element.OlapElement a0, org.eclipse.daanse.olap.api.agg.Segment a1) {
        throw refuse("getElementChild");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.OlapElement getElementChild(org.eclipse.daanse.olap.api.element.OlapElement a0, org.eclipse.daanse.olap.api.agg.Segment a1, org.eclipse.daanse.olap.api.element.MatchType a2) {
        throw refuse("getElementChild");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Member getHierarchyDefaultMember(org.eclipse.daanse.olap.api.element.Hierarchy a0) {
        throw refuse("getHierarchyDefaultMember");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Level> getHierarchyLevels(org.eclipse.daanse.olap.api.element.Hierarchy a0) {
        throw refuse("getHierarchyLevels");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getHierarchyRootMembers(org.eclipse.daanse.olap.api.element.Hierarchy a0) {
        throw refuse("getHierarchyRootMembers");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Member getLeadMember(org.eclipse.daanse.olap.api.element.Member a0, int a1) {
        throw refuse("getLeadMember");
    }

    @Override
    public int getLevelCardinality(org.eclipse.daanse.olap.api.element.Level a0, boolean a1, boolean a2) {
        throw refuse("getLevelCardinality");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getLevelMembers(org.eclipse.daanse.olap.api.element.Level a0, boolean a1) {
        throw refuse("getLevelMembers");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getLevelMembers(org.eclipse.daanse.olap.api.element.Level a0, org.eclipse.daanse.olap.api.evaluator.Evaluator a1) {
        throw refuse("getLevelMembers");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getLevelMembers(org.eclipse.daanse.olap.api.element.Level a0, boolean a1, org.eclipse.daanse.olap.api.evaluator.Evaluator a2) {
        throw refuse("getLevelMembers");
    }

    @Override
    public void getMemberAncestors(org.eclipse.daanse.olap.api.element.Member a0, java.util.List<org.eclipse.daanse.olap.api.element.Member> a1) {
        throw refuse("getMemberAncestors");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Member getMemberByUniqueName(java.util.List<org.eclipse.daanse.olap.api.agg.Segment> a0, boolean a1) {
        throw refuse("getMemberByUniqueName");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Member getMemberByUniqueName(java.util.List<org.eclipse.daanse.olap.api.agg.Segment> a0, boolean a1, org.eclipse.daanse.olap.api.element.MatchType a2) {
        throw refuse("getMemberByUniqueName");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getMemberChildren(org.eclipse.daanse.olap.api.element.Member a0) {
        throw refuse("getMemberChildren");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getMemberChildren(java.util.List<org.eclipse.daanse.olap.api.element.Member> a0) {
        throw refuse("getMemberChildren");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getMemberChildren(org.eclipse.daanse.olap.api.element.Member a0, org.eclipse.daanse.olap.api.evaluator.Evaluator a1) {
        throw refuse("getMemberChildren");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> getMemberChildren(java.util.List<org.eclipse.daanse.olap.api.element.Member> a0, org.eclipse.daanse.olap.api.evaluator.Evaluator a1) {
        throw refuse("getMemberChildren");
    }

    @Override
    public java.util.Map<? extends org.eclipse.daanse.olap.api.element.Member, org.eclipse.daanse.olap.api.access.AccessMember> getMemberChildrenWithDetails(org.eclipse.daanse.olap.api.element.Member a0, org.eclipse.daanse.olap.api.evaluator.Evaluator a1) {
        throw refuse("getMemberChildrenWithDetails");
    }

    @Override
    public int getMemberDepth(org.eclipse.daanse.olap.api.element.Member a0) {
        throw refuse("getMemberDepth");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Member getMemberParent(org.eclipse.daanse.olap.api.element.Member a0) {
        throw refuse("getMemberParent");
    }

    @Override
    public void getMemberRange(org.eclipse.daanse.olap.api.element.Level a0, org.eclipse.daanse.olap.api.element.Member a1, org.eclipse.daanse.olap.api.element.Member a2, java.util.List<org.eclipse.daanse.olap.api.element.Member> a3) {
        throw refuse("getMemberRange");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.NamedSet getNamedSet(java.util.List<org.eclipse.daanse.olap.api.agg.Segment> a0) {
        throw refuse("getNamedSet");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.NameResolver.Namespace> getNamespaces() {
        throw refuse("getNamespaces");
    }

    @Override
    public org.eclipse.daanse.olap.api.evaluator.NativeEvaluator getNativeSetEvaluator(org.eclipse.daanse.olap.api.function.FunctionDefinition a0, org.eclipse.daanse.olap.api.query.component.Expression[] a1, org.eclipse.daanse.olap.api.evaluator.Evaluator a2, org.eclipse.daanse.olap.api.calc.Calc a3) {
        throw refuse("getNativeSetEvaluator");
    }

    @Override
    public org.eclipse.daanse.olap.api.Parameter getParameter(java.lang.String a0) {
        throw refuse("getParameter");
    }

    @Override
    public void getParentChildContributingChildren(org.eclipse.daanse.olap.api.element.Member a0, org.eclipse.daanse.olap.api.element.Hierarchy a1, java.util.List<org.eclipse.daanse.olap.api.element.Member> a2) {
        throw refuse("getParentChildContributingChildren");
    }

    @Override
    public org.eclipse.daanse.olap.api.access.Role getRole() {
        throw refuse("getRole");
    }

    @Override
    public boolean isDrillable(org.eclipse.daanse.olap.api.element.Member a0) {
        throw refuse("isDrillable");
    }

    @Override
    public boolean isVisible(org.eclipse.daanse.olap.api.element.Member a0) {
        throw refuse("isVisible");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.OlapElement lookupCompound(org.eclipse.daanse.olap.api.element.OlapElement a0, java.util.List<org.eclipse.daanse.olap.api.agg.Segment> a1, boolean a2, org.eclipse.daanse.olap.api.DataType a3) {
        throw refuse("lookupCompound");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.OlapElement lookupCompound(org.eclipse.daanse.olap.api.element.OlapElement a0, java.util.List<org.eclipse.daanse.olap.api.agg.Segment> a1, boolean a2, org.eclipse.daanse.olap.api.DataType a3, org.eclipse.daanse.olap.api.element.MatchType a4) {
        throw refuse("lookupCompound");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Member lookupMemberChildByName(org.eclipse.daanse.olap.api.element.Member a0, org.eclipse.daanse.olap.api.agg.Segment a1, org.eclipse.daanse.olap.api.element.MatchType a2) {
        throw refuse("lookupMemberChildByName");
    }

    @Override
    public java.util.List<org.eclipse.daanse.olap.api.element.Member> lookupMemberChildrenByNames(org.eclipse.daanse.olap.api.element.Member a0, java.util.List<org.eclipse.daanse.olap.api.query.NameSegment> a1, org.eclipse.daanse.olap.api.element.MatchType a2) {
        throw refuse("lookupMemberChildrenByNames");
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Member substitute(org.eclipse.daanse.olap.api.element.Member a0) {
        throw refuse("substitute");
    }

    @Override
    public org.eclipse.daanse.olap.api.catalog.CatalogReader withLocus() {
        throw refuse("withLocus");
    }

    @Override
    public org.eclipse.daanse.olap.api.catalog.CatalogReader withoutAccessControl() {
        throw refuse("withoutAccessControl");
    }

    /** The one place that says no, so every refusal reads the same. */
    protected UnsupportedOperationException refuse(String method) {
        return new UnsupportedOperationException(
                getClass().getSimpleName() + "." + method + " is not implemented by the provider");
    }
}
