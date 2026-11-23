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
package org.eclipse.daanse.olap.function.def.udf.lastnonempty;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionOrigin;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

public class LastNonEmptyFunDef  extends AbstractFunctionDefinition {

    static FunctionOperationAtom atom = new FunctionOperationAtom("LastNonEmpty");
    static String description = """
        Returns the last member of a set whose value is not empty""";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            DataType.MEMBER , new FunctionParameterR[] { FunctionParameterR.param(DataType.SET).describedAs("Set"),
            FunctionParameterR.param(DataType.MEMBER).describedAs("Member") }).origin(FunctionOrigin.UDF)
            .library("daanse.udf").withTextKey("LastNonEmpty.Function").caption("LastNonEmpty Function");

    public LastNonEmptyFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
        final Calc<?> memberCalc = compiler.compileScalar(call.getArg(1));
        return new LastNonEmptyCalc(call.getType(), tupleListCalc, memberCalc);
    }

}
