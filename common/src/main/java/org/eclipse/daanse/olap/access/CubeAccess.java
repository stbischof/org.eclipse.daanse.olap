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


package org.eclipse.daanse.olap.access;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.exceptions.MdxCubeSlicerHierarchyErrorException;
import org.eclipse.daanse.olap.exceptions.MdxCubeSlicerMemberErrorException;
import org.eclipse.daanse.olap.query.component.IdImpl;
/**
 * This class implements object of type GrantCube to apply permissions
 * on user's MDX query
 *
 * @author lkrivopaltsev, 01 November, 1999
 */
public class CubeAccess {

    private boolean hasRestrictions;
    /** array of hierarchies with no access */
    private Hierarchy[] noAccessHierarchies;
    /** array of members which have limited access */
    private Member[]  limitedMembers;
    private final List<Hierarchy> hierarchyList = new ArrayList<>();
    private final List<Member> memberList = new ArrayList<>();
    private final Cube mdxCube;

    /**
     * Creates a CubeAccess object.
     *
     * User's code should be responsible for
     * filling cubeAccess with restricted hierarchies and restricted
     * members by calling addSlicer(). Do NOT forget to call
     * {@link #normalizeCubeAccess()} after you done filling cubeAccess.
     */
    public CubeAccess(Cube mdxCube) {
        this.mdxCube = mdxCube;
        noAccessHierarchies = null;
        limitedMembers = null;
        hasRestrictions = false;
    }

    public boolean hasRestrictions() {
        return hasRestrictions;
    }

    public Hierarchy[] getNoAccessHierarchies() {
        return noAccessHierarchies;
    }

    public Member[] getLimitedMembers() {
        return limitedMembers;
    }

    public List<Hierarchy> getNoAccessHierarchyList() {
        return hierarchyList;
    }

    public List<Member> getLimitedMemberList() {
        return memberList;
    }

    public boolean isHierarchyAllowed(Hierarchy mdxHierarchy) {
        String hierName = mdxHierarchy.getUniqueName();
        if (noAccessHierarchies == null || hierName == null) {
            return true;
        }
        for (Hierarchy noAccessHierarchy : noAccessHierarchies) {
            if (hierName.equalsIgnoreCase(noAccessHierarchy.getUniqueName())) {
                return false;
            }
        }
        return true;
    }

    public Member getLimitedMemberForHierarchy(Hierarchy mdxHierarchy) {
        String hierName = mdxHierarchy.getUniqueName();
        if (limitedMembers == null || hierName == null) {
            return null;
        }
        for (Member limitedMember : limitedMembers) {
            Hierarchy limitedHierarchy = limitedMember.getHierarchy();
            if (hierName.equalsIgnoreCase(limitedHierarchy.getUniqueName())) {
                return limitedMember;
            }
        }
        return null;
    }

    /**
     * Adds  restricted hierarchy or limited member based on bMember
     */
    public void addGrantCubeSlicer(
        String sHierarchy,
        String sMember,
        boolean bMember)
    {
        if (bMember) {
            boolean fail = false;
            List<Segment> sMembers = Util.parseIdentifier(sMember);
            CatalogReader schemaReader = mdxCube.getCatalogReader(null);
            Member member = schemaReader.getMemberByUniqueName(sMembers, fail);
            if (member == null) {
                throw new MdxCubeSlicerMemberErrorException(
                    sMember, sHierarchy, mdxCube.getUniqueName());
            }
            // there should be only slicer per hierarchy; ignore the rest
            if (getLimitedMemberForHierarchy(member.getHierarchy()) == null) {
                memberList.add(member);
            }
        } else {
            boolean fail = false;
            Hierarchy hierarchy =
                mdxCube.lookupHierarchy(
                    new IdImpl.NameSegmentImpl(sHierarchy),
                    fail);
            if (hierarchy == null) {
                throw new MdxCubeSlicerHierarchyErrorException(sHierarchy, mdxCube.getUniqueName());
            }
            hierarchyList.add(hierarchy);
        }
    }

    /**
     * Initializes internal arrays of restricted hierarchies and limited
     * members. It has to be called  after all 'addSlicer()' calls.
     */
    public void normalizeCubeAccess() {
        if (!memberList.isEmpty()) {
            limitedMembers = memberList.toArray(Member[]::new);
            hasRestrictions = true;
        }
        if (!hierarchyList.isEmpty()) {
            noAccessHierarchies = hierarchyList.toArray(Hierarchy[]::new);
            hasRestrictions = true;
        }
    }

    @Override
	public boolean equals(Object object) {
        if (!(object instanceof CubeAccess cubeAccess)) {
            return false;
        }
        List<Hierarchy> hierarchyListInner = cubeAccess.getNoAccessHierarchyList();
        List<Member> limitedMemberList = cubeAccess.getLimitedMemberList();

        if ((this.hierarchyList.size() != hierarchyListInner.size())
            || (this.memberList.size() != limitedMemberList.size()))
        {
            return false;
        }
        for (Hierarchy o : hierarchyListInner) {
            if (!this.hierarchyList.contains(o)) {
                return false;
            }
        }
        for (Member member : limitedMemberList) {
            if (!this.memberList.contains(member)) {
                return false;
            }
        }
        return true;
    }

    @Override
	public int hashCode() {
        int h = mdxCube.hashCode();
        h = Util.hash(h, hierarchyList);
        h = Util.hash(h, memberList);
        h = Util.hashArray(h, noAccessHierarchies);
        h = Util.hashArray(h, limitedMembers);
        return h;
    }
}
