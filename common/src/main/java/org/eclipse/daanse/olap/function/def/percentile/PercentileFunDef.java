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
package org.eclipse.daanse.olap.function.def.percentile;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.def.aggregate.AbstractAggregateFunDef;

public class PercentileFunDef extends AbstractAggregateFunDef {

    public PercentileFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
        final Calc<?> calc = compiler.compileScalar(call.getArg(1), true);
        final DoubleCalc percentCalc = compiler.compileDouble(call.getArg(2));
        return new PercentileCalc(call.getType(), tupleListCalc, calc, percentCalc) {
            @Override
            public Double evaluateInternal(Evaluator evaluator) {
                TupleList list = AbstractAggregateFunDef.evaluateCurrentList(tupleListCalc, evaluator);
                Double percentArg = percentCalc.evaluate(evaluator);
                if (percentArg == null) {
                    // A NULL percent argument yields NULL .
                    return null;
                }
                Double percent = percentArg * 0.01;
                final int savepoint = evaluator.savepoint();
                try {
                    evaluator.setNonEmpty(false);
                    final Double percentile = FunUtil.percentile(evaluator, list, calc, percent);
                    return percentile;
                } finally {
                    evaluator.restore(savepoint);
                }
            }

            @Override
            public boolean dependsOn(Hierarchy hierarchy) {
                return HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
            }
        };
    }
}
