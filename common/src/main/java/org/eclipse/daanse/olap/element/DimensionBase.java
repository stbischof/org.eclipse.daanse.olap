/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
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


package org.eclipse.daanse.olap.element;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.DimensionType;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.common.Util;
/**
 * Abstract implementation for a {@link Dimension}.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public abstract class DimensionBase
    extends OlapElementBase
    implements Dimension
{
    protected final String name;
    protected final String uniqueName;
    protected final String description;
    protected List<Hierarchy> hierarchies;
    protected DimensionType dimensionType;
    private final static String mdxDimensionName = "dimension ''{0}''";

    /**
     * Creates a DimensionBase.
     *
     * @param name Name
     * @param dimensionType Type
     */
    protected DimensionBase(
        String name,
        String caption,
        boolean visible,
        String description,
        DimensionType dimensionType)
    {
        Util.assertTrue(name != null, "Dimension or DimensionConnector name should be not null");
        this.name = name;
        this.caption = caption;
        this.visible = visible;
        this.uniqueName = Util.makeFqName(name);
        this.description = description;
        this.dimensionType = dimensionType;
    }

    @Override
	public String getUniqueName() {
        return uniqueName;
    }

    @Override
	public String getName() {
        return name;
    }

    @Override
	public String getDescription() {
        return description;
    }

    @Override
	public List<? extends Hierarchy> getHierarchies() {
        return hierarchies;
    }

    @Override
	public Hierarchy getHierarchy() {
        return hierarchies.getFirst();
    }

    @Override
	public Dimension getDimension() {
        return this;
    }

    @Override
	public DimensionType getDimensionType() {
        return dimensionType;
    }

    @Override
	public String getQualifiedName() {
        return MessageFormat.format(mdxDimensionName, getUniqueName());
    }

    @Override
	public boolean isMeasures() {
        return getUniqueName().equals(MEASURES_UNIQUE_NAME);
    }

    @Override
	public OlapElement lookupChild(
        CatalogReader schemaReader, Segment s, MatchType matchType)
    {
        OlapElement oe = null;
        if (s instanceof NameSegment nameSegment) {
            oe = lookupHierarchy(nameSegment);
        }

        // Original mondrian behavior:
        // If the user is looking for [Marital Status].[Marital Status] we
        // should not return oe "Marital Status", because he is
        // looking for level - we can check that by checking of hierarchy and
        // dimension name is the same.
        //
        // New (SSAS-compatible) behavior. If there is no matching
        // hierarchy, find the first level with the given name.
        if (oe != null) {
            return oe;
        }
        final List<Hierarchy> hierarchyList =
            schemaReader.getDimensionHierarchies(this);
        for (Hierarchy hierarchy : hierarchyList) {
            oe = hierarchy.lookupChild(schemaReader, s, matchType);
            if (oe != null) {
                return oe;
            }
        }
        return null;
    }


    private Hierarchy lookupHierarchy(NameSegment s) {
        for (Hierarchy hierarchy : hierarchies) {
            if (Util.equalName(hierarchy.getName(), s.getName())) {
                return hierarchy;
            }
        }
        return null;
    }
}
