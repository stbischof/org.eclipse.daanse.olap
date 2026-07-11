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
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ConstantCalc;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.DimensionCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.compiler.ParameterSlot;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.exception.TypeConversionException;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.DecimalType;
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
import org.eclipse.daanse.olap.calc.base.constant.ConstantBooleanCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantDoubleCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantHierarchyCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantStringCalc;
import org.eclipse.daanse.olap.calc.base.type.booleanx.DoubleToBooleanCalc;
import org.eclipse.daanse.olap.calc.base.type.booleanx.IntegerToBooleanCalc;
import org.eclipse.daanse.olap.calc.base.type.booleanx.UnknownToBooleanCalc;
import org.eclipse.daanse.olap.calc.base.type.datetime.UnknownToDateTimeCalc;
import org.eclipse.daanse.olap.calc.base.type.dimension.DimensionOfHierarchyCalc;
import org.eclipse.daanse.olap.calc.base.type.dimension.UnknownToDimensionCalc;
import org.eclipse.daanse.olap.calc.base.type.doublex.IntegerToDoubleCalc;
import org.eclipse.daanse.olap.calc.base.type.doublex.UnknownToDoubleCalc;
import org.eclipse.daanse.olap.calc.base.type.hierarchy.DimensionDefaultHierarchyCalc;
import org.eclipse.daanse.olap.calc.base.type.integer.DoubleToIntegerCalc;
import org.eclipse.daanse.olap.calc.base.type.integer.UnknownToIntegerCalc;
import org.eclipse.daanse.olap.calc.base.type.level.UnknownToLevelCalc;
import org.eclipse.daanse.olap.calc.base.type.member.UnknownToMemberCalc;
import org.eclipse.daanse.olap.calc.base.type.string.UnknownToStringCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.IterableListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.MemberValueCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleValueCalc;
import org.eclipse.daanse.olap.calc.base.util.DimensionUtil;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.def.hierarchy.level.LevelHierarchyCalc;
import org.eclipse.daanse.olap.function.def.hierarchy.member.HierarchyCurrentMemberCalc;
import org.eclipse.daanse.olap.function.def.hierarchy.member.HierarchyCurrentMemberFixedCalc;
import org.eclipse.daanse.olap.function.def.hierarchy.member.MemberHierarchyCalc;
import org.eclipse.daanse.olap.function.def.level.member.MemberLevelCalc;
import org.eclipse.daanse.olap.query.component.SymbolLiteralImpl;
import org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl;
import org.eclipse.daanse.olap.util.type.TypeUtil;

public class BaseExpressionCompiler implements ExpressionCompiler {
    private final Evaluator evaluator;
    private final Validator validator;
    private final Map<Parameter, ParameterSlotImpl> parameterSlots = new HashMap<>();
    private List<ResultStyle> resultStyles;
    private static final String NULL_NOT_SUPPORTED = "Function does not support NULL member parameter";
    private static final DecimalType INTEGER_TYPE = new DecimalType(Integer.MAX_VALUE, 0);

    /**
     * Creates an BaseExpressionCompiler
     *
     * @param evaluator Evaluator
     * @param validator Validator
 */
    public BaseExpressionCompiler(Evaluator evaluator, Validator validator) {
        this(evaluator, validator, ResultStyle.ANY_LIST);
    }

    /**
     * Creates an AbstractExpCompiler which is constrained to produce one of a set
     * of result styles.
     *
     * @param evaluator    Evaluator
     * @param validator    Validator
     * @param resultStyles List of result styles, preferred first, must not be
 */
    public BaseExpressionCompiler(Evaluator evaluator, Validator validator, List<ResultStyle> resultStyles) {
        this.evaluator = evaluator;
        this.validator = validator;
        this.resultStyles = Objects.requireNonNullElse(resultStyles, ResultStyle.ANY_LIST);
    }

    @Override
    public Evaluator getEvaluator() {
        return evaluator;
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    @Override
    public Calc<?> compile(Expression expression) {
        return expression.accept(this);
    }

    @Override
    public Calc<?> compileAs(Expression expression, Type resultType, List<ResultStyle> preferredResultStyles) {
        Objects.requireNonNull(preferredResultStyles, "preferredResultTypes must not be null");

        final List<ResultStyle> savedResultStyles = resultStyles;
        try {
            resultStyles = preferredResultStyles;
            if (resultType != null && resultType != expression.getType()) {
                Calc<?> typedCalc = switch (resultType) {
                case MemberType _ -> compileMember(expression);
                case LevelType _ -> compileLevel(expression);
                case HierarchyType _ -> compileHierarchy(expression);
                case DimensionType _ -> compileDimension(expression);
                case ScalarType _ -> compileScalar(expression, false);
                default -> null;
                };
                if (typedCalc != null) {
                    return typedCalc;
                }
            }
            return compile(expression);
        } finally {
            resultStyles = savedResultStyles;
        }
    }

    @Override
    public MemberCalc compileMember(Expression expression) {
        final Type type = expression.getType();
        return switch (type) {
            case HierarchyType _, DimensionType _ -> hierarchyToMember(compileHierarchy(expression));
            case NullType _ -> throw new OlapRuntimeException(NULL_NOT_SUPPORTED);
            case MemberType _ -> {
                Calc<?> calc = compile(expression);
                yield (calc instanceof MemberCalc memberCalc) ? memberCalc
                      : new UnknownToMemberCalc(type, calc);
            }
            default -> throw new IllegalArgumentException("Unexpected type for member compilation: " + type);
        };
    }

    private MemberCalc hierarchyToMember(HierarchyCalc hierarchyCalc) {
        final Hierarchy hierarchy = hierarchyCalc.getType().getHierarchy();
        if (hierarchy != null) {
            return new HierarchyCurrentMemberFixedCalc(TypeUtil.toMemberType(hierarchyCalc.getType()), hierarchy);
        }
        return new HierarchyCurrentMemberCalc(TypeUtil.toMemberType(hierarchyCalc.getType()), hierarchyCalc);
    }

    @Override
    public LevelCalc compileLevel(Expression expression) {
        final Type type = expression.getType();
        return switch (type) {
            case MemberType _ -> {
                // <Member> --> <Member>.Level
                final MemberCalc memberCalc = compileMember(expression);
                yield new MemberLevelCalc(LevelType.forType(type), memberCalc);
            }
            case LevelType _ -> {
                Calc<?> calc = compile(expression);
                yield (calc instanceof LevelCalc levelCalc) ? levelCalc
                      : new UnknownToLevelCalc(type, calc);
            }
            default -> throw new IllegalArgumentException("Unexpected type for level compilation: " + type);
        };
    }

    @Override
    public DimensionCalc compileDimension(Expression expression) {
        final Type type = expression.getType();
        return switch (type) {
            case HierarchyType _ -> {
                final HierarchyCalc hierarchyCalc = compileHierarchy(expression);
                yield new DimensionOfHierarchyCalc(new DimensionType(type.getDimension()), hierarchyCalc);
            }
            case DimensionType _ -> {
                Calc<?> calc = compile(expression);
                yield (calc instanceof DimensionCalc dimensionCalc) ? dimensionCalc
                      : new UnknownToDimensionCalc(type, calc);
            }
            default -> throw new IllegalArgumentException("Unexpected type for dimension compilation: " + type);
        };
    }

    @Override
    public HierarchyCalc compileHierarchy(Expression expression) {
        final Type type = expression.getType();
        return switch (type) {
        case DimensionType _ -> {
            // <Dimension> --> unique Hierarchy else error
            // Resolve at compile time if constant
            final Dimension dimension = type.getDimension();
            if (dimension != null) {
                final Hierarchy hierarchy = DimensionUtil.getDimensionDefaultHierarchyOrThrow(dimension);
                if (hierarchy != null) {
                    yield ConstantHierarchyCalc.of(hierarchy);
                }
            }
            yield new DimensionDefaultHierarchyCalc(HierarchyType.forType(type), compileDimension(expression));
        }
        // <Member> --> <Member>.Hierarchy
        case MemberType _ -> new MemberHierarchyCalc(HierarchyType.forType(type), compileMember(expression));
        // <Level> --> <Level>.Hierarchy
        case LevelType _ -> new LevelHierarchyCalc(HierarchyType.forType(type), compileLevel(expression));
        case HierarchyType _ -> (HierarchyCalc) compile(expression);
        default -> throw new IllegalArgumentException("Unexpected type: " + type);
        };
    }

    @Override
    public IntegerCalc compileInteger(Expression expression) {
        final Calc<?> calc = compileScalar(expression, false);
        final Type type = calc.getType();

        if (type instanceof DecimalType decimalType && decimalType.getScale() == 0) {
            return (IntegerCalc) calc;
        }

        return switch (type) {
        case NullType _ when calc instanceof ConstantCalc<?> _ ->
            new ConstantIntegerCalc(INTEGER_TYPE, null);
        case NumericType _ -> compileIntegerFromNumeric(calc, expression);
        default -> new UnknownToIntegerCalc(INTEGER_TYPE, calc);
        };
    }

    private IntegerCalc compileIntegerFromNumeric(Calc<?> calc, Expression expression) {
        return switch (calc) {
        case ConstantCalc<?> constantCalc -> {
            Object o = constantCalc.evaluate(evaluator);
            Integer i = o == null ? null : ((Number) o).intValue();
            yield new ConstantIntegerCalc(INTEGER_TYPE, i);
        }
        case DoubleCalc doubleCalc -> new DoubleToIntegerCalc(expression.getType(), doubleCalc);
        case IntegerCalc integerCalc -> integerCalc;
        default -> new UnknownToIntegerCalc(INTEGER_TYPE, calc);
        };
    }

    @Override
    public StringCalc compileString(Expression expression) {
        Calc<?> calc = compileScalar(expression, false);
        return switch (calc) {
            case StringCalc stringCalc -> stringCalc;
            case ConstantCalc<?> constantCalc -> {
                Object value = constantCalc.evaluate(null);
                yield new ConstantStringCalc(StringType.INSTANCE, value == null ? null : value.toString());
            }
            default -> new UnknownToStringCalc(StringType.INSTANCE, calc);
        };
    }

    @Override
    public DateTimeCalc compileDateTime(Expression expression) {
        Calc<?> calc = compileScalar(expression, false);
        if (calc instanceof DateTimeCalc dateTimeCalc) {
            return dateTimeCalc;
        }
        return new UnknownToDateTimeCalc(calc.getType(), calc);
    }

    @Override
    public TupleListCalc compileList(Expression expression) {
        return compileList(expression, false);
    }

    @Override
    public TupleListCalc compileList(Expression expression, boolean mutable) {
        if (!(expression.getType() instanceof SetType)) {
            throw new IllegalArgumentException("must be a set: " + expression);
        }
        final List<ResultStyle> resultStyleList = mutable ? ResultStyle.MUTABLELIST_ONLY : ResultStyle.LIST_ONLY;

        Calc<?> calc = compileAs(expression, null, resultStyleList);
        if (calc instanceof TupleListCalc tupleListCalc) {
            return tupleListCalc;
        }
        if (calc == null) {
            calc = compileAs(expression, null, ResultStyle.ITERABLE_ANY);
            assert calc != null;
        }
        if (calc instanceof TupleListCalc tupleListCalc) {
            return tupleListCalc;
        }
        // If expression is an iterator, convert it to a list. Don't check
        // 'calc instanceof TupleIteratorCalc' because some generic calcs implement both
        // TupleListCalc and TupleIteratorCalc.
        if (!(calc instanceof TupleListCalc)) {
            return toTupleListCalc((TupleIteratorCalc<?>) calc);
        }
        // A set can only be implemented as a list or an iterable.
        throw Util.newInternal("Cannot convert calc to list: " + calc);
    }

    protected TupleListCalc toTupleListCalc(TupleIteratorCalc<?> calc) {
        return new IterableListCalc(calc);
    }

    @Override
    public TupleIteratorCalc<?> compileIter(Expression expression) {
        TupleIteratorCalc<?> calc = (TupleIteratorCalc<?>) compileAs(expression, null, ResultStyle.ITERABLE_ONLY);
        if (calc == null) {
            calc = (TupleIteratorCalc<?>) compileAs(expression, null, ResultStyle.ANY_ONLY);
            assert calc != null;
        }
        return calc;
    }

    @Override
    public BooleanCalc compileBoolean(Expression expression) {
        final Calc<?> calc = compileScalar(expression, false);
        if (calc instanceof BooleanCalc booleanCalc) {
            return booleanCalc;
        }
        if (calc instanceof ConstantCalc constantCalc) {
            Boolean bool = switch (constantCalc.evaluate(null)) {
            // Deliberately NOT null: booleans stay two-valued (no 3VL), so a
            // NULL constant folds to BOOLEAN_NULL == false - null-to-false is
            // behavior, not sentinel encoding.
            case null -> FunUtil.BOOLEAN_NULL;
            case Boolean b -> b;
            case Number n -> n.intValue() > 0;
            case Object obj -> throw TypeConversionException.cannotConvert(obj, "Boolean");
            };
            return new ConstantBooleanCalc(BooleanType.INSTANCE, bool);
        }
        return switch (calc) {
        case DoubleCalc doubleCalc -> new DoubleToBooleanCalc(expression.getType(), doubleCalc);
        case IntegerCalc integerCalc -> new IntegerToBooleanCalc(expression.getType(), integerCalc);
        default -> new UnknownToBooleanCalc(expression.getType(), calc);
        };
    }

    @Override
    public DoubleCalc compileDouble(Expression expression) {
        final Calc<?> calc = compileScalar(expression, false);
        if (calc instanceof ConstantCalc<?> constantCalc) {
            Object evaluated = constantCalc.evaluate(null);
            if (!(evaluated instanceof Double)) {
                Double doub = switch (evaluated) {
                    // MDX NULL is Java null in the Double calc world.
                    case null -> null;
                    case Double d -> d;
                    case Number n -> n.doubleValue();
                    case Object obj -> throw TypeConversionException.cannotConvert(obj, "Double");
                };
                return new ConstantDoubleCalc(NumericType.INSTANCE, doub);
            }
        }
        return switch (calc) {
            case DoubleCalc doubleCalc -> doubleCalc;
            case IntegerCalc integerCalc -> new IntegerToDoubleCalc(expression.getType(), integerCalc);
            default -> new UnknownToDoubleCalc(NumericType.INSTANCE, calc);
        };
    }

    @Override
    public TupleCalc compileTuple(Expression expression) {
        return (TupleCalc) compile(expression);
    }

    @Override
    public Calc<?> compileScalar(Expression expression, boolean specific) {
        final Type type = expression.getType();
        return switch (type) {
        case MemberType _ -> memberToScalar(compileMember(expression));
        case DimensionType _,HierarchyType _ -> hierarchyToScalar(compileHierarchy(expression));
        case TupleType tupleType -> {
            final TupleCalc tupleCalc = compileTuple(expression);
            yield new TupleValueCalc(tupleType.getValueType(), tupleCalc,
                    getEvaluator().mightReturnNullForUnrelatedDimension()).optimize();
        }
        case ScalarType _ when specific -> compileSpecificScalar(expression, type);
        default -> compile(expression);
        };
    }

    private Calc<?> compileSpecificScalar(Expression expression, Type type) {
        return switch (type) {
        case BooleanType _ -> compileBoolean(expression);
        case NumericType _ -> compileDouble(expression);
        case StringType _ -> compileString(expression);
        default -> compile(expression);
        };
    }

    private Calc<?> hierarchyToScalar(HierarchyCalc hierarchyCalc) {
        final MemberCalc memberCalc = hierarchyToMember(hierarchyCalc);
        return memberToScalar(memberCalc);
    }

    private Calc<?> memberToScalar(MemberCalc memberCalc) {
        final MemberType memberType = (MemberType) memberCalc.getType();
        return MemberValueCalc.create(memberType.getValueType(), new MemberCalc[] { memberCalc },
                getEvaluator().mightReturnNullForUnrelatedDimension());
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
            calc = compileScalar(defaultExp, true);
        } else {
            calc = compileAs(defaultExp, type, resultStyles);
        }
        slot2.setDefaultValueCalc(calc);
        return slot2;
    }

    @Override
    public List<ResultStyle> getAcceptableResultStyles() {
        return resultStyles;
    }
}
