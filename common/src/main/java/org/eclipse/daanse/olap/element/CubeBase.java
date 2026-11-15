/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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

package org.eclipse.daanse.olap.element;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.common.Util;
/**
 * CubeBase is an abstract implementation of {@link Cube}.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public abstract class CubeBase extends OlapElementBase implements Cube {

    protected final String name;
    private final String uniqueName;
    private final String description;
    protected List<Dimension> dimensions;
    private final static String mdxCubeName = "cube ''{0}''";

    /**
     * Creates a CubeBase.
     *
     * @param name Name
     * @param caption Caption
     * @param description Description
     * @param dimensions List of dimensions
     */
    protected CubeBase(
        String name,
        String caption,
        boolean visible,
        String description,
        List<Dimension> dimensions)
    {
        this.name = name;
        this.caption = caption;
        this.visible = visible;
        this.description = description;
        this.dimensions = dimensions;
        this.uniqueName = Util.quoteMdxIdentifier(name);
    }

    // implement OlapElement
    @Override
	public String getName() {
        return name;
    }

    @Override
	public String getUniqueName() {
        // return e.g. '[Sales Ragged]'
        return uniqueName;
    }

    @Override
	public String getQualifiedName() {
        return MessageFormat.format(mdxCubeName, getName());
    }

    @Override
	public Dimension getDimension() {
        return null;
    }

    @Override
	public Hierarchy getHierarchy() {
        return null;
    }

    @Override
	public String getDescription() {
        return description;
    }

    @Override
	public List<? extends Dimension> getDimensions() {
        return dimensions;
    }

    @Override
	public Hierarchy lookupHierarchy(NameSegment s, boolean unique) {
        for (Dimension dimension : dimensions) {
            List<? extends Hierarchy> hierarchies = dimension.getHierarchies();
            for (Hierarchy hierarchy : hierarchies) {
                String nameInner = unique
                    ? hierarchy.getUniqueName() : hierarchy.getName();
                if (nameInner.equals(s.getName())) {
                    return hierarchy;
                }
            }
        }
        return null;
    }

    @Override
	public OlapElement lookupChild(
        CatalogReader schemaReader,
        Segment segment,
        MatchType matchType)
    {
        Dimension mdxDimension = lookupDimension(segment);
        if (mdxDimension != null) {
            return mdxDimension;
        }

        final List<Dimension> dimensionsInner = schemaReader.getCubeDimensions(this);

        // Look for hierarchies named '[dimension.hierarchy]'.
        if (segment instanceof NameSegment nameSegment) {
            Hierarchy hierarchy = lookupHierarchy(nameSegment, false);
            if (hierarchy != null) {
                return hierarchy;
            }
        }

        // Try hierarchies, levels and members.
        for (Dimension dimension : dimensionsInner) {
            OlapElement mdxElement = dimension.lookupChild(
                schemaReader, segment, matchType);
            if (mdxElement != null) {
                return mdxElement;
            }
        }
        return null;
    }

    /**
     * Looks up a dimension in this cube based on a component of its name.
     *
     * @param s Name segment
     * @return Dimension, or null if not found
     */
    public Dimension lookupDimension(Segment s) {
        if (!(s instanceof NameSegment nameSegment)) {
            return null;
        }
        for (Dimension dimension : dimensions) {
            if (Util.equalName(dimension.getName(), nameSegment.getName())) {
                return dimension;
            }
        }
        return null;
    }

}
