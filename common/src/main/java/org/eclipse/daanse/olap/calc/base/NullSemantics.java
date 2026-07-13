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
 * Single home of the MDX NULL semantics of the calculation layer: the NULL
 * checks and the MSAS-compatible value ordering.
 *
 * <p>
 * MDX NULL is represented as Java {@code null} throughout the calc and cell
 * layers. The comparison methods define the total order used by sorts and
 * rank functions: {@code -Inf < values < NaN < +Inf} for numbers, with NULL
 * (and the not-yet-loaded marker of the dirty evaluation pass) sorting below
 * everything at the object level.
 */
public final class NullSemantics {

    private NullSemantics() {
        // utility class
    }

    /**
     * Boxed NULL check: MDX NULL in the {@code Double} calc world is Java
     * {@code null}.
     */
    public static boolean isNull(Double v) {
        return v == null;
    }

    /**
     * Object-level NULL check: MDX NULL at the cell/object level is Java
     * {@code null}.
     */
    public static boolean isNull(Object o) {
        return o == null;
    }

    /**
     * Compares two unboxed cell values forming the total order
     * {@code -Inf < values < NaN < +Inf} (MSAS-compatible NaN placement).
     *
     * <p>
     * A primitive {@code double} cannot carry MDX NULL (NULL is Java
     * {@code null} at the boxed level, handled by
     * {@link #compareCellValues(Object, Object)}), so this order has no NULL
     * slot.
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
     * Compares two boxed/object cell values: Java {@code null} (MDX NULL)
     * sorts below everything including {@code -Infinity}, the
     * {@link NotLoaded} marker of the dirty evaluation pass next, then typed
     * values are compared — Strings case-insensitively, Numbers by the
     * unboxed order of {@link #compare(double, double)}, dates and
     * {@link OrderKey}s naturally.
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
