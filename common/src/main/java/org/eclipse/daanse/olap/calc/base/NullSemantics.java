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
package org.eclipse.daanse.olap.calc.base;

import java.time.LocalDateTime;
import java.util.Date;

import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.sort.OrderKey;

/**
 * Central home of the MDX NULL semantics of the calculation layer.
 *
 * <p>
 * The engine historically encodes MDX NULL with the in-band sentinel
 * {@link Util#DOUBLE_NULL} / {@link Util#nullValue} and checks it with four
 * different idioms scattered over dozens of call sites (boxed identity
 * {@code ==}, unboxed value {@code ==}, {@code equals}, {@code Objects.equals}).
 * This class gives each idiom exactly one named helper so that the sentinel
 * representation can later be swapped for Java {@code null} in a single place
 *.
 *
 * <p>
 * <b>Transitional API.</b> Until the helpers reproduce today's
 * behavior bit for bit; the distinction between {@link #isNull(Double)},
 * {@link #isSentinelOnly(Double)} and {@link #isNull(double)} exists only
 * because the historical idioms differ in null tolerance and in identity- vs
 * value-comparison. Once the sentinel is gone the value-based variants
 * disappear and everything collapses to {@code v == null}.
 */
public final class NullSemantics {

    private NullSemantics() {
        // utility class
    }

    /**
     * Boxed NULL check, tolerant of both representations. Mirrors the
     * {@code v == FunUtil.DOUBLE_NULL || v == null} idiom of the arithmetic
     * operator calcs: identity comparison against the sentinel singleton, plus
     * Java {@code null}.
 */
    public static boolean isNull(Double v) {
        return v == null || isSentinelOnly(v);
    }

    /**
     * Strict sentinel check without Java-{@code null} tolerance. Mirrors the
     * bare {@code v == FunUtil.DOUBLE_NULL} idiom (comparison operators, most
     * excel calcs, {@code DoubleToBooleanCalc}) whose call sites today throw
     * NPE further down when handed a Java {@code null}. Folds into
     * {@link #isNull(Double)}.
 */
    public static boolean isSentinelOnly(Double v) {
        // Deliberate reference comparison: only the sentinel singleton is
        // NULL here; a runtime-computed Double of equal value is a real value.
        return ((Object) v) == ((Object) Util.DOUBLE_NULL);
    }

    /**
     * Unboxed NULL check. Mirrors the primitive {@code d == DOUBLE_NULL}
     * idiom (comparators, {@code FunUtil.sum}): a value comparison, hence
     * inherently collision-prone — any computation that yields exactly
     * {@code 0.000000012345} is mistaken for NULL. Disappears.
 */
    public static boolean isNull(double v) {
        return v == Util.DOUBLE_NULL;
    }

    /**
     * Strict object-level check against the {@link Util#nullValue} cell
     * sentinel (identity). Mirrors the bare {@code o == Util.nullValue} idiom.
 */
    public static boolean isNullValue(Object o) {
        return o == Util.nullValue;
    }

    /**
     * Tolerant object-level check: Java {@code null} or the
     * {@link Util#nullValue} sentinel. Same contract as {@link Util#isNull}.
 */
    public static boolean isNull(Object o) {
        return o == null || o == Util.nullValue;
    }

    /**
     * Value-based object-level check mirroring the historical
     * {@code Objects.equals(o, Util.nullValue)} idiom of the cell layer
     * ({@code RolapCell.isNull}, {@code RolapResult}): unlike
     * {@link #isNullValue(Object)} this also matches Java {@code null} and —
     * collision-prone — any runtime-computed Double of equal value.
     * Transitional until the cell layer reports state objects instead.
 */
    public static boolean equalsNullValue(Object o) {
        return java.util.Objects.equals(o, Util.nullValue);
    }

    /**
     * Compares two unboxed cell values forming the total order
     * {@code -Inf < NULL < values < NaN < +Inf} (MSAS-compatible). Canonical
     * implementation of the former duplicate in {@code FunUtil} and
     * {@code Sorter}.
 */
    public static int compare(double d1, double d2) {
        if (Double.isNaN(d1)) {
            if (d2 == Double.POSITIVE_INFINITY) {
                return -1;
            } else if (Double.isNaN(d2)) {
                return 0;
            } else {
                return 1;
            }
        } else if (Double.isNaN(d2)) {
            if (d1 == Double.POSITIVE_INFINITY) {
                return 1;
            } else {
                return -1;
            }
        } else if (d1 == d2) {
            return 0;
        } else if (isNull(d1)) {
            if (d2 == Double.NEGATIVE_INFINITY) {
                return 1;
            } else {
                return -1;
            }
        } else if (isNull(d2)) {
            if (d1 == Double.NEGATIVE_INFINITY) {
                return -1;
            } else {
                return 1;
            }
        } else if (d1 < d2) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Compares two boxed/object cell values. Canonical implementation of the
     * former near-duplicate in {@code FunUtil} and {@code Sorter} (superset:
     * supports both {@code LocalDateTime} and {@code OrderKey}).
     *
     * <p>
     * NOTE the historical asymmetry, frozen by the characterization
     * suite: at this object level, Java {@code null} and the
     * {@link Util#nullValue} sentinel sort below <em>everything including
     * -Infinity</em> — unlike the unboxed {@link #compare(double, double)}
     * where NULL sits between -Infinity and real values. Only value-equal
     * non-singleton Doubles fall through to the unboxed order.
 */
    public static int compareCellValues(Object value0, Object value1) {
        if (value0 == value1) {
            return 0;
        }
        // null is less than anything else
        if (value0 == null) {
            return -1;
        }
        if (value1 == null) {
            return 1;
        }

        if (value0 == NotLoaded.INSTANCE) {
            // the left value is not in cache; continue as best as we can
            return -1;
        } else if (value1 == NotLoaded.INSTANCE) {
            // the right value is not in cache; continue as best as we can
            return 1;
        } else if (isNullValue(value0)) {
            return -1; // null == -infinity
        } else if (isNullValue(value1)) {
            return 1; // null == -infinity
        } else if (value0 instanceof String str) {
            return str.compareToIgnoreCase((String) value1);
        } else if (value0 instanceof Number numberValue0) {
            return compare(numberValue0.doubleValue(), ((Number) value1).doubleValue());
        } else if (value0 instanceof Date date) {
            return date.compareTo((Date) value1);
        } else if (value0 instanceof LocalDateTime localDateTime) {
            return localDateTime.compareTo((LocalDateTime) value1);
        } else if (value0 instanceof OrderKey orderKey && value1 instanceof OrderKey orderKeyOther) {
            return orderKey.compareTo(orderKeyOther);
        } else {
            throw Util.newInternal("cannot compare " + value0);
        }
    }
}
