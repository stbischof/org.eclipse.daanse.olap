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
package org.eclipse.daanse.olap.function.def.set.existing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.fun.FunUtil;

public class ExistingCalc extends AbstractNestedTupleListCalc {

    private Type myType;

    protected ExistingCalc(Type type, final TupleIteratorCalc setArg, final Type myType) {
        super(type, setArg);
        this.myType = myType;
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return myType.usesHierarchy(hierarchy, false);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleIterable setTuples = (TupleIterable) getChildCalc(0, TupleIteratorCalc.class).evaluate(evaluator);

        TupleList result = TupleCollections.createList(setTuples.getArity());
        List<Member> contextMembers = Arrays.asList(evaluator.getMembers());

        List<Hierarchy> argDims = null;
        List<Hierarchy> contextDims = getHierarchies(contextMembers);

        for (List<Member> tuple : setTuples) {
            if (argDims == null) {
                argDims = getHierarchies(tuple);
            }
            if (FunUtil.existsInTuple(tuple, contextMembers, argDims, contextDims, evaluator)) {
                result.add(tuple);
            }
        }
        return result;
    }

    private static List<Hierarchy> getHierarchies(final List<Member> members) {
        List<Hierarchy> hierarchies = new ArrayList<>(members.size());
        for (Member member : members) {
            hierarchies.add(member.getHierarchy());
        }
        return hierarchies;
    }

}
