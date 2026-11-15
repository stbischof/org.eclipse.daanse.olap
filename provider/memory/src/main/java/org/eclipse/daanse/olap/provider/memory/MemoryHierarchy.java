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
package org.eclipse.daanse.olap.provider.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.LevelType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.element.HierarchyBase;

/** A hierarchy the provider holds in memory. */
public class MemoryHierarchy extends HierarchyBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryHierarchy.class);

    private final MemoryDimension miniDimension;
    private final List<MemoryLevel> miniLevels = new ArrayList<>();
    private final boolean hasAll;
    private final String conventionalName;
    private int ordinalInCube;
    private MemoryMember allMember;
    private MemoryMember nullMember;

    MemoryHierarchy(MemoryDimension dimension, String name, boolean hasAll) {
        // MDX names a hierarchy [Dim].[Hier], but drops the second part when the hierarchy
        // carries the dimension's own name. Passing null as the sub-name is how the base
        // class is told that.
        super(dimension, name.equals(dimension.getName()) ? null : name, name, true, null, hasAll);
        this.miniDimension = dimension;
        this.hasAll = hasAll;
        this.conventionalName = name.equals(dimension.getName())
                ? dimension.getUniqueName()
                : super.getUniqueName();
    }

    MemoryLevel addLevel(String levelName, int depth, LevelType levelType) {
        MemoryLevel level = new MemoryLevel(this, levelName, depth, levelType);
        miniLevels.add(level);
        return level;
    }

    /** Declares the top level of this hierarchy. */
    public MemoryLevel level(String levelName) {
        return level(levelName, LevelType.REGULAR);
    }

    /** Declares the top level of this hierarchy, with an explicit type. */
    public MemoryLevel level(String levelName, LevelType levelType) {
        if (!miniLevels.isEmpty()) {
            throw new IllegalStateException(getUniqueName() + " already has a top level");
        }
        return addLevel(levelName, 0, levelType);
    }

    /** Adds a root member on the top level. */
    public MemoryMember member(String memberName) {
        MemoryMember member = miniDimension.isMeasures()
                ? new MemoryMeasure(memberName, miniLevels.get(0), null, Member.MemberType.MEASURE)
                : new MemoryMember(memberName, miniLevels.get(0), null, Member.MemberType.REGULAR);
        if (hasAll && allMember == null) {
            allMember = member;
        }
        return member;
    }

    MemoryLevel levelAt(int depth) {
        return miniLevels.get(depth);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The base class always writes {@code [Dimension].[Hierarchy]}. MDX drops the
     * second part when the hierarchy carries the dimension's own name, and that shorter
     * form is what a reader expects to see, so this provider uses it.
     */
    @Override
    public String getUniqueName() {
        return conventionalName;
    }

    @Override
    public boolean hasAll() {
        return hasAll;
    }

    @Override
    public boolean isRagged() {
        return false;
    }

    @Override
    public List<? extends Level> getLevels() {
        return Collections.unmodifiableList(miniLevels);
    }

    @Override
    public Member getAllMember() {
        return allMember;
    }

    @Override
    public Member getDefaultMember() {
        if (allMember != null) {
            return allMember;
        }
        List<Member> top = miniLevels.get(0).getMembers();
        return top.isEmpty() ? null : top.get(0);
    }

    @Override
    public Member getNullMember() {
        if (nullMember == null) {
            nullMember = new MemoryMember("#null", miniLevels.get(0), null, Member.MemberType.NULL);
        }
        return nullMember;
    }

    @Override
    public List<Member> getRootMembers() {
        return miniLevels.get(0).getMembers();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }


    /**
     * The position of this hierarchy among all hierarchies of its cube.
     *
     * <p>The evaluator keeps one current member per hierarchy in an array indexed by this
     * number. Answering zero for every hierarchy, as a placeholder easily does, makes them
     * overwrite one another and the wrong measure is read without any error.
     */
    @Override
    public int getOrdinalInCube() {
        return ordinalInCube;
    }

    void ordinalInCube(int value) {
        this.ordinalInCube = value;
    }

    @Override
    public String getDisplayFolder() {
        return null;
    }

    @Override
    public String getUniqueNameSsas() {
        return getUniqueName();
    }

    @Override
    public String origin() {
        return "mini";
    }

    /**
     * Creates a calculated member, as a {@code WITH MEMBER} clause asks for.
     *
     * <p>It is a member of this provider like any other, except that it carries a formula
     * and therefore answers {@code isCalculated()} with {@code true}.
     */
    @Override
    public org.eclipse.daanse.olap.api.element.Member createMember(
            org.eclipse.daanse.olap.api.element.Member parent,
            org.eclipse.daanse.olap.api.element.Level level,
            String name,
            org.eclipse.daanse.olap.api.query.component.Formula formula) {
        MemoryLevel target = level != null ? (MemoryLevel) level
                : (MemoryLevel) (parent != null ? parent.getLevel() : miniLevels.get(0));
        MemoryMember member = miniDimension.isMeasures()
                ? new MemoryMeasure(name, target, (MemoryMember) parent, Member.MemberType.FORMULA)
                : new MemoryMember(name, target, (MemoryMember) parent, Member.MemberType.FORMULA);
        member.formula(formula);
        return member;
    }


    @Override
    public org.eclipse.daanse.olap.api.element.MetaData getMetaData() {
        return org.eclipse.daanse.olap.element.OlapMetaDataBase.empty();
    }

}
