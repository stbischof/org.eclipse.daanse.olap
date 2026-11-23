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
package org.eclipse.daanse.olap.function.def.covariance;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

public class CovarianceFunDef extends AbstractFunctionDefinition {
    static final OperationAtom functionAtom = new FunctionOperationAtom("Covariance");



    private final boolean biased;

    public CovarianceFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
        this.biased = functionMetaData.operationAtom().name().equals("Covariance");
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc =
            compiler.compileList(call.getArg(0));
        final Calc<?> calc1 =
            compiler.compileExactScalar(call.getArg(1));
        final Calc<?> calc2 =
            call.getArgCount() > 2
            ? compiler.compileExactScalar(call.getArg(2))
            : new CurrentValueUnknownCalc(call.getType());
        return new CovarianceCalc(call.getType(), tupleListCalc, calc1, calc2, biased);
    }
}
