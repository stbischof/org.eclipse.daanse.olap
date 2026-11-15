/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1999-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
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
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.api.element;

import java.util.List;

import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;

/**
 * A Hierarchy is a set of members, organized into levels.
 */
public interface Hierarchy extends OlapElement, MetaElement {
    /**
     * Returns the dimension this hierarchy belongs to.
     */
    @Override
    Dimension getDimension();

    /**
     * Returns the levels in this hierarchy.
     *
     *
     * If a hierarchy is subject to access-control, some of the levels may not be
     * visible; use CatalogReader#getHierarchyLevels instead.
     *
     * return != null
     */
    List<? extends Level> getLevels();

    /**
     * Returns the default member of this hierarchy.
     *
     *
     * If a hierarchy is subject to access-control, the default member may not be
     * visible, so use CatalogReader#getHierarchyDefaultMember.
     *
     * return != null
     */
    Member getDefaultMember();

    /**
     * Returns the "All" member of this hierarchy.
     *
     * return != null
     */
    Member getAllMember();

    /**
     * Whether a write chooses its scenario from this hierarchy.
     * <p>
     * Asked rather than tested by name, so that a catalog which calls its writing dimension something
     * else can say so. The default answers what every catalog has answered so far: the hierarchy called
     * {@code Scenario}.
     */
    default boolean isScenario() {
        return "Scenario".equals(getName());
    }

    /**
     * Returns a special member representing the "null" value. This never occurs on
     * an axis, but may occur if functions such as Lead, NextMember and ParentMember
     * walk off the end of the hierarchy.
     *
     * return != null
     */
    Member getNullMember();

    boolean hasAll();

    /**
     * Creates a member of this hierarchy. If this is the measures hierarchy, a
     * calculated member is created, and formula must not be null.
     */
    Member createMember(Member parent, Level level, String name, Formula formula);

    /**
     * Creates the member the {@code VisualTotals} function substitutes for
     * {@code member}: same parent and level, the given name and caption, and
     * {@code exp} as its aggregate expression. A provider whose evaluator needs
     * its own member type returns that type here.
     */
    VisualTotalMember createVisualTotalMember(Member member, String name, String caption, Expression exp);

    /**
     * Returns the unique name of this hierarchy, always including the dimension
     * name, e.g. "[Time].[Time]", regardless of whether
     * SSAS-compatible naming is enabled.
     *
     * @deprecated Will be removed in mondrian-4.0, when #getUniqueName() will have
     *             this behavior.
     *
     * @return Unique name of hierarchy.
     */
    @Deprecated
    String getUniqueNameSsas();

    String getDisplayFolder();

    String origin();

    List<Member> getRootMembers();

    /**
     * Returns the ordinal of this hierarchy in its cube.
     *
     *
     * Temporarily defined against RolapHierarchy; will be moved to
     * RolapCubeHierarchy as soon as the measures hierarchy is a RolapCubeHierarchy.
     *
     * @return Ordinal of this hierarchy in its cube
     */
    int getOrdinalInCube();

    String getSubName();
}
