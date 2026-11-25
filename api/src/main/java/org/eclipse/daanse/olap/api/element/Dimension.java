/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1999-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * Copyright (C) 2021 Sergei Semenkov
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

import java.util.List;

/**
 * A Dimension represents a dimension of a cube.
 *
 * @author jhyde, 1 March, 1999
 */
public interface Dimension extends OlapElement, MetaElement {
    public static final String MEASURES_UNIQUE_NAME = "[Measures]";
    public static final String MEASURES_NAME = "Measures";

    /**
     * Returns an array of the hierarchies which belong to this dimension.
     */
    List<? extends Hierarchy> getHierarchies();

    /**
     * Returns whether this is the [Measures] dimension.
     */
    boolean isMeasures();

    /**
     * Returns the type of this dimension ( DimensionType#STANDARD_DIMENSION or
     * DimensionType#TIME_DIMENSION
     */
    DimensionType getDimensionType();

    /**
     * Returns the schema this dimension belongs to.
     */
    Catalog getCatalog();

    Cube getCube();

    /**
     * Whether this dimension is the facts themselves.
     *
     * <p>Such a dimension keeps its members in the fact table instead of a table of its own -- an order
     * number, a transaction kind -- so there is no key pointing anywhere and every fact is its own member.
     * A catalog that keeps all its dimensions apart from the facts says no to all of them.
     */
    default boolean isFactDimension() {
        return false;
    }
}
