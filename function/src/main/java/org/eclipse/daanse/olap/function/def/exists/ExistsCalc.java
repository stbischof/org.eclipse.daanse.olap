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
package org.eclipse.daanse.olap.function.def.exists;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.fun.FunUtil;

public class ExistsCalc extends AbstractNestedTupleListCalc {

    public ExistsCalc(Type type, TupleListCalc listCalc1, TupleListCalc listCalc2) {
        super(type, listCalc1, listCalc2);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleList leftTuples = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
        if (leftTuples.isEmpty()) {
            return TupleCollections.emptyList(leftTuples.getArity());
        }
        TupleList rightTuples = getChildCalc(1, TupleListCalc.class).evaluate(evaluator);
        if (rightTuples.isEmpty()) {
            return TupleCollections.emptyList(leftTuples.getArity());
        }
        TupleList result = TupleCollections.createList(leftTuples.getArity());

        List<Hierarchy> leftDims = getHierarchies(leftTuples.get(0));
        List<Hierarchy> rightDims = getHierarchies(rightTuples.get(0));

        leftLoop: for (List<Member> leftTuple : leftTuples) {
            for (List<Member> rightTuple : rightTuples) {
                if (FunUtil.existsInTuple(leftTuple, rightTuple, leftDims, rightDims, null)) {
                    result.add(leftTuple);
                    continue leftLoop;
                }
            }
        }
        return result;
    }

    private static List<Hierarchy> getHierarchies(final List<Member> members) {
        List<Hierarchy> hierarchies = new ArrayList<>();
        for (Member member : members) {
            hierarchies.add(member.getHierarchy());
        }
        return hierarchies;
    }

}
