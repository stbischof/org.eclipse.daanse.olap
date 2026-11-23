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

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.SetConstructorFunction;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.exceptions.ResultStyleException;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.query.base.Expressions;
import org.eclipse.daanse.olap.query.component.ResolvedFunCallImpl;
import org.eclipse.daanse.olap.util.type.TypeUtil;

/**
 * SetFunDef implements the 'set' function (whose syntax is the
 * brace operator, { ... }).
 *
 * @author jhyde
 * @since 3 March, 2002
 */
public class SetFunDef extends AbstractFunctionDefinition implements SetConstructorFunction {

    static OperationAtom functionAtom = new BracesOperationAtom();
    public static final String DESCRIPTION = "Brace operator constructs a set.";
    private final static String argsMustHaveSameHierarchy = "All arguments to function ''{0}'' must have same hierarchy.";

    SetFunDef(FunctionParameterR[] parameterTypes) {
        super(new FunctionMetaDataR(SetFunDef.functionAtom, DESCRIPTION,
                DataType.SET, parameterTypes).withTextKey("Brace.Operator").caption("Brace operator"));
    }

    @Override
    public void unparse(Expression[] args, PrintWriter pw) {
        Expressions.unparseExpressions(pw, args, "{", ", ", "}");
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        // All of the members in {<Member1>[,<MemberI>]...} must have the same
        // Hierarchy. But if there are no members, we can't derive a
        // hierarchy.
        Type type0 = null;
        if (args.length == 0) {
            // No members to go on, so we can't guess the hierarchy.
            type0 = MemberType.Unknown;
        } else {
            for (int i = 0; i < args.length; i++) {
                Expression arg = args[i];
                Type type = arg.getType();
                type = TypeUtil.toMemberOrTupleType(type);
                if (i == 0) {
                    type0 = type;
                } else {
                    if (!TypeUtil.isUnionCompatible(type0, type)) {
                        throw new OlapRuntimeException(MessageFormat.format(argsMustHaveSameHierarchy,
                                getFunctionMetaData().operationAtom().name()));
                    }
                }
            }
        }
        return new SetType(type0);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        if (args.length == 0) {
            // Special treatment for empty set, because we don't know whether it
            // is a set of members or tuples, and so we need it to implement
            // both MemberListCalc and TupleListCalc.
            return new EmptyListCalc(call);
        }
        if (args.length == 1 && args[0].getType() instanceof SetType) {
            // Optimized case when there is only one argument. This occurs quite
            // often, because people write '{Foo.Children} on 1' when they could
            // write 'Foo.Children on 1'.
            return args[0].accept(compiler);
        }
        
        
                  return new SetListCalc(call.getType(), args, compiler, ResultStyle.LIST_MUTABLELIST);
     
    }

    static List<Calc<?>> compileSelf(Expression[] args, ExpressionCompiler compiler,
            List<ResultStyle> resultStyles) {
        List<Calc<?>> calcs = new ArrayList<>(args.length);
        for (Expression arg : args) {
            calcs.add(SetFunDef.createCalc(arg, compiler, resultStyles));
        }
        return calcs;
    }

    private static TupleIteratorCalc createCalc(Expression arg, ExpressionCompiler compiler,
            List<ResultStyle> resultStyles) {
        final Type type = arg.getType();
        if (type instanceof SetType) {
            final Calc<?> calc = compiler.compile(arg, org.eclipse.daanse.olap.api.calc.compiler.CompileRequest.as(null, resultStyles));
            switch (calc.getResultStyle()) {
            case ITERABLE:
                final TupleIteratorCalc tupleIteratorCalc = (TupleIteratorCalc) calc;
                return new SetIterableCalc(type, calc, tupleIteratorCalc) {

                };
            case LIST:
            case MUTABLE_LIST:
                final TupleListCalc tupleListCalc = (TupleListCalc) calc;
                return new SetMutableCalc(type, calc, tupleListCalc);
            }
            throw ResultStyleException.generateBadType(ResultStyle.ITERABLE_LIST_MUTABLELIST, calc.getResultStyle());
        } else if (TypeUtil.couldBeMember(type)) {
            final MemberCalc memberCalc = compiler.compileMember(arg);
            final ResolvedFunCall call = SetFunDef.wrapAsSet(arg);
            return new SetMemberCalc(type, memberCalc);
        } else {
            final TupleCalc tupleCalc = compiler.compileTuple(arg);
            final ResolvedFunCall call = SetFunDef.wrapAsSet(arg);
            return new SetCalc(call.getType(), tupleCalc);
        }
    }

    /**
     * Creates a call to the set operator with a given collection of expressions.
     *
     *
     * There must be at least one expression. Each expression may be a set of
     * members/tuples, or may be a member/tuple, but method assumes that expressions
     * have compatible types.
     *
     * @param args Expressions
     * @return Call to set operator
     */
    public static ResolvedFunCall wrapAsSet(Expression... args) {
        assert args.length > 0;
        final FunctionParameterR[] categories = new FunctionParameterR[args.length];
        Type type = null;
        for (int i = 0; i < args.length; i++) {
            final Expression arg = args[i];
            categories[i] = new FunctionParameterR(arg.getCategory());
            final Type argType = arg.getType();
            if (argType instanceof SetType setType) {
                type = setType.getElementType();
            } else {
                type = argType;
            }
        }

        return new ResolvedFunCallImpl(new SetFunDef(categories), args, new SetType(type));
    }
}
