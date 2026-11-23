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

import org.eclipse.daanse.olap.api.calc.compiler.CompileRequest;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.util.type.TypeUtil;

public class IifFunDef extends AbstractFunctionDefinition {
    /**
     * Creates an IifFunDef.
     *
     * @param functionMetaData FunctionMetaData
     */
    public IifFunDef(FunctionMetaData functionMetaData)
    {
        super(functionMetaData);
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        // This is messy. We have already decided which variant of Iif to use,
        // and that involves some upcasts. For example, Iif(b, n, NULL) resolves
        // to the type of n. We don't want to throw it away and take the most
        // general type. So, for scalar types we create a type based on
        // returnCategory.
        //
        // But for dimensional types (member, level, hierarchy, dimension,
        // tuple) we want to preserve as much type information as possible, so
        // we recompute the type based on the common types of all args.
        //
        // FIXME: We should pass more info into this method, such as the list
        // of conversions computed while resolving overloadings.
        switch (getFunctionMetaData().returnCategory()) {
        case NUMERIC:
            return NumericType.INSTANCE;
        case STRING:
            return StringType.INSTANCE;
        case LOGICAL:
            return BooleanType.INSTANCE;
        default:
            return TypeUtil.computeCommonType(
                true, args[1].getType(), args[2].getType());
        }
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final BooleanCalc booleanCalc =
            compiler.compileBoolean(call.getArg(0));
        final Calc<?> calc1 =
            compiler.compile(call.getArg(1), CompileRequest.as(call.getType(), ResultStyle.ANY_LIST));
        final Calc<?> calc2 =
            compiler.compile(call.getArg(2), CompileRequest.as(call.getType(), ResultStyle.ANY_LIST));
        if (call.getType() instanceof SetType) {
            if (calc1 instanceof TupleListCalc tlc) {

                return new IifSetTypeListCalc(call.getType(), booleanCalc, calc1, calc2);
            } else if (calc1 instanceof TupleIteratorCalc tlc) {

                return new IifSetTypeIterableCalcCalc(call.getType(), booleanCalc, calc1, calc2);
            } else {
                throw new IllegalArgumentException(
                    "Iif: unexpected type for first argument: " + calc1);
            }
        } else {
            return new IifCalc(call.getType(), booleanCalc, calc1, calc2) {
            };
        }
    }


}
