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
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.core.AbstractAggregateFunDef;

public class SumListCalc extends AbstractNestedDoubleCalc {

    public SumListCalc(Type type, TupleListCalc tupleListCalc, Calc<?> calc) {
        super(type, tupleListCalc, calc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        evaluator.getTiming().markStart(SumFunDef.TIMING_NAME);
        final int savepoint = evaluator.savepoint();
        try {
            TupleList memberList = AbstractAggregateFunDef.evaluateCurrentList(getChildCalc(0, TupleListCalc.class),
                    evaluator);
            evaluator.setNonEmpty(false);
            return FunUtil.sumDouble(evaluator, memberList, getChildCalc(1, Calc.class));
        } finally {
            evaluator.restore(savepoint);
            evaluator.getTiming().markEnd(SumFunDef.TIMING_NAME);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }

}
