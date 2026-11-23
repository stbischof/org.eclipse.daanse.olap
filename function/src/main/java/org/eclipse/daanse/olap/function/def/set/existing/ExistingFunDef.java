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

package org.eclipse.daanse.olap.function.def.set.existing;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.function.FunctionInterface;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

public class ExistingFunDef extends AbstractFunctionDefinition {

    static OperationAtom functionAtom = new PrefixOperationAtom("Existing");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom,
            "Forces the set to be evaluated within the current context.", DataType.SET,
            new FunctionParameterR[] { FunctionParameterR.param(DataType.SET).describedAs("Set") }).interfaceName(FunctionInterface.FILTER)
            .withTextKey("Existing.Prefix").caption("Existing Prefix");

    protected ExistingFunDef() {
        super(functionMetaData);
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        return args[0].getType();
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleIteratorCalc setArg = compiler.compileIterable(call.getArg(0));
        final Type myType = call.getArg(0).getType();

        return new ExistingCalc(call.getType(), setArg, myType);
    }
}
