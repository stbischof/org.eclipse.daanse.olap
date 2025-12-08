/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2002-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * jhyde, 3 March, 2002
 *
 * -----------------------------------------------------------------------------
 * Copied from the ICU project's DigitList class.
 *
 * Copyright (C) 1996-2011, International Business Machines Corporation and
 * others. All Rights Reserved.
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
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.ArrayTupleList;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.exceptions.MdxChildObjectNotFoundException;
import org.eclipse.daanse.olap.fun.FunUtil;
/**
 * Utilities for parsing fully-qualified member names, tuples, member lists,
 * and tuple lists.
 *
 * @author jhyde
 */
public class IdentifierParser extends org.eclipse.daanse.olap.impl.IdentifierParser {

    /**
     * Implementation of Builder that resolves segment lists to members.
     */
    public static class BuilderImpl extends MemberBuilder {
        private final CatalogReader schemaReader;
        private final Cube cube;
        protected final List<Hierarchy> hierarchyList;
        private final boolean ignoreInvalid;

        BuilderImpl(
            CatalogReader schemaReader,
            Cube cube,
            List<Hierarchy> hierarchyList)
        {
            this.schemaReader = schemaReader;
            this.cube = cube;
            this.hierarchyList = hierarchyList;
            final SystemWideProperties props = SystemWideProperties.instance();
            final boolean load = cube.isLoadInProgress();
            this.ignoreInvalid =
                (load
                    ?  cube.getCatalog().getInternalConnection().getContext()
                            .getConfigValue(ConfigConstants.IGNORE_INVALID_MEMBERS, ConfigConstants.IGNORE_INVALID_MEMBERS_DEFAULT_VALUE, Boolean.class)
                    : cube.getCatalog().getInternalConnection().getContext()
                    .getConfigValue(ConfigConstants.IGNORE_INVALID_MEMBERS_DURING_QUERY, ConfigConstants.IGNORE_INVALID_MEMBERS_DURING_QUERY_DEFAULT_VALUE, Boolean.class));
        }

        protected Member resolveMember(Hierarchy expectedHierarchy) {
            final List<Segment> segmentList =
                Util.convert(this.segmentList);
            Member member =
                (Member) Util.lookupCompound(
                    schemaReader, cube, segmentList, !ignoreInvalid,
                    DataType.MEMBER);
            if (member == null) {
                assert ignoreInvalid;
                if (expectedHierarchy != null) {
                    return expectedHierarchy.getNullMember();
                } else {
                    // Guess the intended hierarchy from the largest valid
                    // prefix.
                    for (int i = segmentList.size() - 1; i > 0; --i) {
                        List<Segment> partialName =
                            segmentList.subList(0, i);
                        OlapElement olapElement =
                            schemaReader.lookupCompound(
                                cube, partialName, false, DataType.UNKNOWN);
                        if (olapElement != null) {
                            return olapElement.getHierarchy().getNullMember();
                        }
                    }
                    throw new MdxChildObjectNotFoundException(
                        Util.implode(segmentList),
                        cube.getQualifiedName());
                }
            }
            if (expectedHierarchy != null
                && member.getHierarchy() != expectedHierarchy)
            {
                // TODO: better error
                throw Util.newInternal("member is of wrong hierarchy");
            }
            return member;
        }
    }

    /**
     * Implementation of Builder that builds a tuple.
     */
    public static class TupleBuilder extends BuilderImpl {
        protected final List<Member> memberList = new ArrayList<>();

        public TupleBuilder(
            CatalogReader schemaReader,
            Cube cube,
            List<Hierarchy> hierarchyList)
        {
            super(schemaReader, cube, hierarchyList);
        }

        @Override
		public void memberComplete() {
            super.memberComplete();
            if (memberList.size() >= hierarchyList.size()) {
                throw Util.newInternal("expected ')");
            }
            final Hierarchy hierarchy = hierarchyList.get(memberList.size());
            final Member member = resolveMember(hierarchy);
            memberList.add(member);
            segmentList.clear();
        }

        @Override
		public void tupleComplete() {
            if (memberList.size() < hierarchyList.size()) {
                throw Util.newInternal("too few members");
            }
        }
    }

    /**
     * Implementation of Builder that builds a tuple list.
     */
    public static class TupleListBuilder extends TupleBuilder {
        public final TupleList tupleList;

        public TupleListBuilder(
            CatalogReader schemaReader, Cube cube, List<Hierarchy> hierarchyList)
        {
            super(schemaReader, cube, hierarchyList);
            tupleList = new ArrayTupleList(hierarchyList.size());
        }

        @Override
		public void tupleComplete() {
            super.tupleComplete();
            if (!FunUtil.tupleContainsNullMember(memberList)) {
                tupleList.add(memberList);
            }
            this.memberList.clear();
        }
    }

    /**
     * Implementation of Builder that builds a member list.
     */
    public static class MemberListBuilder extends BuilderImpl {
        public final List<Member> memberList = new ArrayList<>();

        public MemberListBuilder(
            CatalogReader schemaReader, Cube cube, Hierarchy hierarchy)
        {
            // hierarchy may be null
            super(schemaReader, cube, Collections.singletonList(hierarchy));
        }

        @Override
		public void memberComplete() {
            final Member member = resolveMember(hierarchyList.get(0));
            if (!member.isNull()) {
                memberList.add(member);
            }
            segmentList.clear();
        }

        @Override
        public void tupleComplete() {
            // nothing to do
        }
    }
}
