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

package org.eclipse.daanse.olap.function.def.cast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.CaseOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.constant.ConstantCalcs;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

class CaseMatchFunDef extends AbstractFunctionDefinition {

    static final String NAME = "_CaseMatch";
    static final OperationAtom functionAtom = new CaseOperationAtom(NAME);

    static final String DESCRIPTION = "Evaluates various expressions, and returns the corresponding expression for the first which matches a particular value.";

    public CaseMatchFunDef(DataType returnType, FunctionParameterR[] types) {
        super(new FunctionMetaDataR(functionAtom, DESCRIPTION,
                returnType, types).withTextKey("_CaseMatch.Operation").caption("CASE <Value> WHEN <Value> THEN <Expression> ... END"));
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        final List<Calc<?>> calcList = new ArrayList<>();
        final Calc<?> valueCalc = compiler.compileExactScalar(args[0]);
        calcList.add(valueCalc);
        final int matchCount = (args.length - 1) / 2;
        final Calc<?>[] matchCalcs = new Calc[matchCount];
        final Calc<?>[] exprCalcs = new Calc[matchCount];
        for (int i = 0, j = 1; i < exprCalcs.length; i++) {
            matchCalcs[i] = compiler.compileExactScalar(args[j++]);
            calcList.add(matchCalcs[i]);
            exprCalcs[i] = compiler.compile(args[j++]);
            calcList.add(exprCalcs[i]);
        }
        final Calc<?> defaultCalc = args.length % 2 == 0 ? compiler.compile(args[args.length - 1])
                : ConstantCalcs.nullCalcOf(call.getType());
        calcList.add(defaultCalc);
        final Calc<?>[] calcs = calcList.toArray(Calc[]::new);

        return new CaseMatchCalc(call.getType(), valueCalc, exprCalcs, matchCalcs, defaultCalc, calcs);
    }
}
