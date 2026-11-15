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
package org.eclipse.daanse.olap.function.def.aggregate;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.aggregate.AggregateCalc;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;
import org.eclipse.daanse.olap.function.core.AbstractAggregateFunDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregateFunDef extends AbstractAggregateFunDef {

    /** A call of this function aggregates the set it is given. */
    @Override
    public boolean aggregates() {
        return true;
    }

    private static final Logger LOGGER =
        LoggerFactory.getLogger(AggregateFunDef.class);
    /**
     * Creates an AggregateFunDef.
     *
     * @param functionMetaData Function Meta Data
     */
    public AggregateFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    private Member getMember(Expression exp) {
        if (exp instanceof MemberExpression memberExpr) {
            Member m = memberExpr.getMember();
            if (m.isMeasure() && !m.isCalculated()) {
                return m;
            }
        }
        // Since the expression is not a base measure, we won't
        // attempt to determine the aggregator and will simply sum.
        String expStr = exp.toString();
        AggregateFunDef.LOGGER.warn(
            "Unable to determine aggregator for non-base measures in 2nd parameter of Aggregate(), summing: {}",
            expStr);
        return null;
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
        final Calc<?> calc =
            call.getArgCount() > 1
                ? compiler.compileScalar(call.getArg(1), true)
                : new CurrentValueUnknownCalc(call.getType());
        final Member member =
            call.getArgCount() > 1 ? getMember(call.getArg(1)) : null;
        return new AggregateCalc(calc.getType(), tupleListCalc, calc, member);
    }

}
