/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.calc.base.profile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.ByteCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ConstantCalc;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.DimensionCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.FloatCalc;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.LongCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.TupleIterableCalc;
import org.eclipse.daanse.olap.api.calc.VoidCalc;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;

/**
 * The hull the {@link ProfilingCompiler} puts around each compiled calc of a profiled statement: it counts the
 * evaluations, sums their nanoseconds and, for a calc of sets, the elements and their squares, and passes type, result
 * style, {@code dependsOn} and children through. In the plan it does not appear; its numbers stand at the calc it
 * holds. One subclass per typed calc interface, so that a function that asks for a {@code DoubleCalc} still gets one.
 * Not synchronized: the counts of a calc evaluated by several threads at once are approximate.
 */
public abstract class ProfilingCalc<T> implements Calc<T> {

    private final Calc<T> calc;
    private final String name;
    private long calls;
    private long nanos;
    private long elements;
    private long elementsSquared;

    ProfilingCalc(Calc<T> calc, String name) {
        this.calc = calc;
        this.name = name;
    }

    /**
     * The calc in a hull for profiling, of the same typed interface. A constant stays bare: the compiler and
     * functions recognise constants by {@code instanceof ConstantCalc}, and a constant costs nothing to time.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Calc<?> wrap(Calc<?> calc, String name) {
        if (calc == null || calc instanceof ProfilingCalc || calc instanceof ConstantCalc) {
            return calc;
        }
        if (calc instanceof TupleListCalc typed) {
            return new ProfilingTupleList(typed, name);
        }
        if (calc instanceof TupleIteratorCalc<?> typed) {
            return new ProfilingTupleIterator(typed, name);
        }
        if (calc instanceof BooleanCalc typed) {
            return new ProfilingBoolean(typed, name);
        }
        if (calc instanceof ByteCalc typed) {
            return new ProfilingByte(typed, name);
        }
        if (calc instanceof DateTimeCalc typed) {
            return new ProfilingDateTime(typed, name);
        }
        if (calc instanceof DimensionCalc typed) {
            return new ProfilingDimension(typed, name);
        }
        if (calc instanceof DoubleCalc typed) {
            return new ProfilingDouble(typed, name);
        }
        if (calc instanceof FloatCalc typed) {
            return new ProfilingFloat(typed, name);
        }
        if (calc instanceof HierarchyCalc typed) {
            return new ProfilingHierarchy(typed, name);
        }
        if (calc instanceof IntegerCalc typed) {
            return new ProfilingInteger(typed, name);
        }
        if (calc instanceof LevelCalc typed) {
            return new ProfilingLevel(typed, name);
        }
        if (calc instanceof LongCalc typed) {
            return new ProfilingLong(typed, name);
        }
        if (calc instanceof MemberCalc typed) {
            return new ProfilingMember(typed, name);
        }
        if (calc instanceof StringCalc typed) {
            return new ProfilingString(typed, name);
        }
        if (calc instanceof TupleCalc typed) {
            return new ProfilingTuple(typed, name);
        }
        if (calc instanceof TupleIterableCalc typed) {
            return new ProfilingTupleIterable(typed, name);
        }
        if (calc instanceof VoidCalc typed) {
            return new ProfilingVoid(typed, name);
        }
        return new ProfilingCalc(calc, name) {
        };
    }

    /** The calc a hull holds, or the calc itself: for checks of the concrete class of a compiled calc. */
    public static Calc<?> unwrap(Calc<?> calc) {
        return calc instanceof ProfilingCalc<?> hull ? hull.calc : calc;
    }

    @Override
    public T evaluate(Evaluator evaluator) {
        long start = System.nanoTime();
        try {
            T result = calc.evaluate(evaluator);
            if (result instanceof Collection<?> collection) {
                long size = collection.size();
                elements += size;
                elementsSquared += size * size;
            }
            return result;
        } finally {
            nanos += System.nanoTime() - start;
            calls++;
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return calc.dependsOn(hierarchy);
    }

    @Override
    public Type getType() {
        return calc.getType();
    }

    @Override
    public ResultStyle getResultStyle() {
        return calc.getResultStyle();
    }

    /** The profile of the calc held, with the numbers of this hull and the function name of its expression. */
    @Override
    public CalculationProfile getCalculationProfile() {
        CalculationProfile inner = calc.getCalculationProfile();
        Map<String, Object> values = new HashMap<>(inner.additionalValues());
        values.put("callCount", calls);
        return new CalcProfileR(inner.clazz(), Optional.ofNullable(name).or(inner::name), inner.type(),
                inner.resultStyle(), nanos, elements, elementsSquared, values,
                inner.childProfiles());
    }

    static final class ProfilingBoolean extends ProfilingCalc<Boolean> implements BooleanCalc {
        ProfilingBoolean(BooleanCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingByte extends ProfilingCalc<Byte> implements ByteCalc {
        ProfilingByte(ByteCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingDateTime extends ProfilingCalc<LocalDateTime> implements DateTimeCalc {
        ProfilingDateTime(DateTimeCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingDimension extends ProfilingCalc<Dimension> implements DimensionCalc {
        ProfilingDimension(DimensionCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingDouble extends ProfilingCalc<Double> implements DoubleCalc {
        ProfilingDouble(DoubleCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingFloat extends ProfilingCalc<Float> implements FloatCalc {
        ProfilingFloat(FloatCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingHierarchy extends ProfilingCalc<Hierarchy> implements HierarchyCalc {
        ProfilingHierarchy(HierarchyCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingInteger extends ProfilingCalc<Integer> implements IntegerCalc {
        ProfilingInteger(IntegerCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingLevel extends ProfilingCalc<Level> implements LevelCalc {
        ProfilingLevel(LevelCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingLong extends ProfilingCalc<Long> implements LongCalc {
        ProfilingLong(LongCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingMember extends ProfilingCalc<Member> implements MemberCalc {
        ProfilingMember(MemberCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingString extends ProfilingCalc<String> implements StringCalc {
        ProfilingString(StringCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingTuple extends ProfilingCalc<Member[]> implements TupleCalc {
        ProfilingTuple(TupleCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingTupleIterable extends ProfilingCalc<TupleIterable> implements TupleIterableCalc {
        ProfilingTupleIterable(TupleIterableCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingTupleList extends ProfilingCalc<TupleList> implements TupleListCalc {
        ProfilingTupleList(TupleListCalc calc, String name) {
            super(calc, name);
        }
    }
    static final class ProfilingVoid extends ProfilingCalc<Void> implements VoidCalc {
        ProfilingVoid(VoidCalc calc, String name) {
            super(calc, name);
        }
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static final class ProfilingTupleIterator extends ProfilingCalc implements TupleIteratorCalc {
        ProfilingTupleIterator(TupleIteratorCalc<?> calc, String name) {
            super(calc, name);
        }
    }
}
