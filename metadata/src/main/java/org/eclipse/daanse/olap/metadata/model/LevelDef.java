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

/** A level of a hierarchy. Identity: the same object wherever its hierarchy is used. */
public final class LevelDef {

    private final String name;
    private final LevelKind kind;
    private final ValueKind keyKind;
    private final boolean uniqueMembers;
    private final boolean ordered;
    private final boolean descending;
    private final HideMember hideMember;
    private final List<MemberPropertyDef> properties;
    private final FormatterSpec memberFormatter;
    private final ElementInfo info;
    private final Integer approxRowCount;
    private final org.eclipse.daanse.olap.metadata.relational.LevelColumns columns;

    public LevelDef(String name, LevelKind kind, ValueKind keyKind, boolean uniqueMembers, boolean ordered,
            HideMember hideMember, List<MemberPropertyDef> properties, FormatterSpec memberFormatter,
            ElementInfo info) {
        this(name, kind, keyKind, uniqueMembers, ordered, false, hideMember, properties, memberFormatter, info);
    }

    /**
     * A level whose members, when it is ordered, are placed by their order key in the given direction -- the
     * direction a relational level gives its ordinal column.
     */
    public LevelDef(String name, LevelKind kind, ValueKind keyKind, boolean uniqueMembers, boolean ordered,
            boolean descending, HideMember hideMember, List<MemberPropertyDef> properties,
            FormatterSpec memberFormatter, ElementInfo info) {
        this(name, kind, keyKind, uniqueMembers, ordered, descending, hideMember, properties, memberFormatter, info,
                null);
    }

    public LevelDef(String name, LevelKind kind, ValueKind keyKind, boolean uniqueMembers, boolean ordered,
            boolean descending, HideMember hideMember, List<MemberPropertyDef> properties,
            FormatterSpec memberFormatter, ElementInfo info, Integer approxRowCount) {
        this(name, kind, keyKind, uniqueMembers, ordered, descending, hideMember, properties, memberFormatter, info,
                approxRowCount, null);
    }

    public LevelDef(String name, LevelKind kind, ValueKind keyKind, boolean uniqueMembers, boolean ordered,
            boolean descending, HideMember hideMember, List<MemberPropertyDef> properties,
            FormatterSpec memberFormatter, ElementInfo info, Integer approxRowCount,
            org.eclipse.daanse.olap.metadata.relational.LevelColumns columns) {
        this.columns = columns;
        this.approxRowCount = approxRowCount;
        this.descending = descending;
        this.name = name;
        this.kind = kind;
        this.keyKind = keyKind;
        this.uniqueMembers = uniqueMembers;
        this.ordered = ordered;
        this.hideMember = hideMember == null ? HideMember.NEVER : hideMember;
        this.properties = List.copyOf(properties);
        this.memberFormatter = memberFormatter;
        this.info = info;
    }

    public String name() {
        return name;
    }

    /** Which columns the members are read from; empty for a level whose members come through a source of its own. */
    public java.util.Optional<org.eclipse.daanse.olap.metadata.relational.LevelColumns> columns() {
        return java.util.Optional.ofNullable(columns);
    }

    /**
     * How many members the catalog says the level holds (SSAS EstimatedCount); {@code null} when it says nothing and
     * they are counted.
     */
    public Integer approxRowCount() {
        return approxRowCount;
    }

    public LevelKind kind() {
        return kind;
    }

    /** The kind of the member keys of this level, or null when the catalog says nothing about them. */
    public ValueKind keyKind() {
        return keyKind;
    }

    /** Whether an ordered level places its members by descending order key. */
    public boolean descending() {
        return descending;
    }

    /** Whether a member name is unique on the whole level, not only below its parent. */
    public boolean uniqueMembers() {
        return uniqueMembers;
    }

    /** When a member of this level is none of a client's business, so that the hierarchy has gaps. */
    public HideMember hideMember() {
        return hideMember;
    }

    /** Whether the members have an order of their own, other than the order they are supplied in. */
    public boolean ordered() {
        return ordered;
    }

    public List<MemberPropertyDef> properties() {
        return properties;
    }

    /** {@code null} when member captions need no formatter. */
    public FormatterSpec memberFormatter() {
        return memberFormatter;
    }

    public ElementInfo info() {
        return info;
    }

    @Override
    public String toString() {
        return "level " + name;
    }
}
