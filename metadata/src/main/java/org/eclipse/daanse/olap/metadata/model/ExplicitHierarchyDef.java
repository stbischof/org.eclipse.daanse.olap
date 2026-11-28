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

/** A hierarchy with one level per depth; {@code ragged} when a member may skip levels. */
public final class ExplicitHierarchyDef extends HierarchyDef {

    private final List<LevelDef> levels;
    private final boolean ragged;

    public ExplicitHierarchyDef(String name, String allMemberName, String allLevelName, String defaultMember,
            List<LevelDef> levels, boolean ragged, ElementInfo info) {
        this(name, allMemberName, null, allLevelName, defaultMember, levels, ragged, info);
    }

    public ExplicitHierarchyDef(String name, String allMemberName, LocalizedText allMemberCaption, String allLevelName,
            String defaultMember, List<LevelDef> levels, boolean ragged, ElementInfo info) {
        this(name, allMemberName, allMemberCaption, allLevelName, defaultMember, levels, ragged, info, null);
    }

    public ExplicitHierarchyDef(String name, String allMemberName, LocalizedText allMemberCaption, String allLevelName,
            String defaultMember, List<LevelDef> levels, boolean ragged, ElementInfo info,
            org.eclipse.daanse.olap.metadata.relational.HierarchyRows rows) {
        this(name, allMemberName, allMemberCaption, allLevelName, defaultMember, levels, ragged, info, rows, false);
    }

    public ExplicitHierarchyDef(String name, String allMemberName, LocalizedText allMemberCaption, String allLevelName,
            String defaultMember, List<LevelDef> levels, boolean ragged, ElementInfo info,
            org.eclipse.daanse.olap.metadata.relational.HierarchyRows rows, boolean namedAfterUsage) {
        super(name, allMemberName, allMemberCaption, allLevelName, defaultMember, info, rows, namedAfterUsage);
        this.levels = List.copyOf(levels);
        this.ragged = ragged;
    }

    @Override
    public List<LevelDef> levels() {
        return levels;
    }

    public boolean ragged() {
        return ragged;
    }
}
