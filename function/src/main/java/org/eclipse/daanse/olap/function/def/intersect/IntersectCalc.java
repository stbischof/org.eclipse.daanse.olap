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
package org.eclipse.daanse.olap.function.def.intersect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.VisualTotalMember;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;

public class IntersectCalc extends AbstractNestedTupleListCalc {

    private final boolean all;
    private final int arity;

    public IntersectCalc(Type type, TupleListCalc listCalc1, TupleListCalc listCalc2, final boolean all,
            final int arity) {
        super(type, listCalc1, listCalc2);
        this.all = all;
        this.arity = arity;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleList leftList = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
        if (leftList.isEmpty()) {
            return leftList;
        }
        final TupleList rightList = getChildCalc(1, TupleListCalc.class).evaluate(evaluator);
        if (rightList.isEmpty()) {
            return rightList;
        }

        // Set of members from the right side of the intersect.
        // We use a RetrievableSet because distinct keys
        // (regular members and visual totals members) compare
        // identical using hashCode and equals, we want to retrieve
        // the actual key, and java.util.Set only has containsKey.
        RetrievableSet<List<Member>> rightSet = new RetrievableHashSet<>(rightList.size() * 3 / 2);
        for (List<Member> tuple : rightList) {
            rightSet.add(tuple);
        }

        final TupleList result = TupleCollections.createList(arity, Math.min(leftList.size(), rightList.size()));
        final Set<List<Member>> resultSet = all ? null : new HashSet<>();
        for (List<Member> leftTuple : leftList) {
            List<Member> rightKey = rightSet.getKey(leftTuple);
            if (rightKey == null) {
                continue;
            }
            if (resultSet != null && !resultSet.add(leftTuple)) {
                continue;
            }
            result.add(copyTupleWithVisualTotalsMembersOverriding(leftTuple, rightKey));
        }
        return result;
    }

    /**
     * Constructs a tuple consisting of members from {@code leftTuple}, but
     * overridden by any corresponding members from {@code rightKey} that happen to
     * be visual totals members.
     *
     *
     * Returns the original tuple if there are no visual totals members on the RHS.
     *
     * @param leftTuple Original tuple
     * @param rightKey  Right tuple
     * @return Copy of original tuple, with any VisualTotalMembers from right tuple
     *         overriding
     */
    private List<Member> copyTupleWithVisualTotalsMembersOverriding(List<Member> leftTuple, List<Member> rightKey) {
        List<Member> tuple = leftTuple;
        for (int i = 0; i < rightKey.size(); i++) {
            Member member = rightKey.get(i);
            if (!(tuple.get(i) instanceof VisualTotalMember)
                    && member instanceof VisualTotalMember) {
                if (tuple == leftTuple) {
                    // clone on first VisualTotalMember -- to avoid
                    // alloc/copy in the common case where there are
                    // no VisualTotalMembers
                    tuple = new ArrayList<>(leftTuple);
                }
                tuple.set(i, member);
            }
        }
        return tuple;
    }

}
