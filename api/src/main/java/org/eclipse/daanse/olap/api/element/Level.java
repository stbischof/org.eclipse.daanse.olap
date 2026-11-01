/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1999-2005 Julian Hyde
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

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.formatter.MemberFormatter;

/**
 * A Level is a group of Members in a Hierarchy, all with the same attributes
 * and at the same depth in the hierarchy.
 *
 * @author jhyde, 1 March, 1999
 */
public interface Level extends OlapElement, MetaElement {

    /**
     * Returns the depth of this level.
     *
     *
     * Note #1: In an access-controlled context, the first visible level of a
     * hierarchy (as returned by CatalogReader#getHierarchyLevels) may not have a
     * depth of 0.
     *
     *
     *
     * Note #2: In a parent-child hierarchy, the depth of a member (as returned by
     * CatalogReader#getMemberDepth) may not be the same as the depth of its level.
     */
    int getDepth();

    @Override
    Hierarchy getHierarchy();

    Level getChildLevel();

    Level getParentLevel();

    boolean isAll();

    boolean areMembersUnique();

    LevelType getLevelType();

    /** Returns properties defined against this level. */
    Property[] getProperties();

    /** Returns properties defined against this level and parent levels. */
    Property[] getInheritedProperties();

    /**
     * Returns the object that is used to format members of this level.
     */
    MemberFormatter getMemberFormatter();

    /**
     * Returns the approximate number of members in this level, or Integer#MIN_VALUE
     * if no approximation is known.
     */
    int getApproxRowCount();

    int getCardinality();

    List<Member> getMembers();

    boolean isUnique();

    /**
     * Whether the members of this level carry an order key of their own, so that siblings can be
     * ordered by it instead of by their ordinal.
     */
    default boolean hasOrderKey() {
        return false;
    }

    boolean isParentChild();

    boolean isParentAsLeafEnable();

    String getParentAsLeafNameFormat();

    default HideMemberCondition getHideMemberCondition() {
        return HideMemberCondition.NEVER;
    }

    default Optional<OrderByProperty> getOrderByProperty() {
        return Optional.empty();
    }

}
