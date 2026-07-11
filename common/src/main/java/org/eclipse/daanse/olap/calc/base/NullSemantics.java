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
 * The engine historically encoded MDX NULL with the in-band sentinel
 * {@link Util#DOUBLE_NULL} / {@link Util#nullValue}. Migration (see
 * the null-semantics notes)
 * has been executed for the {@code Double} side of the calc layer: a
 * {@code DoubleCalc} now represents MDX NULL as Java {@code null}, and
 * {@link #isNull(Double)} is a plain {@code v == null} check. A computed
 * {@code Double} of value {@code 0.000000012345} is an ordinary value
 * everywhere (the historical collision is healed).
 *
 * <p>
 * The object/cell side ({@link #isNullValue(Object)}, {@link #isNull(Object)},
 * {@link #equalsNullValue(Object)}, {@link #compareCellValues(Object, Object)})
 * still recognizes the {@link Util#nullValue} sentinel; it switches to a
 * dedicated {@code CellValue} state in the null-semantics migration.
 */
public final class NullSemantics {

    private NullSemantics() {
        // utility class
    }

    /**
     * Boxed NULL check: since , MDX NULL in the {@code Double} calc
     * world IS Java {@code null}. The former {@link Util#DOUBLE_NULL} sentinel
     * is an ordinary value.
 */
    public static boolean isNull(Double v) {
        return v == null;
    }

    /**
     * Identical to {@link #isNull(Double)}.
     *
     * @deprecated The strict-sentinel idiom this helper mirrored was folded
     *             into {@link #isNull(Double)} when replaced the
     *             {@code DOUBLE_NULL} sentinel with Java {@code null}; the
     *             method is retained for API stability and will be removed in
     *             (see
     *             the null-semantics notes).
 */
    @Deprecated(forRemoval = true)
    public static boolean isSentinelOnly(Double v) {
        return v == null;
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
     * {@code -Inf < values < NaN < +Inf} (MSAS-compatible NaN placement).
     * Canonical implementation of the former duplicate in {@code FunUtil} and
     * {@code Sorter}.
     *
     * <p>
     * A primitive {@code double} cannot carry MDX NULL
     * (NULL is Java {@code null} at the boxed level, handled by
     * {@link #compareCellValues(Object, Object)}), so this order has no NULL
     * slot anymore.
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
