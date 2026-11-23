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
package org.eclipse.daanse.olap.function.def.hierarchize;

import java.util.List;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

public class HierarchizeFunDef extends AbstractFunctionDefinition {
    static final List<String> prePost = List.of("PRE", "POST");

    public HierarchizeFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileMutableList(call.getArg(0));
        String order = FunUtil.getLiteralArg(call, 1, "PRE", HierarchizeFunDef.prePost);
        final boolean post = order.equals("POST");
        return new HierarchizeCalc(call.getType(), tupleListCalc, post) {
        };
    }
}
