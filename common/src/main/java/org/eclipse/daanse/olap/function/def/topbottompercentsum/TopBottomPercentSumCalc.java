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
package org.eclipse.daanse.olap.function.def.topbottompercentsum;

import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.NullSemantics;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractProfilingNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.fun.sort.Sorter;

public class TopBottomPercentSumCalc extends AbstractProfilingNestedTupleListCalc {
    private boolean top;
    private boolean percent;

    public TopBottomPercentSumCalc(Type type, TupleListCalc tupleListCalc, DoubleCalc doubleCalc, Calc<?> calc,
            boolean top, boolean percent) {
        super(type, tupleListCalc, doubleCalc, calc);
        this.top = top;
        this.percent = percent;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleListCalc tupleListCalc = getChildCalc(0, TupleListCalc.class);
        DoubleCalc doubleCalc = getChildCalc(1, DoubleCalc.class);
        Calc<?> calc = getChildCalc(2, Calc.class);
        TupleList list = tupleListCalc.evaluate(evaluator);
        Double target = doubleCalc.evaluate(evaluator);
        if (list.isEmpty()) {
            return list;
        }
        if (target == null) {
            // A NULL target behaves like 0 .
            target = 0.0;
        }
        Map<List<Member>, Object> mapMemberToValue = Sorter.evaluateTuples(evaluator, calc, list);
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            list = Sorter.sortTuples(evaluator, list, list, calc, top, true, getType().getArity());
        } finally {
            evaluator.restore(savepoint);
        }
        if (percent) {
            FunUtil.toPercent(list, mapMemberToValue);
        }
        double runningTotal = 0;
        int memberCount = list.size();
        int nullCount = 0;
        for (int i = 0; i < memberCount; i++) {
            if (runningTotal >= target) {
                list = list.subList(0, i);
                break;
            }
            final List<Member> key = list.get(i);
            final Object o = mapMemberToValue.get(key);
            if (NullSemantics.isNull(o)) {
                nullCount++;
            } else if (o instanceof Number n) {
                runningTotal += n.doubleValue();
            } else if (o == NotLoaded.INSTANCE) {
                // dirty batching pass, results discarded - the legacy
                // Double(0) marker used to add 0 to the running total here
            } else if (o instanceof Exception) {
                // ignore the error
            } else {
                throw Util.newInternal(new StringBuilder("got ").append(o).append(" when expecting Number").toString());
            }
        }

        // MSAS exhibits the following behavior. If the value of all members
        // is null, then the first (or last) member of the set is returned
        // for percent operations.
        if (memberCount > 0 && percent && nullCount == memberCount) {
            return top ? list.subList(0, 1) : list.subList(memberCount - 1, memberCount);
        }
        return list;
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }
}
