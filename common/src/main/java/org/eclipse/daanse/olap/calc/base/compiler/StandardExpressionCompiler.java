/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * 2002-2017 Hitachi Vantara.
 * 2006      jhyde
 *
 * Contributors after Fork in 2023:
 *   Sergei Semenkov (2001)
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.calc.base.compiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.daanse.mdx.model.api.expression.operation.CastOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DimensionCalc;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.CompileHook;
import org.eclipse.daanse.olap.api.calc.compiler.CompileRequest;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.compiler.ParameterSlot;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.exception.TypeConversionException;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.DimensionType;
import org.eclipse.daanse.olap.api.type.HierarchyType;
import org.eclipse.daanse.olap.api.type.LevelType;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.NullType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.ScalarType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.query.component.SymbolLiteralImpl;
import org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl;
import org.eclipse.daanse.olap.util.type.TypeUtil;

/**
 * Turns a resolved expression into a {@link Calc}: the one compiler (before, a base class and an
 * "enhanced" subclass the factory built, with nothing built of the base alone).
 * <p>
 * {@link #compile(Expression, CompileRequest)} is the one way in, and every typed entry of {@link ExpressionCompiler}
 * reaches it: a request of kind ANY visits the expression -- the expression calls back the compiler for its node kind,
 * a function call reaches its {@code FunctionDefinition.compileCall} -- and every other kind compiles, then converts
 * through {@link CalcAdapters}. Every entry passes the {@link CompileHook}s once (profiling, the dependency check);
 * the steps between kinds here (a double is a scalar, then converted) call the private implementations, not the entry,
 * so no step is hulled twice. A function's arguments are entries: {@code compileCall} asks the compiler for them.
 * A request made by {@code CompileRequest.as} holds its preferred {@link ResultStyle}s for the duration of the call.
 */
public class StandardExpressionCompiler implements ExpressionCompiler {
    private final Evaluator evaluator;
    private final Validator validator;
    private final Map<Parameter, ParameterSlotImpl> parameterSlots = new HashMap<>();
    private List<ResultStyle> resultStyles;
    private final List<CompileHook> hooks;
    private static final String NULL_NOT_SUPPORTED = "Function does not support NULL member parameter";

    public StandardExpressionCompiler(Evaluator evaluator, Validator validator) {
        this(evaluator, validator, ResultStyle.ANY_LIST);
    }

    /** A compiler constrained to produce one of a list of result styles, preferred first; null is any list. */
    public StandardExpressionCompiler(Evaluator evaluator, Validator validator, List<ResultStyle> resultStyles) {
        this(evaluator, validator, resultStyles, List.of());
    }

    /** A compiler whose every compiled calc passes the hooks in order, the first one's hull innermost. */
    public StandardExpressionCompiler(Evaluator evaluator, Validator validator, List<ResultStyle> resultStyles,
            List<CompileHook> hooks) {
        this.evaluator = evaluator;
        this.validator = validator;
        this.resultStyles = Objects.requireNonNullElse(resultStyles, ResultStyle.ANY_LIST);
        this.hooks = List.copyOf(hooks);
    }

    @Override
    public Evaluator getEvaluator() {
        return evaluator;
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    /**
     * The one way in: the calc the request asks for, then through the hooks in order -- once per entry, whichever
     * entry; the steps between kinds below do not come here, so no step is hulled twice.
     */
    @Override
    public Calc<?> compile(Expression expression, CompileRequest request) {
        Calc<?> calc = translate(expression, request);
        for (CompileHook hook : hooks) {
            calc = hook.afterCompile(expression, calc, request);
        }
        return calc;
    }

    /**
     * A request of kind ANY visits the expression (under the styles of {@link CompileRequest#as}), every other kind
     * goes to its implementation below.
     */
    private Calc<?> translate(Expression expression, CompileRequest request) {
        return switch (request.kind()) {
        case ANY -> request.styles() == null ? expression.accept(this)
                : as(expression, request.resultType(), request.styles());
        case MEMBER -> member(expression);
        case LEVEL -> level(expression);
        case DIMENSION -> dimension(expression);
        case HIERARCHY -> hierarchy(expression);
        case INTEGER -> CalcAdapters.asInteger(scalar(expression, false), expression.getType());
        case STRING -> CalcAdapters.asString(scalar(expression, false));
        case DATE_TIME -> CalcAdapters.asDateTime(scalar(expression, false));
        case BOOLEAN -> CalcAdapters.asBoolean(scalar(expression, false), expression.getType());
        case DOUBLE -> CalcAdapters.asDouble(scalar(expression, false), expression.getType());
        case TUPLE -> tuple(expression);
        case LIST -> list(expression, false);
        case MUTABLE_LIST -> list(expression, true);
        case ITERABLE -> iterable(expression);
        case SCALAR -> scalar(expression, false);
        case EXACT_SCALAR -> scalar(expression, true);
        };
    }

    private Calc<?> as(Expression expression, Type resultType, List<ResultStyle> preferredResultStyles) {
        final List<ResultStyle> savedResultStyles = resultStyles;
        try {
            resultStyles = preferredResultStyles;
            if (resultType != null && resultType != expression.getType()) {
                Calc<?> typedCalc = switch (resultType) {
                case MemberType _ -> member(expression);
                case LevelType _ -> level(expression);
                case HierarchyType _ -> hierarchy(expression);
                case DimensionType _ -> dimension(expression);
                case ScalarType _ -> scalar(expression, false);
                default -> null;
                };
                if (typedCalc != null) {
                    return typedCalc;
                }
            }
            return expression.accept(this);
        } finally {
            resultStyles = savedResultStyles;
        }
    }

    private MemberCalc member(Expression expression) {
        final Type type = expression.getType();
        return switch (type) {
        case HierarchyType _, DimensionType _ -> CalcAdapters.currentMember(hierarchy(expression));
        case NullType _ -> throw new OlapRuntimeException(NULL_NOT_SUPPORTED);
        case MemberType _ -> CalcAdapters.asMember(type, expression.accept(this));
        default -> throw new TypeConversionException("no member can be worked out from " + type);
        };
    }

    private LevelCalc level(Expression expression) {
        final Type type = expression.getType();
        return switch (type) {
        // <Member> --> <Member>.Level; <Dimension> --> <Dimension>.CurrentMember.Level: the conversion
        // Dimension -> Level goes through the member of the default hierarchy
        case MemberType _, DimensionType _ -> CalcAdapters.levelOf(type, member(expression));
        case LevelType _ -> CalcAdapters.asLevel(type, expression.accept(this));
        case NullType _ -> throw new OlapRuntimeException(NULL_NOT_SUPPORTED);
        default -> throw new TypeConversionException("no level can be worked out from " + type);
        };
    }

    private DimensionCalc dimension(Expression expression) {
        final Type type = expression.getType();
        return switch (type) {
        case HierarchyType _ -> CalcAdapters.dimensionOf(type, hierarchy(expression));
        case DimensionType _ -> CalcAdapters.asDimension(type, expression.accept(this));
        default -> throw new TypeConversionException("no dimension can be worked out from " + type);
        };
    }

    private HierarchyCalc hierarchy(Expression expression) {
        final Type type = expression.getType();
        return switch (type) {
        case DimensionType _ -> {
            // <Dimension> --> unique Hierarchy else error; resolved at compile time if constant
            final HierarchyCalc constant = CalcAdapters.constantDefaultHierarchy(type);
            yield constant != null ? constant : CalcAdapters.defaultHierarchy(type, dimension(expression));
        }
        case MemberType _ -> CalcAdapters.hierarchyOf(type, member(expression));
        case LevelType _ -> CalcAdapters.hierarchyOf(type, level(expression));
        case HierarchyType _ -> (HierarchyCalc) expression.accept(this);
        default -> throw new IllegalArgumentException("Unexpected type: " + type);
        };
    }

    private TupleCalc tuple(Expression expression) {
        final Calc<?> calc = expression.accept(this);
        final Type type = expression.getType();
        return switch (type) {
        case DimensionType _, HierarchyType _ -> {
            // a dimension or hierarchy in a tuple position is its default member
            final UnresolvedFunCallImpl defaultMember = new UnresolvedFunCallImpl(
                    new PlainPropertyOperationAtom("DefaultMember"), new Expression[] { expression });
            yield tuple(defaultMember.accept(getValidator()));
        }
        case TupleType _, MemberType _ -> CalcAdapters.asTuple(type, calc);
        default -> throw Util.newInternal("cannot cast " + expression);
        };
    }

    private TupleListCalc list(Expression expression, boolean mutable) {
        if (!(expression.getType() instanceof SetType)) {
            throw new IllegalArgumentException("must be a set: " + expression);
        }
        final List<ResultStyle> resultStyleList = mutable ? ResultStyle.MUTABLELIST_ONLY : ResultStyle.LIST_ONLY;

        Calc<?> calc = as(expression, null, resultStyleList);
        if (calc instanceof TupleListCalc tupleListCalc) {
            return CalcAdapters.mutableIfAsked(tupleListCalc, mutable);
        }
        if (calc == null) {
            calc = as(expression, null, ResultStyle.ITERABLE_ANY);
            assert calc != null;
        }
        if (calc instanceof TupleListCalc tupleListCalc) {
            return CalcAdapters.mutableIfAsked(tupleListCalc, mutable);
        }
        // If expression is an iterator, convert it to a list. Don't check
        // 'calc instanceof TupleIteratorCalc' because some generic calcs implement both
        // TupleListCalc and TupleIteratorCalc.
        if (!(calc instanceof TupleListCalc)) {
            return CalcAdapters.listOf((TupleIteratorCalc<?>) calc);
        }
        // A set can only be implemented as a list or an iterable.
        throw Util.newInternal("Cannot convert calc to list: " + calc);
    }

    private TupleIteratorCalc<?> iterable(Expression expression) {
        TupleIteratorCalc<?> calc = (TupleIteratorCalc<?>) as(expression, null, ResultStyle.ITERABLE_ONLY);
        if (calc == null) {
            calc = (TupleIteratorCalc<?>) as(expression, null, ResultStyle.ANY_ONLY);
            assert calc != null;
        }
        return calc;
    }

    private Calc<?> scalar(Expression expression, boolean specific) {
        final Type type = expression.getType();
        return switch (type) {
        case MemberType _ -> CalcAdapters.valueOf(member(expression), nullForUnrelatedDimension());
        case DimensionType _, HierarchyType _ ->
            CalcAdapters.valueOf(CalcAdapters.currentMember(hierarchy(expression)), nullForUnrelatedDimension());
        case TupleType tupleType -> {
            final TupleCalc tupleCalc = tuple(expression);
            yield CalcAdapters.valueOf(tupleType, tupleCalc, nullForUnrelatedDimension());
        }
        case ScalarType _ when specific -> switch (type) {
            case BooleanType _ -> CalcAdapters.asBoolean(scalar(expression, false), type);
            case NumericType _ -> CalcAdapters.asDouble(scalar(expression, false), type);
            case StringType _ -> CalcAdapters.asString(scalar(expression, false));
            default -> expression.accept(this);
            };
        default -> expression.accept(this);
        };
    }

    /** Asked only where a member or tuple value is read: a compiler without an evaluator compiles plain scalars. */
    private boolean nullForUnrelatedDimension() {
        return getEvaluator().mightReturnNullForUnrelatedDimension();
    }

    @Override
    public ParameterSlot registerParameter(Parameter parameter) {
        final ParameterSlot slot = parameterSlots.get(parameter);
        if (slot != null) {
            return slot;
        }
        final int index = parameterSlots.size();
        final ParameterSlotImpl slot2 = new ParameterSlotImpl(parameter, index);
        parameterSlots.put(parameter, slot2);
        slot2.value = parameter.getValue();

        // Compile the expression only AFTER the parameter has been
        // registered with a slot. Otherwise a cycle is possible.
        final Type type = parameter.getType();
        Expression defaultExp = parameter.getDefaultExp();
        Calc<?> calc;
        if (type instanceof ScalarType) {
            if (!defaultExp.getType().equals(type)) {
                defaultExp = new UnresolvedFunCallImpl(new CastOperationAtom(), new Expression[] { defaultExp,
                        SymbolLiteralImpl.create(TypeUtil.typeToCategory(type).getName()) });
                defaultExp = getValidator().validate(defaultExp, true);
            }
            // the steps, not the entries: the default value of a parameter is no compiled expression of the query
            // and was never hulled (the decorators did not see it either)
            calc = scalar(defaultExp, true);
        } else {
            calc = as(defaultExp, type, resultStyles);
        }
        slot2.setDefaultValueCalc(calc);
        return slot2;
    }

    @Override
    public List<ResultStyle> getAcceptableResultStyles() {
        return resultStyles;
    }
}
