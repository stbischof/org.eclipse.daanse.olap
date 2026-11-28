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
package org.eclipse.daanse.olap.metadata.model;

import java.util.List;

/**
 * A hierarchy of a dimension: either {@link ExplicitHierarchyDef}, with one level per depth, or
 * {@link ParentChildHierarchyDef}, where members of one level nest. Identity.
 */
public abstract sealed class HierarchyDef permits ExplicitHierarchyDef, ParentChildHierarchyDef {

    private final String name;
    private final String allMemberName;
    private final String allLevelName;
    private final String defaultMember;
    private final ElementInfo info;
    private final LocalizedText allMemberCaption;
    private final org.eclipse.daanse.olap.metadata.relational.HierarchyRows rows;
    private final boolean namedAfterUsage;

    HierarchyDef(String name, String allMemberName, String allLevelName, String defaultMember, ElementInfo info) {
        this(name, allMemberName, null, allLevelName, defaultMember, info);
    }

    HierarchyDef(String name, String allMemberName, LocalizedText allMemberCaption, String allLevelName, String defaultMember,
            ElementInfo info) {
        this(name, allMemberName, allMemberCaption, allLevelName, defaultMember, info, null);
    }

    HierarchyDef(String name, String allMemberName, LocalizedText allMemberCaption, String allLevelName, String defaultMember,
            ElementInfo info, org.eclipse.daanse.olap.metadata.relational.HierarchyRows rows) {
        this(name, allMemberName, allMemberCaption, allLevelName, defaultMember, info, rows, false);
    }

    HierarchyDef(String name, String allMemberName, LocalizedText allMemberCaption, String allLevelName, String defaultMember,
            ElementInfo info, org.eclipse.daanse.olap.metadata.relational.HierarchyRows rows, boolean namedAfterUsage) {
        this.namedAfterUsage = namedAfterUsage;
        this.rows = rows;
        this.allMemberCaption = allMemberCaption;
        this.name = name;
        this.allMemberName = allMemberName;
        this.allLevelName = allLevelName;
        this.defaultMember = defaultMember;
        this.info = info;
    }

    public String name() {
        return name;
    }

    /**
     * Whether a cube shows this hierarchy under the name of the usage instead of its own: Mondrian's rule for a
     * hierarchy that has no name of its own, which takes the name of the cube dimension. {@link #name()} stays the
     * hierarchy's name in the description and towards its member source. SSAS has no hierarchy without a name.
     */
    public boolean namedAfterUsage() {
        return namedAfterUsage;
    }

    /** Where the members lie in tables; empty for a hierarchy whose members come through a source of its own. */
    public java.util.Optional<org.eclipse.daanse.olap.metadata.relational.HierarchyRows> rows() {
        return java.util.Optional.ofNullable(rows);
    }

    /** The name of the All member; {@code null} when the hierarchy has none. */
    public String allMemberName() {
        return allMemberName;
    }

    /** The caption of the All member; {@code null} when it shows its name. */
    public LocalizedText allMemberCaption() {
        return allMemberCaption;
    }

    /** The name of the level the All member is on; {@code null} for {@code (All)}. */
    public String allLevelName() {
        return allLevelName;
    }

    public boolean hasAll() {
        return allMemberName != null;
    }

    /** MDX text naming the default member; {@code null} for the All member or the first root. */
    public String defaultMember() {
        return defaultMember;
    }

    public ElementInfo info() {
        return info;
    }

    /** The levels below the All level, top down. */
    public abstract List<LevelDef> levels();

    @Override
    public String toString() {
        return "hierarchy " + name;
    }
}
