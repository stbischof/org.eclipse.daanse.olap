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
package org.eclipse.daanse.olap.function.def.set.filter;

import org.eclipse.daanse.olap.api.calc.compiler.CompileRequest;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionInterface;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.exceptions.ResultStyleException;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

public class FilterFunDef extends AbstractFunctionDefinition {

    public static final String TIMING_NAME = FilterFunDef.class.getSimpleName();

    static OperationAtom functionAtom = new FunctionOperationAtom("Filter");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom,
            "Returns the set resulting from filtering a set based on a search condition.",
            DataType.SET, new FunctionParameterR[] { FunctionParameterR.param(DataType.SET).describedAs("Set"),
            FunctionParameterR.param(DataType.LOGICAL).describedAs("Condition") }).interfaceName(FunctionInterface.FILTER)
            .withTextKey("Filter.Function").caption("Filter Function");

    public FilterFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(final ResolvedFunCall call, ExpressionCompiler compiler) {
        // Ignore the caller's priority. We prefer to return iterable, because
        // it makes NamedSet.CurrentOrdinal work.
        List<ResultStyle> styles = compiler.getAcceptableResultStyles();
        if (call.getArg(0) instanceof ResolvedFunCall resolvedFunCall
                && resolvedFunCall.getFunDef().getFunctionMetaData().operationAtom().name().equals("AS")) {
            styles = ResultStyle.ITERABLE_ONLY;
        }
        if (styles.contains(ResultStyle.ITERABLE) || styles.contains(ResultStyle.ANY)) {
            return compileCallIterable(call, compiler);
        } else if (styles.contains(ResultStyle.LIST) || styles.contains(ResultStyle.MUTABLE_LIST)) {
            return compileCallList(call, compiler);
        } else {
            throw ResultStyleException.generate(ResultStyle.ITERABLE_LIST_MUTABLELIST_ANY, styles);
        }
    }

    /**
     * Returns an TupleIteratorCalc.
     *
     *
     * Here we would like to get either a TupleIteratorCalc or TupleListCalc
     * (mutable) from the inner expression. For the TupleIteratorCalc, its Iterator
     * can be wrapped with another Iterator that filters each element. For the
     * mutable list, remove all members that are filtered.
     *
     * @param call     Call
     * @param compiler Compiler
     * @return Implementation of this function call in the Iterable result style
     */
    protected TupleIteratorCalc compileCallIterable(final ResolvedFunCall call, ExpressionCompiler compiler) {
        // want iterable, mutable list or immutable list in that order
        Calc<?> imlcalc = compiler.compile(call.getArg(0), CompileRequest.as(null, ResultStyle.ITERABLE_LIST_MUTABLELIST));
        BooleanCalc bcalc = compiler.compileBoolean(call.getArg(1));
        Calc<?>[] calcs = new Calc[] { imlcalc, bcalc };

        // check returned calc ResultStyles
        FunUtil.checkIterListResultStyles(imlcalc);

        if (imlcalc.getResultStyle() == ResultStyle.ITERABLE) {
            return new IterIterFilterCalc(call, calcs);
        } else if (imlcalc.getResultStyle() == ResultStyle.LIST) {
            return new ImmutableIterFilterCalc(call, calcs);
        } else {
            return new MutableIterFilterCalc(call, calcs);
        }
    }

    /**
     * Returns a TupleListCalc.
     *
     * @param call     Call
     * @param compiler Compiler
     * @return Implementation of this function call in the List result style
     */
    protected TupleListCalc compileCallList(final ResolvedFunCall call, ExpressionCompiler compiler) {
        Calc<?> ilcalc = compiler.compileList(call.getArg(0));
        BooleanCalc bcalc = compiler.compileBoolean(call.getArg(1));
        Calc<?>[] calcs = new Calc[] { ilcalc, bcalc };

        // Note that all of the TupleListCalc's return will be mutable
        if (ResultStyle.LIST.equals(ilcalc.getResultStyle())) {
            return new ImmutableListFilterCalc(call, calcs);
        } else if (ResultStyle.MUTABLE_LIST.equals(ilcalc.getResultStyle())) {
            return new MutableListFilterCalc(call, calcs);
        }
        throw ResultStyleException.generateBadType(ResultStyle.MUTABLELIST_LIST, ilcalc.getResultStyle());
    }
}
