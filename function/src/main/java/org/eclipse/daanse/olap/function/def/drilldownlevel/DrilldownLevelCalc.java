/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.drilldownlevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.fun.FunUtil;

public class DrilldownLevelCalc extends AbstractNestedTupleListCalc{
    private final boolean includeCalcMembers;
    
    public DrilldownLevelCalc(Type type, TupleListCalc tupleListCalc, LevelCalc levelCalc, final boolean includeCalcMembers) {
        super(type, tupleListCalc, levelCalc);
        this.includeCalcMembers = includeCalcMembers;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleListCalc tupleListCalc = getChildCalc(0, TupleListCalc.class);
        LevelCalc levelCalc = getChildCalc(1, LevelCalc.class);
        TupleList list = tupleListCalc.evaluate(evaluator);
        if (list.isEmpty()) {
            return list;
        }
        int searchDepth = -1;
        if (levelCalc != null) {
            Level level = levelCalc.evaluate(evaluator);
            searchDepth = level.getDepth();
        }
        return new UnaryTupleList(
            drill(searchDepth, list.slice(0), evaluator, includeCalcMembers));
    }

    private List<Member> drill(int searchDepth, List<Member> list, Evaluator evaluator, boolean includeCalcMembers)
    {
        HashMap<Member,List<Member>> calcMembersByParent = getCalcMembersByParent(
                list.get(0).getHierarchy(),
                evaluator,
                includeCalcMembers);

        if (searchDepth == -1) {
            searchDepth = list.get(0).getLevel().getDepth();

            for (int i = 1, m = list.size(); i < m; i++) {
                Member member = list.get(i);
                int memberDepth = member.getLevel().getDepth();

                if (memberDepth > searchDepth) {
                    searchDepth = memberDepth;
                }
            }
        }

        List<Member> drilledSet = new ArrayList<>();

        List<Member> parentMembers = new ArrayList<>();

        for (int i = 0, m = list.size(); i < m; i++) {
            Member member = list.get(i);
            drilledSet.add(member);

            Member nextMember =
                i == (m - 1)
                ? null
                : list.get(i + 1);

            //
            // This member is drilled if it's at the correct depth
            // and if it isn't drilled yet. A member is considered
            // to be "drilled" if it is immediately followed by
            // at least one descendant
            //
            if (member.getLevel().getDepth() == searchDepth
                && !FunUtil.isAncestorOf(member, nextMember, true))
            {
                parentMembers.add(member);
            }
        }

        for(Member parentMember: parentMembers) {
            List<Member> childMembers =
                    evaluator.getCatalogReader().getMemberChildren(parentMember);
            for (Member childMember : childMembers) {
                drilledSet.add(childMember);
            }
            List<Member> childrenCalcMembers = calcMembersByParent.get(parentMember);
            if(childrenCalcMembers != null) {
                for (Member childMember : childrenCalcMembers) {
                    drilledSet.add(childMember);
                }
            }
        }

        return drilledSet;
    }

    static HashMap<Member,List<Member>> getCalcMembersByParent(Hierarchy hierarchy, Evaluator evaluator, boolean includeCalcMembers) {
        List<Member> calculatedMembers;
        if(includeCalcMembers) {
            final CatalogReader schemaReader =
                    evaluator.getCatalogReader();
            calculatedMembers = schemaReader.getCalculatedMembers(hierarchy);
        }
        else {
            calculatedMembers = new ArrayList<>();
        }
        HashMap<Member,List<Member>> calcMembersByParent = new HashMap<>();
        for(Member member: calculatedMembers) {
            if(member.getParentMember() != null) {
                List<Member> children = calcMembersByParent.get(member.getParentMember());
                if(children == null) {
                    children = new ArrayList<>();
                    calcMembersByParent.put(member.getParentMember(), children);
                }
                children.add(member);
            }
        }
        return calcMembersByParent;
    }

}
