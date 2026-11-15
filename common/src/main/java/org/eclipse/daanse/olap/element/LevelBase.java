/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
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


package org.eclipse.daanse.olap.element;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.KeyMember;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.LevelType;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.formatter.MemberFormatter;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.Quoting;
import org.eclipse.daanse.olap.common.Util;
/**
 * Skeleton implementation of {@link Level}.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public abstract class LevelBase
    extends OlapElementBase
    implements Level
{
    protected final Hierarchy hierarchy;
    protected final String name;
    protected final String uniqueName;
    protected final String description;
    protected final int depth;
    protected final LevelType levelType;
    protected MemberFormatter memberFormatter;
    protected int  approxRowCount;
    protected int cardinality = 0;
    protected List<Member> members;
    private final static String mdxLevelName = "level ''{0}''";

    protected LevelBase(
        Hierarchy hierarchy,
        String name,
        String caption,
        boolean visible,
        String description,
        int depth,
        LevelType levelType)
    {
        this.hierarchy = hierarchy;
        this.name = name;
        this.caption = caption;
        this.visible = visible;
        this.description = description;
        this.uniqueName = Util.makeFqName(hierarchy, name);
        this.depth = depth;
        this.levelType = levelType;
    }

    /**
     * Sets the approximate number of members in this Level.
     * @see #getApproxRowCount()
     */
    public void setApproxRowCount(int approxRowCount) {
        this.approxRowCount = approxRowCount;
    }

    // from Element
    @Override
	public String getQualifiedName() {
        return MessageFormat.format(mdxLevelName, getUniqueName());
    }

    @Override
	public LevelType getLevelType() {
        return levelType;
    }

    @Override
	public String getUniqueName() {
        return uniqueName;
    }

    @Override
	public String getName() {
        return name;
    }

    @Override
	public String getDescription() {
        return description;
    }

    @Override
	public Hierarchy getHierarchy() {
        return hierarchy;
    }

    @Override
	public Dimension getDimension() {
        return hierarchy.getDimension();
    }

    @Override
	public int getDepth() {
        return depth;
    }

    @Override
	public Level getChildLevel() {
        int childDepth = depth + 1;
        List<? extends Level> levels = hierarchy.getLevels();
        return (childDepth < levels.size())
            ? levels.get(childDepth)
            : null;
    }

    @Override
	public Level getParentLevel() {
        int parentDepth = depth - 1;
        List<? extends Level> levels = hierarchy.getLevels();
        return (parentDepth >= 0)
            ? levels.get(parentDepth)
            : null;
    }

    @Override
	public abstract boolean isAll();

    public boolean isMeasure() {
        return hierarchy.getName().equals("Measures");
    }

    @Override
	public OlapElement lookupChild(
        CatalogReader schemaReader, Segment s, MatchType matchType)
    {
        if (s.getQuoting() == Quoting.KEY) {
            return memberByKey(schemaReader, s);
        }
        if (areMembersUnique()
            && s instanceof NameSegment nameSegment)
        {
            return Util.lookupHierarchyRootMember(
                schemaReader, hierarchy, nameSegment, matchType);
        } else {
            return null;
        }
    }

    /**
     * The member of this level that carries the key a segment names, the way {@code &[7]} names the
     * key 7. A key of several parts names no member of one level, and saying so beats answering that
     * a member of that name is missing.
     *
     * @param schemaReader the reader that hands out the members
     * @param key the segment, a key by its quoting
     * @return the member carrying that key, or null when no member does
     */
    private OlapElement memberByKey(CatalogReader schemaReader, Segment key) {
        List<NameSegment> parts = key.getKeyParts();
        if (parts == null || parts.size() != 1) {
            throw new OlapRuntimeException(
                "a compound key is not resolved: " + key + " of level " + getUniqueName());
        }
        String wanted = parts.get(0).getName();
        for (Member member : schemaReader.getLevelMembers(this, false)) {
            if (member instanceof KeyMember keyed && keyed.getKey() != null
                && String.valueOf(keyed.getKey()).equals(wanted))
            {
                return member;
            }
        }
        return null;
    }

    @Override
	public MemberFormatter getMemberFormatter() {
        return memberFormatter;
    }
}


// End LevelBase.java
