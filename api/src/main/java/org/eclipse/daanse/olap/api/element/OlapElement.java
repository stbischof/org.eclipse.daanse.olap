/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1998-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
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
 */
package org.eclipse.daanse.olap.api.element;

import java.util.Locale;

import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;

/**
 * An OlapElement is a catalog object (dimension, hierarchy, level, member).
 *
 * @author jhyde, 21 January, 1999
 */
public interface OlapElement {
    String getUniqueName();

    String getName();

    String getDescription();

    /**
     * Looks up a child element, returning null if it does not exist.
     */
    OlapElement lookupChild(CatalogReader schemaReader, Segment s, MatchType matchType);

    /**
     * Returns the name of this element qualified by its class, for example
     * "hierarchy 'Customers'".
     */
    String getQualifiedName();

    String getCaption();

    /**
     * Returns the value of a property (caption or description) of this element in
     * the given locale.
     *
     * @param locale Locale
     * @return Localized caption or description
     */
    String getLocalized(LocalizedProperty prop, Locale locale);

    Hierarchy getHierarchy();

    /**
     * Returns the dimension of a this expression, or null if no dimension is
     * defined. Applicable only to set expressions.
     *
     *
     * Example 1:
     *
     *
     * [Sales].children
     *
     *
     * has dimension [Sales].
     *
     *
     *
     * Example 2:
     *
     *
     * order(except([Promotion Media].[Media Type].members, {[Promotion
     * Media].[Media Type].[No Media]}), [Measures].[Unit Sales], DESC)
     *
     *
     * has dimension [Promotion Media].
     *
     *
     *
     * Example 3:
     *
     *
     * CrossJoin([Product].[Product Department].members, [Gender].members)
     *
     *
     * has no dimension (well, actually it is [Product] x [Gender], but we can't
     * represent that, so we return null);
     *
     */
    Dimension getDimension();

    /**
     * Returns whether this element is visible to end-users.
     *
     *
     * Visibility is a hint for client applications. An element's visibility does
     * not affect how it is treated when MDX queries are evaluated.
     *
     * @return Whether this element is visible
     */
    boolean isVisible();

    enum LocalizedProperty {
        CAPTION, DESCRIPTION
    }
}
