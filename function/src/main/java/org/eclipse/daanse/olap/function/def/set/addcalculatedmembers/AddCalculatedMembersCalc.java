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
package org.eclipse.daanse.olap.function.def.set.addcalculatedmembers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.profile.ProfilingCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.fun.DaanseEvaluationException;
import org.eclipse.daanse.olap.function.def.set.level.LevelMembersCalc;

public class AddCalculatedMembersCalc extends AbstractNestedTupleListCalc {

    protected AddCalculatedMembersCalc(Type type, final TupleListCalc tupleListCalc) {
        super(type, tupleListCalc);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleListCalc tc = getChildCalc(0, TupleListCalc.class);
        final TupleList list = tc.evaluate(evaluator);
        if ( ProfilingCalc.unwrap(tc) instanceof LevelMembersCalc ) {
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
            return new UnaryTupleList(addCalculatedMembers(result.slice(0), evaluator));
        }
        return new UnaryTupleList(addCalculatedMembers(list.slice(0), evaluator));
    }

    private boolean notHaveparents(TupleList tl, List<Member> o) {
        boolean parentIsNull = o.stream().noneMatch(it -> (it.getParentMember() != null));
        if (!parentIsNull) {
            List<Member> parents = o.stream().filter(m -> (m.getParentMember() != null)).map(it -> it.getParentMember()).toList();
            boolean nothaveParent = tl.stream().allMatch(it -> (it.stream().noneMatch(m -> parents.contains(m))));
            return nothaveParent;
        } else {
            return true;
        }
    }

    private List<Member> addCalculatedMembers(List<Member> memberList, Evaluator evaluator) {
        // Determine unique levels in the set
        final Set<Level> levels = new LinkedHashSet<>();
        Hierarchy hierarchy = null;

        for (Member member : memberList) {
            if (hierarchy == null) {
                hierarchy = member.getHierarchy();
            } else if (hierarchy != member.getHierarchy()) {
                throw new DaanseEvaluationException(
                        new StringBuilder("Only members from the same hierarchy are allowed in the ")
                                .append("AddCalculatedMembers set: ").append(hierarchy).append(" vs ")
                                .append(member.getHierarchy()).toString());
            }
            levels.add(member.getLevel());
        }

        // For each level, add the calculated members from both
        // the schema and the query
        List<Member> workingList = new ArrayList<>(memberList);
        final CatalogReader schemaReader = evaluator.getQuery().getCatalogReader(true);
        for (Level level : levels) {
            List<Member> calcMemberList = schemaReader.getCalculatedMembers(level);
            for (Member calcMember : calcMemberList) {
                Member parentMember = calcMember.getParentMember();
                if (parentMember == null || memberList.stream().anyMatch(m -> m.getParentMember() != null
                        && m.getParentMember().getUniqueName().equals(parentMember.getUniqueName()))) {
                    workingList.add(calcMember);
                }
            }
        }
        return workingList;
    }

}
