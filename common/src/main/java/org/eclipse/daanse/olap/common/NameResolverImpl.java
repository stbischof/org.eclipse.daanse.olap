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

import java.util.List;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.NameResolver;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.query.IdentifierSegment;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.query.IdentifierNode;

/**
 * Resolves a list of segments (a parsed identifier) to an OLAP element.
 */
public final class NameResolverImpl implements NameResolver {

    /**
     * Creates a NameResolver.
     */
    public NameResolverImpl() {
    }

    /**
     * Looks a name up as a reader of a catalog does: the reader says which namespaces are in force, and the
     * segments are read the way a client wrote them. Every reader that resolves a whole name this way asks
     * here, so the walk itself is written once.
     *
     * @param reader         the reader whose namespaces are in force
     * @param parent         the element the name starts from, or null for the catalog itself
     * @param names          the name, segment by segment
     * @param failIfNotFound whether a name that resolves to nothing is an error
     * @param category       what kind of element is looked for
     * @param matchType      how closely the name has to match
     * @return the element the name stands for, or null when there is none and failing is not asked for
     */
    public static OlapElement lookupCompound(CatalogReader reader, OlapElement parent,
            List<org.eclipse.daanse.olap.api.query.Segment> names, boolean failIfNotFound, DataType category,
            MatchType matchType) {
        return new NameResolverImpl().resolve(parent, Util.toOlap4j(names), failIfNotFound, category,
                matchType, reader.getNamespaces());
    }

    /**
     * Resolves a list of segments (a parsed identifier) to an OLAP element.
     *
     * @param parent Parent element to search in, usually a cube
     * @param segments Exploded compound name, such as {"Products",
     *   "Product Department", "Produce"}
     * @param failIfNotFound If the element is not found, determines whether
     *   to return null or throw an error
     * @param category Type of returned element, a {@link DataType} value;
     *   {@link DataType#UNKNOWN} if it doesn't matter.
     * @param matchType Match type
     * @param namespaces Namespaces wherein to find child element at each step
     * @return OLAP element with given name, or null if not found
     */
    @Override
	public OlapElement resolve(
        OlapElement parent,
        List<IdentifierSegment> segments,
        boolean failIfNotFound,
        DataType category,
        MatchType matchType,
        List<Namespace> namespaces)
    {
        OlapElement element;
        if (matchType == MatchType.EXACT) {
            element = resolveExact(
                parent,
                segments,
                namespaces);
        } else {
            element = resolveInexact(
                parent,
                segments,
                matchType,
                namespaces);
        }
        if (element != null) {
            element = nullify(category, element);
        }
        if (element == null && failIfNotFound) {
            throw Util.newElementNotFoundException(
                category,
                new IdentifierNode(segments));
        }
        return element;
    }

    private OlapElement resolveInexact(
        OlapElement parent,
        List<IdentifierSegment> segments,
        MatchType matchType,
        List<Namespace> namespaces)
    {
        OlapElement element = parent;
        for (final IdentifierSegment segment : segments) {
            assert element != null;
            OlapElement child = null;
            for (Namespace namespace : namespaces) {
                child = namespace.lookupChild(element, segment, matchType);
                if (child != null) {
                    switch (matchType) {
                    case EXACT:
                    case EXACT_SCHEMA:
                        break;
                    case BEFORE:
                        if (!Util.matches(segment, child.getName())) {
                            matchType = MatchType.LAST;
                        }
                        break;
                    case AFTER:
                        if (!Util.matches(segment, child.getName())) {
                            matchType = MatchType.FIRST;
                        }
                        break;
                    }
                    break;
                }
            }
            if (child == null) {
                return null;
            }
            element = child;
        }
        return element;
    }

    // same logic as resolveInexact, pared down for common case
    // matchType == EXACT
    private OlapElement resolveExact(
        OlapElement parent,
        List<IdentifierSegment> segments,
        List<Namespace> namespaces)
    {
        OlapElement element = parent;
        for (final IdentifierSegment segment : segments) {
            assert element != null;
            OlapElement child = null;
            for (Namespace namespace : namespaces) {
                child = namespace.lookupChild(element, segment);
                if (child != null) {
                    break;
                }
            }
            if (child == null) {
                return null;
            }
            element = child;
        }
        return element;
    }

    /**
     * Converts an element to the required type, converting if possible,
     * returning null if it is not of the required type and cannot be converted.
     *
     * @param category Desired category of element
     * @param element Element
     * @return Element of the desired category, or null
     */
    private OlapElement nullify(DataType category, OlapElement element) {
        switch (category) {
        case UNKNOWN:
            return element;
        case MEMBER:
            return element instanceof Member ? element : null;
        case LEVEL:
            return element instanceof Level ? element : null;
        case HIERARCHY:
            if (element instanceof Hierarchy) {
                return element;
            } else if (element instanceof Dimension dimension) {
                final List<? extends Hierarchy> hierarchies = dimension.getHierarchies();
                if (hierarchies.size() == 1) {
                    return hierarchies.getFirst();
                }
                return null;
            } else {
                return null;
            }
        case DIMENSION:
            return element instanceof Dimension ? element : null;
        case SET:
            return element instanceof NamedSet ? element : null;
        default:
            throw Util.newInternal("unexpected: " + category);
        }
    }

    /**
     * Returns whether a formula (representing a calculated member or named
     * set) matches a given parent and name segment.
     *
     * @param formula Formula
     * @param parent Parent element
     * @param segment Name segment
     * @return Whether formula matches
     */
    public static boolean matches(
        Formula formula,
        OlapElement parent,
        IdentifierSegment segment)
    {
        if (!Util.matches(segment, formula.getName())) {
            return false;
        }
        if (formula.isMember()) {
            final Member formulaMember = formula.getMdxMember();
            if (formulaMember.getParentMember() != null) {
                if (parent instanceof Member) {
                    // SSAS matches calc members very loosely. For example,
                    // [Foo].[Z] will match calc member [Foo].[X].[Y].[Z].
                    return formulaMember.getParentMember().isChildOrEqualTo(
                        (Member) parent);
                } else if (parent instanceof Hierarchy) {
                    return formulaMember.getParentMember().getHierarchy()
                        .equals(parent);
                } else {
                    return parent.getUniqueName().equals(
                        formulaMember.getParentMember().getUniqueName());
                }
            } else {
                // If parent is not a member, member must be a root member.
                return parent.equals(formulaMember.getHierarchy())
                    || parent.equals(formulaMember.getDimension());
            }
        } else {
            return parent instanceof Cube;
        }
    }


}
