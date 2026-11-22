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
package org.eclipse.daanse.olap.function.def.aggregate.children;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.aggregator.Aggregator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.fun.FunUtil;

public class AggregateChildrenCalc extends AbstractNestedUnknownCalc {

    protected AggregateChildrenCalc(Type type, HierarchyCalc hierarchyCalc, CurrentValueUnknownCalc valueCalc) {
        super(type, hierarchyCalc, valueCalc);
    }

    @Override
    public Object evaluateInternal(Evaluator evaluator) {
        Hierarchy hierarchy = getChildCalc(0, HierarchyCalc.class).evaluate(evaluator);
        return aggregateChildren(evaluator, hierarchy, getChildCalc(1, CurrentValueUnknownCalc.class));
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return new Calc[] { getChildCalc(0, HierarchyCalc.class), getChildCalc(1, CurrentValueUnknownCalc.class) };
    }

    Object aggregateChildren(Evaluator evaluator, Hierarchy hierarchy, final Calc<?> valueFunCall) {
        Member member = evaluator.getPreviousContext(hierarchy);
        List<Member> members = new ArrayList<>();
        if (member == null) {
            member = hierarchy.getDefaultMember();
        }
        if (member != null) {
            evaluator.getCatalogReader().getParentChildContributingChildren(member.getDataMember(), hierarchy, members);
        }
        Aggregator aggregator = (Aggregator) evaluator.getProperty(StandardProperty.AGGREGATION_TYPE.getName(), null);
        if (aggregator == null) {
            throw FunUtil.newEvalException(null, new StringBuilder("Could not find an aggregator in the current ")
                    .append("evaluation context").toString());
        }
        Aggregator rollup = aggregator.getRollup();
        if (rollup == null) {
            throw FunUtil.newEvalException(null, new StringBuilder("Don't know how to rollup aggregator '")
                    .append(aggregator).append("'").toString());
        }
        final int savepoint = evaluator.savepoint();
        try {
            return rollup.aggregate(evaluator, new UnaryTupleList(members), valueFunCall);
        } finally {
            evaluator.restore(savepoint);
        }
    }

}
