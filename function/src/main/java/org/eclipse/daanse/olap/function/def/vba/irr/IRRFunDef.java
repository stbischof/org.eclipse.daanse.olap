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
package org.eclipse.daanse.olap.function.def.vba.irr;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.calc.base.constant.ConstantDoubleCalc;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

public class IRRFunDef  extends AbstractFunctionDefinition {


    public IRRFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Calc<?> valueArrayCalc = compiler.compileScalar(call.getArg(0));
        DoubleCalc guessCalc = null;
        if (call.getArgCount() == 1) {
            guessCalc = new ConstantDoubleCalc(NumericType.INSTANCE, 0.10);
        }
        if (call.getArgCount() == 2) {
            guessCalc = compiler.compileDouble(call.getArg(1));
        }

        return new IRRCalc(call.getType(), valueArrayCalc, guessCalc);
    }

}
