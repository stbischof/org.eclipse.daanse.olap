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
import org.eclipse.daanse.olap.element.LevelBase;

/** A level the provider holds in memory. */
public class MemoryLevel extends LevelBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLevel.class);

    private final MemoryHierarchy miniHierarchy;
    private MemoryLevel childLevel;

    MemoryLevel(MemoryHierarchy hierarchy, String name, int depth, LevelType levelType) {
        super(hierarchy, name, name, true, null, depth, levelType);
        this.miniHierarchy = hierarchy;
        this.members = new ArrayList<>();
    }

    /** Adds the level one step below this one. */
    public MemoryLevel level(String levelName) {
        return level(levelName, LevelType.REGULAR);
    }

    /** Adds the level one step below this one, with an explicit type. */
    public MemoryLevel level(String levelName, LevelType levelType) {
        if (childLevel != null) {
            throw new IllegalStateException(
                    getUniqueName() + " already has a level below it: " + childLevel.getUniqueName());
        }
        childLevel = miniHierarchy.addLevel(levelName, getDepth() + 1, levelType);
        return childLevel;
    }

    MemoryLevel childLevel() {
        return childLevel;
    }

    void add(MemoryMember member) {
        member.ordinal(members.size());
        members.add(member);
    }

    @Override
    public List<Member> getMembers() {
        return Collections.unmodifiableList(members);
    }

    @Override
    public Level getChildLevel() {
        return childLevel;
    }

    @Override
    public Level getParentLevel() {
        return getDepth() == 0 ? null : miniHierarchy.levelAt(getDepth() - 1);
    }

    @Override
    public boolean isAll() {
        return getLevelType() == LevelType.REGULAR && getDepth() == 0 && miniHierarchy.hasAll();
    }

    @Override
    public int getCardinality() {
        return members.size();
    }

    @Override
    public int getApproxRowCount() {
        return members.size();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }


    @Override
    public boolean isParentChild() {
        return false;
    }

    @Override
    public boolean isParentAsLeafEnable() {
        return false;
    }

    @Override
    public String getParentAsLeafNameFormat() {
        return null;
    }

    @Override
    public boolean areMembersUnique() {
        return true;
    }


    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Property[] getProperties() {
        return new org.eclipse.daanse.olap.api.element.Property[0];
    }

    @Override
    public org.eclipse.daanse.olap.api.element.Property[] getInheritedProperties() {
        return new org.eclipse.daanse.olap.api.element.Property[0];
    }

    @Override
    public org.eclipse.daanse.olap.api.element.MetaData getMetaData() {
        return org.eclipse.daanse.olap.element.OlapMetaDataBase.empty();
    }

}
