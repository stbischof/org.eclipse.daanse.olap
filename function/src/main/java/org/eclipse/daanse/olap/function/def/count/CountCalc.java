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
package org.eclipse.daanse.olap.function.def.count;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedIntegerCalc;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.core.AbstractAggregateFunDef;

public class CountCalc extends AbstractNestedIntegerCalc {

    private final boolean includeEmpty;

    protected CountCalc(Type type, Calc<?> calc, final boolean includeEmpty) {
        super(type, calc);
        this.includeEmpty = includeEmpty;
    }

    @Override
    public Integer evaluateInternal(Evaluator evaluator) {
        evaluator.getTiming().markStart(CountFunDef.TIMING_NAME);
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            final int count;
            Calc<?> calc = getChildCalc(0, Calc.class);
            if (calc instanceof TupleIteratorCalc tupleIteratorCalc) {
                TupleIterable iterable = evaluateCurrentIterable(tupleIteratorCalc, evaluator);
                count = FunUtil.count(evaluator, iterable, includeEmpty);
            } else {
                // must be TupleListCalc
                TupleListCalc tupleListCalc = (TupleListCalc) calc;
                TupleList list = AbstractAggregateFunDef.evaluateCurrentList(tupleListCalc, evaluator);
                count = FunUtil.count(evaluator, list, includeEmpty);
            }
            return count;
        } finally {
            evaluator.restore(savepoint);
            evaluator.getTiming().markEnd(CountFunDef.TIMING_NAME);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        // COUNT(<set>, INCLUDEEMPTY) is straightforward -- it
        // depends only on the dimensions that <Set> depends
        // on.
        Calc<?> calc = getChildCalc(0, Calc.class);
        if (super.dependsOn(hierarchy)) {
            return true;
        }
        if (includeEmpty) {
            return false;
        }
        // COUNT(<set>, EXCLUDEEMPTY) depends only on the
        // dimensions that <Set> depends on, plus all
        // dimensions not masked by the set.
        return !calc.getType().usesHierarchy(hierarchy, true);
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
