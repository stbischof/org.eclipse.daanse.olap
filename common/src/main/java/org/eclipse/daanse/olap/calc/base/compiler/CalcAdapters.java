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
 */
package org.eclipse.daanse.olap.calc.base.compiler;

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
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.exception.TypeConversionException;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.api.type.DimensionType;
import org.eclipse.daanse.olap.api.type.HierarchyType;
import org.eclipse.daanse.olap.api.type.LevelType;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.NullType;
import org.eclipse.daanse.olap.api.type.NumericType;
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
import org.eclipse.daanse.olap.calc.base.type.hierarchy.HierarchyCurrentMemberCalc;
import org.eclipse.daanse.olap.calc.base.type.hierarchy.HierarchyCurrentMemberFixedCalc;
import org.eclipse.daanse.olap.calc.base.type.hierarchy.LevelHierarchyCalc;
import org.eclipse.daanse.olap.calc.base.type.hierarchy.MemberHierarchyCalc;
import org.eclipse.daanse.olap.calc.base.type.integer.DoubleToIntegerCalc;
import org.eclipse.daanse.olap.calc.base.type.integer.UnknownToIntegerCalc;
import org.eclipse.daanse.olap.calc.base.type.level.MemberLevelCalc;
import org.eclipse.daanse.olap.calc.base.type.level.UnknownToLevelCalc;
import org.eclipse.daanse.olap.calc.base.type.member.UnknownToMemberCalc;
import org.eclipse.daanse.olap.calc.base.type.string.UnknownToStringCalc;
import org.eclipse.daanse.olap.calc.base.type.tuple.MemberCalcToTupleCalc;
import org.eclipse.daanse.olap.calc.base.type.tuple.UnknownToTupleCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.CopyingListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.IterableListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.MemberValueCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleValueCalc;
import org.eclipse.daanse.olap.calc.base.util.DimensionUtil;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.util.type.TypeUtil;

/**
 * The conversions of the compiler, one function each: a calc of one kind into a calc of another, constants folded.
 * They need no compiler -- the calc, the type of its expression and, for the value of a member or
 * a tuple, whether the evaluator may return null for an unrelated dimension -- so each is tested alone
 * ({@code CalcAdaptersTest}). {@link StandardExpressionCompiler} compiles, then hands the calc to one of these.
 */
public final class CalcAdapters {

    /** The type of an integer: a decimal of scale 0. */
    public static final DecimalType INTEGER_TYPE = new DecimalType(Integer.MAX_VALUE, 0);

    private CalcAdapters() {
    }

    /** The calc of a member expression as a member calc: itself, or a cast when it is evaluated. */
    public static MemberCalc asMember(Type type, Calc<?> calc) {
        return calc instanceof MemberCalc memberCalc ? memberCalc : new UnknownToMemberCalc(type, calc);
    }

    /** The current member of a hierarchy: fixed to the hierarchy when the type names it at compile time. */
    public static MemberCalc currentMember(HierarchyCalc hierarchyCalc) {
        final Hierarchy hierarchy = hierarchyCalc.getType().getHierarchy();
        if (hierarchy != null) {
            return new HierarchyCurrentMemberFixedCalc(TypeUtil.toMemberType(hierarchyCalc.getType()), hierarchy);
        }
        return new HierarchyCurrentMemberCalc(TypeUtil.toMemberType(hierarchyCalc.getType()), hierarchyCalc);
    }

    /** {@code <Member>.Level}, typed after the expression ({@code <Dimension>} goes through its current member). */
    public static LevelCalc levelOf(Type type, MemberCalc memberCalc) {
        return new MemberLevelCalc(LevelType.forType(type), memberCalc);
    }

    /** The calc of a level expression as a level calc. */
    public static LevelCalc asLevel(Type type, Calc<?> calc) {
        return calc instanceof LevelCalc levelCalc ? levelCalc : new UnknownToLevelCalc(type, calc);
    }

    /** {@code <Hierarchy>.Dimension}. */
    public static DimensionCalc dimensionOf(Type type, HierarchyCalc hierarchyCalc) {
        return new DimensionOfHierarchyCalc(new DimensionType(type.getDimension()), hierarchyCalc);
    }

    /** The calc of a dimension expression as a dimension calc. */
    public static DimensionCalc asDimension(Type type, Calc<?> calc) {
        return calc instanceof DimensionCalc dimensionCalc ? dimensionCalc : new UnknownToDimensionCalc(type, calc);
    }

    /**
     * The default hierarchy of a dimension known at compile time, as a constant; null when the type names no dimension
     * (then {@link #defaultHierarchy} works it out when evaluated). A dimension of several hierarchies is refused.
     */
    public static HierarchyCalc constantDefaultHierarchy(Type type) {
        final Dimension dimension = type.getDimension();
        if (dimension == null) {
            return null;
        }
        final Hierarchy hierarchy = DimensionUtil.getDimensionDefaultHierarchyOrThrow(dimension);
        return hierarchy == null ? null : ConstantHierarchyCalc.of(hierarchy);
    }

    /** The default hierarchy of the dimension a calc yields. */
    public static HierarchyCalc defaultHierarchy(Type type, DimensionCalc dimensionCalc) {
        return new DimensionDefaultHierarchyCalc(HierarchyType.forType(type), dimensionCalc);
    }

    /** {@code <Member>.Hierarchy}. */
    public static HierarchyCalc hierarchyOf(Type type, MemberCalc memberCalc) {
        return new MemberHierarchyCalc(HierarchyType.forType(type), memberCalc);
    }

    /** {@code <Level>.Hierarchy}. */
    public static HierarchyCalc hierarchyOf(Type type, LevelCalc levelCalc) {
        return new LevelHierarchyCalc(HierarchyType.forType(type), levelCalc);
    }

    /** A scalar as an integer: a whole decimal stays, a constant folds (truncated), a double is cut, the rest cast. */
    public static IntegerCalc asInteger(Calc<?> scalar, Type expressionType) {
        final Type type = scalar.getType();
        if (type instanceof DecimalType decimalType && decimalType.getScale() == 0) {
            return (IntegerCalc) scalar;
        }
        return switch (type) {
        case NullType _ when scalar instanceof ConstantCalc<?> _ -> new ConstantIntegerCalc(INTEGER_TYPE, null);
        case NumericType _ -> switch (scalar) {
            case ConstantCalc<?> constantCalc -> {
                Object o = constantCalc.evaluate(null);
                Integer i = o == null ? null : ((Number) o).intValue();
                yield new ConstantIntegerCalc(INTEGER_TYPE, i);
            }
            case DoubleCalc doubleCalc -> new DoubleToIntegerCalc(expressionType, doubleCalc);
            case IntegerCalc integerCalc -> integerCalc;
            default -> new UnknownToIntegerCalc(INTEGER_TYPE, scalar);
        };
        default -> new UnknownToIntegerCalc(INTEGER_TYPE, scalar);
        };
    }

    /** A scalar as a string: a constant folds to its text, the rest is cast when evaluated. */
    public static StringCalc asString(Calc<?> scalar) {
        return switch (scalar) {
        case StringCalc stringCalc -> stringCalc;
        case ConstantCalc<?> constantCalc -> {
            Object value = constantCalc.evaluate(null);
            yield new ConstantStringCalc(StringType.INSTANCE, value == null ? null : value.toString());
        }
        default -> new UnknownToStringCalc(StringType.INSTANCE, scalar);
        };
    }

    /** A scalar as a date and time. */
    public static DateTimeCalc asDateTime(Calc<?> scalar) {
        return scalar instanceof DateTimeCalc dateTimeCalc ? dateTimeCalc
                : new UnknownToDateTimeCalc(scalar.getType(), scalar);
    }

    /** A scalar as a truth value: every number other than nought is true, NULL is false, a text is refused. */
    public static BooleanCalc asBoolean(Calc<?> scalar, Type expressionType) {
        if (scalar instanceof BooleanCalc booleanCalc) {
            return booleanCalc;
        }
        if (scalar instanceof ConstantCalc<?> constantCalc) {
            Boolean bool = switch (constantCalc.evaluate(null)) {
            // Deliberately NOT null: booleans stay two-valued (no 3VL), so a
            // NULL constant folds to BOOLEAN_NULL == false - null-to-false is
            // behavior, not sentinel encoding.
            case null -> FunUtil.BOOLEAN_NULL;
            case Boolean b -> b;
            // Every number other than zero is true, as DoubleToBooleanCalc below has it: cutting
            // the fraction away would make 0.5 false, and asking for more than zero would make -1 false.
            case Number num -> {
                final double d = num.doubleValue();
                yield !Double.isNaN(d) && d != 0;
            }
            case Object obj -> throw TypeConversionException.cannotConvert(obj, "Boolean");
            };
            return new ConstantBooleanCalc(BooleanType.INSTANCE, bool);
        }
        return switch (scalar) {
        case DoubleCalc doubleCalc -> new DoubleToBooleanCalc(expressionType, doubleCalc);
        case IntegerCalc integerCalc -> new IntegerToBooleanCalc(expressionType, integerCalc);
        default -> new UnknownToBooleanCalc(expressionType, scalar);
        };
    }

    /** A scalar as a double: a constant folds (MDX NULL is Java null), an integer widens, the rest is cast. */
    public static DoubleCalc asDouble(Calc<?> scalar, Type expressionType) {
        if (scalar instanceof ConstantCalc<?> constantCalc) {
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
        return switch (scalar) {
        case DoubleCalc doubleCalc -> doubleCalc;
        case IntegerCalc integerCalc -> new IntegerToDoubleCalc(expressionType, integerCalc);
        default -> new UnknownToDoubleCalc(NumericType.INSTANCE, scalar);
        };
    }

    /**
     * The calc of a tuple or member expression as a tuple calc: a member is a tuple of one. A dimension or hierarchy
     * needs its default member, which only the compiler can resolve; it is not converted here.
     */
    public static TupleCalc asTuple(Type type, Calc<?> calc) {
        return switch (type) {
        case TupleType _ -> calc instanceof TupleCalc tupleCalc ? tupleCalc : new UnknownToTupleCalc(type, calc);
        case MemberType _ -> new MemberCalcToTupleCalc(type, asMember(type, calc));
        default -> throw new TypeConversionException("no tuple can be worked out from " + type);
        };
    }

    /**
     * A list for a consumer that will change it: a copy of one only promised as LIST. Mondrian 3
     * and 4 make this copy in BetterExpCompiler.compileList (CopyListCalc, cloneList): an axis, NativizeSet or a
     * function sorting in place must not change a list its producer keeps (a named set, a cached member list).
     */
    public static TupleListCalc mutableIfAsked(TupleListCalc calc, boolean mutable) {
        if (mutable && calc.getResultStyle() == ResultStyle.LIST) {
            return new CopyingListCalc(calc);
        }
        return calc;
    }

    /** An iterable as a list. */
    public static TupleListCalc listOf(TupleIteratorCalc<?> calc) {
        return new IterableListCalc(calc);
    }

    /** The value of the current measure at a member. */
    public static Calc<?> valueOf(MemberCalc memberCalc, boolean nullForUnrelatedDimension) {
        final MemberType memberType = (MemberType) memberCalc.getType();
        return MemberValueCalc.create(memberType.getValueType(), new MemberCalc[] { memberCalc },
                nullForUnrelatedDimension);
    }

    /** The value of the current measure at a tuple. */
    public static Calc<?> valueOf(TupleType tupleType, TupleCalc tupleCalc, boolean nullForUnrelatedDimension) {
        return new TupleValueCalc(tupleType.getValueType(), tupleCalc, nullForUnrelatedDimension).optimize();
    }
}
