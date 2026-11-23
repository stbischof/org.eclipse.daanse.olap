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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

public class TopBottomPercentSumFunDef extends AbstractFunctionDefinition {

    /**
     * Whether to calculate top (as opposed to bottom).
     */
    final boolean top;
    /**
     * Whether to calculate percent (as opposed to sum).
     */
    final boolean percent;

    public TopBottomPercentSumFunDef(FunctionMetaData functionMetaData, boolean top, boolean percent) {
        super(functionMetaData);
        this.top = top;
        this.percent = percent;
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileMutableList(call.getArg(0));
        final DoubleCalc doubleCalc = compiler.compileDouble(call.getArg(1));
        final Calc<?> calc = compiler.compileExactScalar(call.getArg(2));
        return new TopBottomPercentSumCalc(call.getType(), tupleListCalc, doubleCalc, calc, top, percent);
    }

}
