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
import java.util.function.Function;

import org.eclipse.daanse.olap.metadata.model.ElementInfo;
import org.eclipse.daanse.olap.metadata.model.FormatterSpec;
import org.eclipse.daanse.olap.metadata.model.HideMember;
import org.eclipse.daanse.olap.metadata.model.LevelDef;
import org.eclipse.daanse.olap.metadata.model.LevelKind;
import org.eclipse.daanse.olap.metadata.model.MemberPropertyDef;
import org.eclipse.daanse.olap.metadata.model.ValueKind;

public final class LevelBuilder extends ElementBuilder<LevelBuilder> {

    private LevelKind kind = LevelKind.REGULAR;
    /** What the keys of this level hold; {@code null} until the catalog says, and then nobody claims. */
    private ValueKind keyKind;
    private boolean uniqueMembers;
    private boolean ordered;
    private boolean descending;
    private HideMember hideMember = HideMember.NEVER;
    private FormatterSpec memberFormatter;
    private final List<MemberPropertyDef> properties = new ArrayList<>();

    LevelBuilder(String name) {
        super(name);
    }

    public LevelBuilder kind(LevelKind kind) {
        this.kind = kind;
        return this;
    }

    /** What the keys of this level's members hold. A level that never says this says nothing. */
    public LevelBuilder keyKind(ValueKind keyKind) {
        this.keyKind = keyKind;
        return this;
    }

    /** A member name is unique on the whole level, so MDX may address it without its parents. */
    public LevelBuilder uniqueMembers() {
        this.uniqueMembers = true;
        return this;
    }

    /** The members carry an order of their own. */
    public LevelBuilder ordered() {
        this.ordered = true;
        return this;
    }

    /** Ordered as {@link #ordered()}, by descending order key. */
    public LevelBuilder orderedDescending() {
        this.ordered = true;
        this.descending = true;
        return this;
    }

    /**
     * When a member of this level is none of a client's business, which gives the hierarchy gaps: a
     * member whose name is blank, or one that repeats the name of its parent, stands for a step the
     * data does not take.
     */
    public LevelBuilder hideMemberIf(HideMember when) {
        this.hideMember = when;
        return this;
    }

    public LevelBuilder property(String propertyName, ValueKind propertyKind) {
        properties.add(new MemberPropertyDef(propertyName, propertyKind, ElementInfo.DEFAULT, null));
        return this;
    }

    /**
     * A property with a caption a client shows instead of its name and a description.
     *
     * @param caption     the caption, or {@code null} to show the name
     * @param description the description, or {@code null}
     */
    public LevelBuilder property(String propertyName, ValueKind propertyKind, String caption, String description) {
        properties.add(new MemberPropertyDef(propertyName, propertyKind,
                new ElementInfo(caption == null ? null : org.eclipse.daanse.olap.metadata.model.LocalizedText.of(caption),
                        description == null ? null : org.eclipse.daanse.olap.metadata.model.LocalizedText.of(description),
                        true, java.util.List.of()),
                null));
        return this;
    }

    /** A property whose value a client sees through a formatter of this catalog's own. */
    public LevelBuilder property(String propertyName, ValueKind propertyKind, Function<Object, String> formatter) {
        properties.add(new MemberPropertyDef(propertyName, propertyKind, ElementInfo.DEFAULT,
                new FormatterSpec.ByInstance(formatter)));
        return this;
    }

    /**
     * A property whose formatter is named by its class, as a catalog file names it; an engine that looks no formatter
     * up by name refuses the catalog, naming the formatter and the property.
     */
    public LevelBuilder propertyFormattedBy(String propertyName, ValueKind propertyKind, String formatterName) {
        properties.add(new MemberPropertyDef(propertyName, propertyKind, ElementInfo.DEFAULT,
                new FormatterSpec.ByName(formatterName)));
        return this;
    }

    public LevelBuilder memberFormatter(Function<Object, String> formatter) {
        this.memberFormatter = new FormatterSpec.ByInstance(formatter);
        return this;
    }

    /** A member formatter named by its class, as a catalog file names it. */
    public LevelBuilder memberFormatterNamed(String className) {
        this.memberFormatter = new FormatterSpec.ByName(className);
        return this;
    }

    private Integer approxRowCount;

    /**
     * How many members the level holds, as the catalog estimates it (Mondrian approxRowCount): the All member answers
     * CHILDREN_CARDINALITY with it instead of counting.
     */
    public LevelBuilder approxRowCount(int approxRowCount) {
        this.approxRowCount = approxRowCount;
        return this;
    }

    private org.eclipse.daanse.olap.metadata.relational.LevelColumns columns;

    /** A property every member carries, read from a column. */
    public LevelBuilder property(String propertyName, ValueKind propertyKind,
            org.eclipse.daanse.olap.metadata.relational.ColumnDef column) {
        properties.add(new MemberPropertyDef(propertyName, propertyKind, ElementInfo.DEFAULT, null, column));
        return this;
    }

    /** A property read from a column and shown through a formatter named by class, as a catalog file names both. */
    public LevelBuilder propertyFormattedBy(String propertyName, ValueKind propertyKind,
            org.eclipse.daanse.olap.metadata.relational.ColumnDef column, String formatterName) {
        return propertyFormattedBy(propertyName, propertyKind, column, formatterName, null);
    }

    /** As {@link #propertyFormattedBy(String, ValueKind, org.eclipse.daanse.olap.metadata.relational.ColumnDef, String)}, with a description (or {@code null}). */
    public LevelBuilder propertyFormattedBy(String propertyName, ValueKind propertyKind,
            org.eclipse.daanse.olap.metadata.relational.ColumnDef column, String formatterName, String description) {
        properties.add(new MemberPropertyDef(propertyName, propertyKind, described(description),
                formatterName == null ? null : new FormatterSpec.ByName(formatterName), column));
        return this;
    }

    /** The info of a property that shows its name and says what it is, if anything. */
    private static ElementInfo described(String description) {
        return description == null ? ElementInfo.DEFAULT
                : new ElementInfo(null, org.eclipse.daanse.olap.metadata.model.LocalizedText.of(description), true,
                        java.util.List.of());
    }

    /**
     * A property read from a column whose value follows from the level's key (Mondrian dependsOnLevelValue), shown
     * through a formatter named by class where one is given.
     */
    public LevelBuilder propertyDependingOnLevelValue(String propertyName, ValueKind propertyKind,
            org.eclipse.daanse.olap.metadata.relational.ColumnDef column, String formatterName) {
        return propertyDependingOnLevelValue(propertyName, propertyKind, column, formatterName, null);
    }

    /** As {@link #propertyDependingOnLevelValue(String, ValueKind, org.eclipse.daanse.olap.metadata.relational.ColumnDef, String)}, with a description. */
    public LevelBuilder propertyDependingOnLevelValue(String propertyName, ValueKind propertyKind,
            org.eclipse.daanse.olap.metadata.relational.ColumnDef column, String formatterName, String description) {
        properties.add(new MemberPropertyDef(propertyName, propertyKind, described(description),
                formatterName == null ? null : new FormatterSpec.ByName(formatterName), column, true));
        return this;
    }

    /** The column the members' keys are read from. */
    public LevelBuilder column(org.eclipse.daanse.olap.metadata.relational.ColumnDef key) {
        this.columns = org.eclipse.daanse.olap.metadata.relational.LevelColumns.of(key);
        return this;
    }

    /** The columns the members are read from: key, name, caption, order. */
    public LevelBuilder columns(org.eclipse.daanse.olap.metadata.relational.LevelColumns columns) {
        this.columns = columns;
        return this;
    }

    LevelDef build() {
        return new LevelDef(name, kind, keyKind, uniqueMembers, ordered, descending, hideMember, properties,
                memberFormatter, info(), approxRowCount, columns);
    }
}
