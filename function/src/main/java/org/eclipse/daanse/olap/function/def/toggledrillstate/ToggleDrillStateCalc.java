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
package org.eclipse.daanse.olap.function.def.toggledrillstate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class ToggleDrillStateCalc extends AbstractNestedTupleListCalc{

    public ToggleDrillStateCalc(Type type, TupleListCalc listCalc0, TupleListCalc listCalc1) {
        super(type, listCalc0, listCalc1);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleListCalc listCalc0 = getChildCalc(0, TupleListCalc.class);
        TupleListCalc listCalc1 = getChildCalc(1, TupleListCalc.class);
        final TupleList list0 = listCalc0.evaluate(evaluator);
        final TupleList list1 = listCalc1.evaluate(evaluator);
        return toggleDrillStateTuples(evaluator, list0, list1);
    }

    private TupleList toggleDrillStateTuples(
            Evaluator evaluator, TupleList v0, TupleList list1)
        {
            assert list1.getArity() == 1;
            if (list1.isEmpty()) {
                return v0;
            }
            if (v0.isEmpty()) {
                return v0;
            }
            final Member[] members = new Member[v0.getArity()]; // tuple workspace
            final Set<Member> set = new HashSet<>(list1.slice(0));
            TupleList result = v0.copyList((v0.size() * 3) / 2 + 1); // allow 50%
            int i = 0, n = v0.size();
            while (i < n) {
                List<Member> o = v0.get(i++);
                result.add(o);
                Member m = null;
                int k = -1;
                for (int j = 0; j < o.size(); j++) {
                    Member member = o.get(j);
                    if (set.contains(member)) {
                        k = j;
                        m = member;
                        break;
                    }
                }
                if (k == -1) {
                    continue;
                }
                boolean isDrilledDown = false;
                if (i < n) {
                    List<Member> next = v0.get(i);
                    Member nextMember = next.get(k);
                    boolean strict = true;
                    if (FunUtil.isAncestorOf(m, nextMember, strict)) {
                        isDrilledDown = true;
                    }
                }
                if (isDrilledDown) {
                    // skip descendants of this member
                    do {
                        List<Member> next = v0.get(i);
                        Member nextMember = next.get(k);
                        boolean strict = true;
                        if (FunUtil.isAncestorOf(m, nextMember, strict)) {
                            i++;
                        } else {
                            break;
                        }
                    } while (i < n);
                } else {
                    List<Member> children =
                        evaluator.getCatalogReader().getMemberChildren(m);
                    for (Member child : children) {
                        o.toArray(members);
                        members[k] = child;
                        result.addTuple(members);
                    }
                }
            }
            return result;
        }

}
