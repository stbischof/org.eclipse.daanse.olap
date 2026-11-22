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
package org.eclipse.daanse.olap.function.def.set;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.VoidCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedVoidCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.fun.FunUtil;

/**
 * Compiled expression to implement the MDX set function, { ...
 * }, applied to a set of tuples, as a list.
 *
 * The set function can contain expressions which yield sets together
 * with expressions which yield individual tuples, provided that
 * they all have the same type. It automatically removes null
 * or partially-null tuples from the list.
 *
 * Also, does not process high-cardinality dimensions specially.
 */
public class SetListCalc  extends AbstractNestedTupleListCalc {
    private TupleList result;
    private final VoidCalc[] voidCalcs;

    public SetListCalc(
        Type type,
        Expression[] args,
        ExpressionCompiler compiler,
        List<ResultStyle> resultStyles)
    {
        super(type, null);
        voidCalcs = compileSelf(args, compiler, resultStyles);
        result = TupleCollections.createList(getType().getArity());
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return voidCalcs;
    }

    private VoidCalc[] compileSelf(
        Expression[] args,
        ExpressionCompiler compiler,
        List<ResultStyle> resultStyles)
    {
        VoidCalc[] voidCalcs = new VoidCalc[args.length];
        for (int i = 0; i < args.length; i++) {
            voidCalcs[i] = createCalc(args[i], compiler, resultStyles);
        }
        return voidCalcs;
    }

    private VoidCalc createCalc(
        Expression arg,
        ExpressionCompiler compiler,
        List<ResultStyle> resultStyles)
    {
        final Type type = arg.getType();
        if (type instanceof SetType) {
            // TODO use resultStyles
            final TupleListCalc tupleListCalc = compiler.compileList(arg);
            return new AbstractNestedVoidCalc<>(type, new Calc[] {tupleListCalc}) {
                // name "Sublist..."
                @Override
                public Void evaluateInternal(Evaluator evaluator) {
                    TupleList list =
                        tupleListCalc.evaluate(evaluator);
                    // Add only tuples which are not null. Tuples with
                    // any null members are considered null.
                    outer:
                    for (List<Member> members : list) {
                        for (Member member : members) {
                            if (member == null || member.isNull()) {
                                continue outer;
                            }
                        }
                        result.add(members);
                    }
                    return null;
                }

            };
        } else if (type instanceof org.eclipse.daanse.olap.api.type.LevelType) {
            org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl unresolvedFunCall = new org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl(
                    new PlainPropertyOperationAtom("Members"),
                    new Expression[] {arg});
            final TupleListCalc tupleListCalc = compiler.compileList(unresolvedFunCall.accept(compiler.getValidator()));
            return new AbstractNestedVoidCalc<>(type, new Calc[] {tupleListCalc}) {
                @Override
                public Void evaluateInternal(Evaluator evaluator) {
                    TupleList list =
                            tupleListCalc.evaluate(evaluator);
                    result = list;
                    return null;
                }
            };
        } else if (type.getArity() == 1 && arg instanceof MemberType) {
            final MemberCalc memberCalc = compiler.compileMember(arg);
            return new AbstractNestedVoidCalc<>(type, new Calc[]{memberCalc}) {
                final Member[] members = {null};
                @Override
                public Void evaluateInternal(Evaluator evaluator) {
                    // Don't add null or partially null tuple to result.
                    Member member = memberCalc.evaluate(evaluator);
                    if (member == null || member.isNull()) {
                        return null;
                    }
                    members[0] = member;
                    result.addTuple(members);
                    return null;
                }
            };
        } else {
            final TupleCalc tupleCalc = compiler.compileTuple(arg);
            return new AbstractNestedVoidCalc<>(type, new Calc[]{tupleCalc}) {
                @Override
                public Void evaluateInternal(Evaluator evaluator) {
                    // Don't add null or partially null tuple to result.
                    Member[] members = tupleCalc.evaluate(evaluator);
                    if (members == null
                        || FunUtil.tupleContainsNullMember(members))
                    {
                        return null;
                    }
                    result.addTuple(members);
                    return null;
                }
            };
        }
    }

    @Override
    public TupleList evaluateInternal(final Evaluator evaluator) {
        result.clear();
        for (VoidCalc voidCalc : voidCalcs) {
            voidCalc.evaluate(evaluator);
        }
        return result.copyList(-1);
    }
}
