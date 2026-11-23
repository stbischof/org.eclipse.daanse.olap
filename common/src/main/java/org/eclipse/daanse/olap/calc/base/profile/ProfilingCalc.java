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
import org.eclipse.daanse.olap.api.calc.profile.ProfileCounters;
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
 * The hull the {@link ProfilingHook} puts around each compiled calc of a profiled statement: it counts the
 * evaluations, sums their nanoseconds and, for a calc of sets, the elements and their squares, and passes type, result
 * style, {@code dependsOn} and children through. In the plan it does not appear; its numbers stand at the calc it
 * holds. One subclass per typed calc interface, so that a function that asks for a {@code DoubleCalc} still gets one.
 * Not synchronized: the counts of a calc evaluated by several threads at once are approximate.
 */
public abstract class ProfilingCalc<T> implements Calc<T> {

    private final Calc<T> calc;
    private final String name;
    /** Where every evaluation is counted as well (the counters sink), or {@code null}. */
    private final ProfileCounters.Function counter;
    private long calls;
    private long nanos;
    private long ownNanos;
    private long elements;
    private long elementsSquared;

    /**
     * The time the hulls called by the running evaluation took, per thread: a hull starts it at 0, the hulls inside
     * add their time, and what is left of its own time is the hull's own. One slot, saved and restored around each
     * evaluation, so the nesting needs no stack.
     */
    private static final ThreadLocal<long[]> CALLED = ThreadLocal.withInitial(() -> new long[1]);

    ProfilingCalc(Calc<T> calc, String name, ProfileCounters.Function counter) {
        this.calc = calc;
        this.name = name;
        this.counter = counter;
    }

    /**
     * The calc in a hull for profiling, of the same typed interface. A constant stays bare: the compiler and
     * functions recognise constants by {@code instanceof ConstantCalc}, and a constant costs nothing to time.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Calc<?> wrap(Calc<?> calc, String name) {
        return wrap(calc, name, null);
    }

    /**
     * As {@link #wrap(Calc, String)}, and every evaluation is counted in {@code counters} as well, under the function
     * name or, where nobody named the calc, its simple class name.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Calc<?> wrap(Calc<?> calc, String name, ProfileCounters counters) {
        if (calc == null || calc instanceof ProfilingCalc || calc instanceof ConstantCalc) {
            return calc;
        }
        ProfileCounters.Function counter = counters == null ? null
                : counters.function(name != null ? name : calc.getClass().getSimpleName());
        if (calc instanceof TupleListCalc typed) {
            return new ProfilingTupleList(typed, name, counter);
        }
        if (calc instanceof TupleIteratorCalc<?> typed) {
            return new ProfilingTupleIterator(typed, name, counter);
        }
        if (calc instanceof BooleanCalc typed) {
            return new ProfilingBoolean(typed, name, counter);
        }
        if (calc instanceof ByteCalc typed) {
            return new ProfilingByte(typed, name, counter);
        }
        if (calc instanceof DateTimeCalc typed) {
            return new ProfilingDateTime(typed, name, counter);
        }
        if (calc instanceof DimensionCalc typed) {
            return new ProfilingDimension(typed, name, counter);
        }
        if (calc instanceof DoubleCalc typed) {
            return new ProfilingDouble(typed, name, counter);
        }
        if (calc instanceof FloatCalc typed) {
            return new ProfilingFloat(typed, name, counter);
        }
        if (calc instanceof HierarchyCalc typed) {
            return new ProfilingHierarchy(typed, name, counter);
        }
        if (calc instanceof IntegerCalc typed) {
            return new ProfilingInteger(typed, name, counter);
        }
        if (calc instanceof LevelCalc typed) {
            return new ProfilingLevel(typed, name, counter);
        }
        if (calc instanceof LongCalc typed) {
            return new ProfilingLong(typed, name, counter);
        }
        if (calc instanceof MemberCalc typed) {
            return new ProfilingMember(typed, name, counter);
        }
        if (calc instanceof StringCalc typed) {
            return new ProfilingString(typed, name, counter);
        }
        if (calc instanceof TupleCalc typed) {
            return new ProfilingTuple(typed, name, counter);
        }
        if (calc instanceof TupleIterableCalc typed) {
            return new ProfilingTupleIterable(typed, name, counter);
        }
        if (calc instanceof VoidCalc typed) {
            return new ProfilingVoid(typed, name, counter);
        }
        return new ProfilingCalc(calc, name, counter) {
        };
    }

    /** The calc a hull holds, or the calc itself: for checks of the concrete class of a compiled calc. */
    public static Calc<?> unwrap(Calc<?> calc) {
        return calc instanceof ProfilingCalc<?> hull ? hull.calc : calc;
    }

    @Override
    public T evaluate(Evaluator evaluator) {
        long[] called = CALLED.get();
        long calledBefore = called[0];
        called[0] = 0;
        long size = 0;
        long start = System.nanoTime();
        try {
            T result = calc.evaluate(evaluator);
            if (result instanceof Collection<?> collection) {
                size = collection.size();
                elements += size;
                elementsSquared += size * size;
            }
            return result;
        } finally {
            long took = System.nanoTime() - start;
            long own = Math.max(0, took - called[0]);
            called[0] = calledBefore + took;
            nanos += took;
            ownNanos += own;
            calls++;
            if (counter != null) {
                counter.add(took, own, size);
            }
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
                inner.resultStyle(), nanos, ownNanos, elements, elementsSquared, values,
                inner.childProfiles());
    }

    static final class ProfilingBoolean extends ProfilingCalc<Boolean> implements BooleanCalc {
        ProfilingBoolean(BooleanCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingByte extends ProfilingCalc<Byte> implements ByteCalc {
        ProfilingByte(ByteCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingDateTime extends ProfilingCalc<LocalDateTime> implements DateTimeCalc {
        ProfilingDateTime(DateTimeCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingDimension extends ProfilingCalc<Dimension> implements DimensionCalc {
        ProfilingDimension(DimensionCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingDouble extends ProfilingCalc<Double> implements DoubleCalc {
        ProfilingDouble(DoubleCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingFloat extends ProfilingCalc<Float> implements FloatCalc {
        ProfilingFloat(FloatCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingHierarchy extends ProfilingCalc<Hierarchy> implements HierarchyCalc {
        ProfilingHierarchy(HierarchyCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingInteger extends ProfilingCalc<Integer> implements IntegerCalc {
        ProfilingInteger(IntegerCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingLevel extends ProfilingCalc<Level> implements LevelCalc {
        ProfilingLevel(LevelCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingLong extends ProfilingCalc<Long> implements LongCalc {
        ProfilingLong(LongCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingMember extends ProfilingCalc<Member> implements MemberCalc {
        ProfilingMember(MemberCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingString extends ProfilingCalc<String> implements StringCalc {
        ProfilingString(StringCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingTuple extends ProfilingCalc<Member[]> implements TupleCalc {
        ProfilingTuple(TupleCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingTupleIterable extends ProfilingCalc<TupleIterable> implements TupleIterableCalc {
        ProfilingTupleIterable(TupleIterableCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingTupleList extends ProfilingCalc<TupleList> implements TupleListCalc {
        ProfilingTupleList(TupleListCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    static final class ProfilingVoid extends ProfilingCalc<Void> implements VoidCalc {
        ProfilingVoid(VoidCalc calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static final class ProfilingTupleIterator extends ProfilingCalc implements TupleIteratorCalc {
        ProfilingTupleIterator(TupleIteratorCalc<?> calc, String name, ProfileCounters.Function counter) {
            super(calc, name, counter);
        }
    }
}
