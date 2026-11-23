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
package org.eclipse.daanse.olap.function.def.iif;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;

public class IifNumericFunDef extends IifFunDef {

    static FunctionParameterR[] params = { FunctionParameterR.param(DataType.LOGICAL, "Condition").describedAs("Condition"),
            FunctionParameterR.param(DataType.NUMERIC, "TruePart").describedAs("Numeric Expression returned when Condition is true"),
            FunctionParameterR.param(DataType.NUMERIC, "FalsePart").describedAs("Numeric Expression returned when Condition is false") };
    static OperationAtom NUMERIC_INSTANCE_FUNCTION_ATOM = new FunctionOperationAtom("IIf");
    static FunctionMetaData NUMERIC_INSTANCE_FUNCTION_META_DATA = new FunctionMetaDataR(NUMERIC_INSTANCE_FUNCTION_ATOM,
            "Returns one of two numeric values determined by a logical test.",
            DataType.NUMERIC, params).withTextKey("IIf.Numeric.Function").caption("IIf for Numeric Function");
    // IIf(<Logical Expression>, <Numeric Expression>, <Numeric Expression>)

    public IifNumericFunDef() {
        super(NUMERIC_INSTANCE_FUNCTION_META_DATA);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final BooleanCalc booleanCalc = compiler.compileBoolean(call.getArg(0));
        final Calc<?> calc1 = compiler.compileExactScalar(call.getArg(1));
        final Calc<?> calc2 = compiler.compileExactScalar(call.getArg(2));
        return new IifNumericCalc(call.getType(), booleanCalc, calc1, calc2);
    }

}
