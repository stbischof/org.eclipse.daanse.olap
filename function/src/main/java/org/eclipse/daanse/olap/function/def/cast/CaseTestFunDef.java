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
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.calc.base.constant.ConstantCalcs;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

class CaseTestFunDef extends AbstractFunctionDefinition {
    static final String NAME = "_CaseTest";
    static final OperationAtom functionAtom = new CaseOperationAtom(NAME);

    static final String DESCRIPTION = "Evaluates various conditions, and returns the corresponding expression for the first which evaluates to true.";

    public CaseTestFunDef(DataType returnType, FunctionParameterR[] types) {
        super(new FunctionMetaDataR(functionAtom, DESCRIPTION, returnType, types).withTextKey("_CaseTest.Operation").caption("CASE WHEN <Logical Expression> THEN <Expression> ... END"));
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        // When the CASE result type is a (non-boolean) scalar, a value branch that is a tuple/member
        // must be coerced to its scalar cell value; compile such branches with compileScalar so the
        // result is the value, not the raw Member[]. (Boolean has its own dedicated calc below.)
        final boolean scalar = call.getType() instanceof org.eclipse.daanse.olap.api.type.ScalarType
                && !(call.getType() instanceof BooleanType);
        final BooleanCalc[] conditionCalcs = new BooleanCalc[args.length / 2];
        final Calc<?>[] exprCalcs = new Calc[args.length / 2];
        final List<Calc<?>> calcList = new ArrayList<>();
        for (int i = 0, j = 0; i < exprCalcs.length; i++) {
            conditionCalcs[i] = compiler.compileBoolean(args[j++]);
            calcList.add(conditionCalcs[i]);
            exprCalcs[i] = scalar ? compiler.compileExactScalar(args[j++]) : compiler.compile(args[j++]);
            calcList.add(exprCalcs[i]);
        }
        final Calc<?> defaultCalc = args.length % 2 == 1
                ? (scalar ? compiler.compileExactScalar(args[args.length - 1])
                        : compiler.compile(args[args.length - 1]))
                : ConstantCalcs.nullCalcOf(call.getType());
        calcList.add(defaultCalc);
        final Calc<?>[] calcs = calcList.stream().toArray(Calc[]::new);

        if (call.getType() instanceof BooleanType) {
            return new CaseTestNestedBooleanCalc(call.getType(), conditionCalcs, exprCalcs, defaultCalc, calcs);
        }
        return new CaseTestGenericCalc(call.getType(), conditionCalcs, exprCalcs, defaultCalc, calcs);
    }
}
