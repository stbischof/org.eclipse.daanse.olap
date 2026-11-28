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
 * A hierarchy whose members nest within one level, each naming its parent, as employees do.
 * The member source supplies the parent with every member.
 */
public final class ParentChildHierarchyDef extends HierarchyDef {

    private final LevelDef level;
    private final org.eclipse.daanse.olap.metadata.relational.ParentChildColumns columns;

    public ParentChildHierarchyDef(String name, String allMemberName, String allLevelName, String defaultMember,
            LevelDef level, ElementInfo info) {
        this(name, allMemberName, null, allLevelName, defaultMember, level, info);
    }

    public ParentChildHierarchyDef(String name, String allMemberName, LocalizedText allMemberCaption, String allLevelName,
            String defaultMember, LevelDef level, ElementInfo info) {
        this(name, allMemberName, allMemberCaption, allLevelName, defaultMember, level, info, null, null);
    }

    public ParentChildHierarchyDef(String name, String allMemberName, LocalizedText allMemberCaption, String allLevelName,
            String defaultMember, LevelDef level, ElementInfo info, org.eclipse.daanse.olap.metadata.relational.HierarchyRows rows,
            org.eclipse.daanse.olap.metadata.relational.ParentChildColumns columns) {
        this(name, allMemberName, allMemberCaption, allLevelName, defaultMember, level, info, rows, columns, false);
    }

    public ParentChildHierarchyDef(String name, String allMemberName, LocalizedText allMemberCaption, String allLevelName,
            String defaultMember, LevelDef level, ElementInfo info, org.eclipse.daanse.olap.metadata.relational.HierarchyRows rows,
            org.eclipse.daanse.olap.metadata.relational.ParentChildColumns columns, boolean namedAfterUsage) {
        this(name, allMemberName, allMemberCaption, allLevelName, defaultMember, level, info, rows, columns,
                namedAfterUsage, MembersWithData.NON_LEAF_DATA_HIDDEN, null);
    }

    public ParentChildHierarchyDef(String name, String allMemberName, LocalizedText allMemberCaption, String allLevelName,
            String defaultMember, LevelDef level, ElementInfo info, org.eclipse.daanse.olap.metadata.relational.HierarchyRows rows,
            org.eclipse.daanse.olap.metadata.relational.ParentChildColumns columns, boolean namedAfterUsage,
            MembersWithData membersWithData, String dataMemberCaption) {
        super(name, allMemberName, allMemberCaption, allLevelName, defaultMember, info, rows, namedAfterUsage);
        this.level = level;
        this.columns = columns;
        this.membersWithData = membersWithData == null ? MembersWithData.NON_LEAF_DATA_HIDDEN : membersWithData;
        this.dataMemberCaption = dataMemberCaption;
    }

    private final MembersWithData membersWithData;
    private final String dataMemberCaption;

    /** Whether a parent's data member shows among its children; hidden by default. */
    public MembersWithData membersWithData() {
        return membersWithData;
    }

    /** The {@link String#format} pattern over the parent's name a visible data member is named by; empty for the name. */
    public java.util.Optional<String> dataMemberCaption() {
        return java.util.Optional.ofNullable(dataMemberCaption);
    }

    /** How a row points at its parent; empty for a hierarchy whose members come through a source of its own. */
    public java.util.Optional<org.eclipse.daanse.olap.metadata.relational.ParentChildColumns> columns() {
        return java.util.Optional.ofNullable(columns);
    }

    /** The one level all members are on; {@code null} when the description names none. */
    public LevelDef level() {
        return level;
    }

    /** The one level, or no level at all, which the validator blames the hierarchy for. */
    @Override
    public List<LevelDef> levels() {
        return level == null ? List.of() : List.of(level);
    }
}
