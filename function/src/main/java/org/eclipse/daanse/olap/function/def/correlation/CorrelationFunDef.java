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
package org.eclipse.daanse.olap.function.def.correlation;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.core.AbstractAggregateFunDef;

public class CorrelationFunDef extends AbstractAggregateFunDef {

    public CorrelationFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
        final Calc<?> calc1 = compiler.compileScalar(call.getArg(1), true);
        final Calc<?> calc2 = call.getArgCount() > 2 ? compiler.compileScalar(call.getArg(2), true)
                : new CurrentValueUnknownCalc(call.getType());
        return new AbstractNestedDoubleCalc(call.getType(), new Calc[] { tupleListCalc, calc1, calc2 }) {
            @Override
            public Double evaluateInternal(Evaluator evaluator) {
                final int savepoint = evaluator.savepoint();
                try {
                    evaluator.setNonEmpty(false);
                    TupleList list = AbstractAggregateFunDef.evaluateCurrentList(tupleListCalc, evaluator);

                    return FunUtil.correlation(evaluator, list, calc1, calc2);
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
