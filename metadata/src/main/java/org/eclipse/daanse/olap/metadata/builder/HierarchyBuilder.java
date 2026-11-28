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
package org.eclipse.daanse.olap.metadata.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.findings.Place;
import org.eclipse.daanse.olap.metadata.DescriptionRules;
import org.eclipse.daanse.olap.metadata.model.ExplicitHierarchyDef;
import org.eclipse.daanse.olap.metadata.model.HierarchyDef;
import org.eclipse.daanse.olap.metadata.model.LevelDef;
import org.eclipse.daanse.olap.metadata.model.LocalizedText;
import org.eclipse.daanse.olap.metadata.model.ParentChildHierarchyDef;

/** Builds a hierarchy. It has an All member named "All &lt;name&gt;" unless told otherwise. */
public final class HierarchyBuilder extends ElementBuilder<HierarchyBuilder> {

    private String allMemberName;
    private String allLevelName;
    private LocalizedText allMemberCaption;
    private String defaultMember;
    private boolean ragged;
    private boolean parentChild;
    private final List<LevelDef> levels = new ArrayList<>();

    HierarchyBuilder(String name) {
        super(name);
        this.allMemberName = "All " + name;
    }

    public HierarchyBuilder allMember(String allName) {
        this.allMemberName = allName;
        return this;
    }

    /** An All member with this name that shows this caption. */
    public HierarchyBuilder allMember(String allName, String caption) {
        this.allMemberCaption = caption == null ? null : LocalizedText.of(caption);
        return allMember(allName);
    }

    /** The name of the level the All member is on, instead of {@code (All)}. */
    public HierarchyBuilder allLevel(String levelName) {
        this.allLevelName = levelName;
        return this;
    }

    public HierarchyBuilder noAllMember() {
        this.allMemberName = null;
        return this;
    }

    /** MDX text naming the default member. */
    public HierarchyBuilder defaultMember(String mdx) {
        this.defaultMember = mdx;
        return this;
    }

    /** A member may skip levels. */
    public HierarchyBuilder ragged() {
        this.ragged = true;
        return this;
    }

    /** The members nest within the one level of this hierarchy, each naming its parent. */
    public HierarchyBuilder parentChild() {
        this.parentChild = true;
        return this;
    }

    private org.eclipse.daanse.olap.metadata.model.MembersWithData membersWithData;
    private String dataMemberCaption;

    /**
     * A parent's own facts show as its first child (SSAS {@code NonLeafDataVisible}, Mondrian {@code parentAsLeaf}),
     * named by the pattern over the parent's name, {@code "%s (own)"}; {@code null} keeps the parent's name.
     */
    public HierarchyBuilder membersWithDataVisible(String caption) {
        this.membersWithData = org.eclipse.daanse.olap.metadata.model.MembersWithData.NON_LEAF_DATA_VISIBLE;
        this.dataMemberCaption = caption;
        return this;
    }

    public HierarchyBuilder level(String levelName) {
        return level(levelName, l -> { });
    }

    public HierarchyBuilder level(String levelName, Consumer<LevelBuilder> level) {
        LevelBuilder builder = new LevelBuilder(levelName);
        level.accept(builder);
        levels.add(builder.build());
        return this;
    }

    private boolean namedAfterUsage;

    /**
     * A cube shows this hierarchy under the name of the usage ({@link HierarchyDef#namedAfterUsage()}), for a
     * connector that translates a catalog file whose hierarchy has no name. Code that builds a catalog names its
     * hierarchies.
     */
    public HierarchyBuilder namedAfterUsage() {
        this.namedAfterUsage = true;
        return this;
    }

    private org.eclipse.daanse.olap.metadata.relational.HierarchyRows rows;
    private org.eclipse.daanse.olap.metadata.relational.ParentChildColumns parentChildColumns;

    /**
     * Where the members lie in tables: the source and the column a fact's foreign key points at.
     */
    public HierarchyBuilder rows(org.eclipse.daanse.olap.metadata.relational.RelationalSource source, org.eclipse.daanse.olap.metadata.relational.ColumnDef primaryKey) {
        this.rows = new org.eclipse.daanse.olap.metadata.relational.HierarchyRows(source, primaryKey, null);
        return this;
    }

    public HierarchyBuilder rows(org.eclipse.daanse.olap.metadata.relational.HierarchyRows rows) {
        this.rows = rows;
        return this;
    }

    /** How a parent-child hierarchy's rows point at their parents. */
    public HierarchyBuilder parentColumns(org.eclipse.daanse.olap.metadata.relational.ParentChildColumns columns) {
        this.parentChildColumns = columns;
        return this;
    }

    HierarchyDef build(List<Finding> problems, Place path) {
        if (parentChild) {
            // a hierarchy without a level is blamed for that by the validator, like every other one
            if (levels.size() > 1) {
                problems.add(BuildFindings.of(DescriptionRules.M103, path.then("hierarchy", name),
                        "a parent-child hierarchy has exactly one level, not " + levels.size()));
            }
            if (ragged) {
                problems.add(BuildFindings.of(DescriptionRules.M104, path.then("hierarchy", name),
                        "a parent-child hierarchy cannot be ragged"));
            }
            return new ParentChildHierarchyDef(name, allMemberName, allMemberCaption, allLevelName, defaultMember,
                    levels.isEmpty() ? null : levels.get(0), info(), rows, parentChildColumns, namedAfterUsage,
                    membersWithData, dataMemberCaption);
        }
        if (membersWithData != null) {
            problems.add(BuildFindings.of(DescriptionRules.M107, path.then("hierarchy", name),
                    "shows data members but is no parent-child hierarchy"));
        }
        if (parentChildColumns != null) {
            problems.add(BuildFindings.of(DescriptionRules.M108, path.then("hierarchy", name),
                    "names a parent column but is no parent-child hierarchy"));
        }
        return new ExplicitHierarchyDef(name, allMemberName, allMemberCaption, allLevelName, defaultMember, levels,
                ragged, info(), rows, namedAfterUsage);
    }
}
