/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.math.BigDecimal;

import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionParameter;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.query.component.MemberExpressionImpl;
import org.eclipse.daanse.olap.query.component.NumericLiteralImpl;
import org.eclipse.daanse.olap.query.component.ResolvedFunCallImpl;
import org.junit.jupiter.api.Test;

/**
 * Which formulas do no more than rename a member. Read by the name a function is registered under, so
 * the cell needs to know nothing about the function library.
 */
class AFormulaThatOnlyRenamesAMemberTest {

    private static final Member TARGET = new TestMember("[Geo].[Berlin]");

    @Test
    void aFormulaThatIsTheMemberItselfNamesIt() {
        assertThat(behind(new MemberExpressionImpl(TARGET))).contains(TARGET);
    }

    @Test
    void anAggregateOverOneMemberNamesThatMember() {
        assertThat(behind(call("Aggregate", braces(new MemberExpressionImpl(TARGET))))).contains(TARGET);
    }

    @Test
    void anAggregateOverTwoMembersNamesNone() {
        Expression two = braces(new MemberExpressionImpl(TARGET), new MemberExpressionImpl(TARGET));

        assertThat(behind(call("Aggregate", two))).isEmpty();
    }

    @Test
    void anotherFunctionOverOneMemberNamesNone() {
        assertThat(behind(call("Sum", braces(new MemberExpressionImpl(TARGET))))).isEmpty();
    }

    @Test
    void aFormulaThatIsANumberNamesNone() {
        assertThat(behind(NumericLiteralImpl.create(BigDecimal.ONE))).isEmpty();
    }

    private static java.util.Optional<Member> behind(Expression formula) {
        Member calculated = new TestMember("[Geo].[Named]") {
            @Override
            public boolean isCalculated() {
                return true;
            }

            @Override
            public Expression getExpression() {
                return formula;
            }
        };
        return ResultCell.memberBehindTrivialFormula(calculated);
    }

    private static Expression braces(Expression... members) {
        return new ResolvedFunCallImpl(new NamedFunction(new BracesOperationAtom()), members,
                NumericType.INSTANCE);
    }

    private static ResolvedFunCall call(String name, Expression... args) {
        return new ResolvedFunCallImpl(new NamedFunction(new FunctionOperationAtom(name)), args,
                NumericType.INSTANCE);
    }

    /** A function that is nothing but its name; the reading under test looks at no more than that. */
    private record NamedFunction(OperationAtom atom) implements FunctionDefinition, FunctionMetaData {

        @Override
        public FunctionMetaData getFunctionMetaData() {
            return this;
        }

        @Override
        public OperationAtom operationAtom() {
            return atom;
        }

        @Override
        public String description() {
            return atom.name();
        }

        @Override
        public DataType returnCategory() {
            return DataType.NUMERIC;
        }

        @Override
        public DataType[] parameterDataTypes() {
            return new DataType[0];
        }

        @Override
        public FunctionParameter[] parameters() {
            return new FunctionParameter[0];
        }

        @Override
        public Expression createCall(Validator validator, Expression[] args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSignature() {
            return atom.name();
        }

        @Override
        public void unparse(Expression[] args, PrintWriter pw) {
            pw.print(atom.name());
        }

        @Override
        public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
            throw new UnsupportedOperationException();
        }
    }
}
