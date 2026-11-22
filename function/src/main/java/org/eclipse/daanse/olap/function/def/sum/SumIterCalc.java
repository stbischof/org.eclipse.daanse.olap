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
package org.eclipse.daanse.olap.function.def.sum;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.core.AbstractAggregateFunDef;

public class SumIterCalc extends AbstractNestedDoubleCalc {

    public SumIterCalc(Type type, TupleIteratorCalc tupleIteratorCalc, Calc<?> calc) {
        super(type, tupleIteratorCalc, calc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        evaluator.getTiming().markStart(SumFunDef.TIMING_NAME);
        final int savepoint = evaluator.savepoint();
        try {
            TupleIterable iterable = evaluateCurrentIterable(getChildCalc(0, TupleIteratorCalc.class), evaluator);
            return FunUtil.sumDouble(evaluator, iterable, getChildCalc(1, Calc.class));
        } finally {
            evaluator.restore(savepoint);
            evaluator.getTiming().markEnd(SumFunDef.TIMING_NAME);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }

    private TupleIterable evaluateCurrentIterable(TupleIteratorCalc<?> tupleIteratorCalc, Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        int currLen = 0;
        TupleIterable iterable;
        try {
            evaluator.setNonEmpty(false);
            iterable = tupleIteratorCalc.evaluate(evaluator);
        } finally {
            evaluator.restore(savepoint);
        }
        AbstractAggregateFunDef.crossProd(evaluator, currLen);
        return iterable;
    }
}
