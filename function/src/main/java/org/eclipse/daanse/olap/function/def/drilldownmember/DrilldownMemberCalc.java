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
package org.eclipse.daanse.olap.function.def.drilldownmember;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.ParentChildMember;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.function.def.set.level.LevelMembersCalc;

public class DrilldownMemberCalc extends AbstractNestedTupleListCalc {

    private final boolean recursive;

    public DrilldownMemberCalc(final Type type, final TupleListCalc listCalc1, final TupleListCalc listCalc2,
            final boolean recursive) {
        super(type, listCalc1, listCalc2);
        this.recursive = recursive;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleListCalc tc = getChildCalc(0, TupleListCalc.class);
        TupleList list1;
        TupleList list = tc.evaluate(evaluator);
        if ( tc instanceof LevelMembersCalc ) {
            TupleList result = TupleCollections.createList(list.getArity());
            int i = 0;
            int n = list.size();
            final Member[] members = new Member[list.getArity()];
            while (i < n) {
                List<Member> o = list.get(i++);
                o.toArray(members);
                if (notHaveparents(list, o)) {
                    result.add(o);
                }
            }
            list1 = result;
        } else {
            list1 = list;
        }
        final TupleList list2 = getChildCalc(1, TupleListCalc.class).evaluate(evaluator);
        return drilldownMember(list1, list2, evaluator);
    }

    /**
     * Drills down an element.
     *
     *
     * Algorithm: If object is present in {@code memberSet} adds to result children
     * of the object. If flag {@code recursive} is set then this method is called
     * recursively for the children.
     *
     * @param evaluator  Evaluator
     * @param tuple      Tuple (may have arity 1)
     * @param memberSet  Set of members
     * @param resultList Result
     */
    protected void drillDownObj(Evaluator evaluator, Member[] tuple, Set<Member> memberSet, TupleList resultList) {
        for (int k = 0; k < tuple.length; k++) {
            Member member = tuple[k];
            if (memberSet.contains(member)) {
                List<Member> children = evaluator.getCatalogReader().getMemberChildren(member);
                final Member[] tuple2 = tuple.clone();
                if (tuple[k].getLevel().isParentAsLeafEnable() && children.size() > 0) {
                    final Member[] t = tuple.clone();
                    String name;
                    if (tuple[k].getLevel().getParentAsLeafNameFormat() != null) {
                        name = String.format(tuple[k].getLevel().getParentAsLeafNameFormat(), member.getName());
                    } else {
                        name = member.getName();
                    }
                    t[k] = ((ParentChildMember)member).createPseudoLeafMember(children.get(0), name);
                    resultList.addTuple(t);
                }
                for (Member childMember : children) {
                    tuple2[k] = childMember;
                    resultList.addTuple(tuple2);
                    if (recursive) {
                        drillDownObj(evaluator, tuple2, memberSet, resultList);
                    }
                }
                break;
            }
        }
    }

    private TupleList drilldownMember(TupleList v0, TupleList v1, Evaluator evaluator) {
        assert v1.getArity() == 1;
        if (v0.isEmpty() || v1.isEmpty()) {
            return v0;
        }

        Set<Member> set1 = new HashSet<>(v1.slice(0));

        TupleList result = TupleCollections.createList(v0.getArity());
        int i = 0;
        int n = v0.size();
        final Member[] members = new Member[v0.getArity()];
        while (i < n) {
            List<Member> o = v0.get(i++);
            o.toArray(members);
            result.add(o);
            drillDownObj(evaluator, members, set1, result);
        }
        return result;
    }

    private boolean notHaveparents(TupleList tl, List<Member> o) {
        boolean parentIsNull = o.stream().noneMatch(it -> (it.getParentMember() != null));
        if (!parentIsNull) {
            List<Member> parents = o.stream().filter(m -> (m.getParentMember() != null)).map(it -> it.getParentMember()).toList();
            boolean nothaveParent = tl.stream().noneMatch(it -> (it.stream().noneMatch(m -> parents.contains(m))));
            return nothaveParent;
        } else {
            return true;
        }
    }

}
