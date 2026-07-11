/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.nullsemantics;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.fun.sort.Sorter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * characterization tests for the NULL ordering comparators.
 *
 * These tests freeze TODAY's behavior as a safety net for the NULL-semantics
 * refactoring (.
 *
 * Both {@code FunUtil.compareValues} and {@code Sorter.compareValues} are
 * tested independently: they are duplicates that will be merged in ,
 * so the behavior of BOTH must be frozen.
 *
 * Frozen characteristics:
 * - double overloads implement the MDX total order
 *   -Inf &lt; NULL &lt; values &lt; NaN &lt; +Inf, where NULL is the primitive
 *   sentinel value 0.000000012345 (VALUE-based: any double equal to the
 *   sentinel sorts as NULL — a collision).
 * - Object overloads sort {@code Util.nullValue} below EVERYTHING, including
 *   -Infinity — subtly different from the double overloads, where
 *   -Inf &lt; NULL.
 */
class NullOrderingTest {

    private static final double SENTINEL_VALUE = Double.parseDouble("0.000000012345");

    /**
     * The frozen total order of the double overloads:
     * -Inf &lt; NULL &lt; -1 &lt; 0 &lt; 1 &lt; NaN &lt; +Inf.
 */
    private static final double[] DOUBLE_ORDER = {
            Double.NEGATIVE_INFINITY,
            FunUtil.DOUBLE_NULL, // unboxes to the sentinel value
            -1.0,
            0.0,
            1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY };

    private static final String[] DOUBLE_ORDER_NAMES = {
            "-Inf", "NULL(sentinel)", "-1", "0", "1", "NaN", "+Inf" };

    /**
     * The frozen total order of the Object overloads: Util.nullValue sorts
     * below everything, EVEN below -Infinity (the double overloads put -Inf
     * below NULL instead).
 */
    private static final Object[] OBJECT_ORDER = {
            Util.nullValue,
            Double.NEGATIVE_INFINITY,
            -1.0,
            0.0,
            1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY };

    private static final String[] OBJECT_ORDER_NAMES = {
            "nullValue", "-Inf", "-1", "0", "1", "NaN", "+Inf" };

    @Test
    @DisplayName("FunUtil.compareValues(double,double): full pairwise order -Inf < NULL < -1 < 0 < 1 < NaN < +Inf")
    void funUtilDoubleOverloadPairwiseOrder() {
        for (int i = 0; i < DOUBLE_ORDER.length; i++) {
            for (int j = 0; j < DOUBLE_ORDER.length; j++) {
                assertThat(FunUtil.compareValues(DOUBLE_ORDER[i], DOUBLE_ORDER[j]))
                        .as("FunUtil.compareValues(%s, %s)", DOUBLE_ORDER_NAMES[i], DOUBLE_ORDER_NAMES[j])
                        .isEqualTo(Integer.compare(i, j));
            }
        }
    }

    @Test
    @DisplayName("Sorter.compareValues(double,double): full pairwise order -Inf < NULL < -1 < 0 < 1 < NaN < +Inf")
    void sorterDoubleOverloadPairwiseOrder() {
        for (int i = 0; i < DOUBLE_ORDER.length; i++) {
            for (int j = 0; j < DOUBLE_ORDER.length; j++) {
                assertThat(Sorter.compareValues(DOUBLE_ORDER[i], DOUBLE_ORDER[j]))
                        .as("Sorter.compareValues(%s, %s)", DOUBLE_ORDER_NAMES[i], DOUBLE_ORDER_NAMES[j])
                        .isEqualTo(Integer.compare(i, j));
            }
        }
    }

    @Test
    @DisplayName("FunUtil.compareValues(Object,Object): full pairwise order nullValue < -Inf < -1 < 0 < 1 < NaN < +Inf")
    void funUtilObjectOverloadPairwiseOrder() {
        // NOTE: in the Object overload, Util.nullValue sorts BELOW -Infinity —
        // the opposite of the double overload, where -Inf < NULL. This
        // discrepancy between the two overloads is frozen here and will be
        // resolved when the comparators are unified (/3), see
        // the null-semantics notes
        for (int i = 0; i < OBJECT_ORDER.length; i++) {
            for (int j = 0; j < OBJECT_ORDER.length; j++) {
                assertThat(FunUtil.compareValues(OBJECT_ORDER[i], OBJECT_ORDER[j]))
                        .as("FunUtil.compareValues(%s, %s)", OBJECT_ORDER_NAMES[i], OBJECT_ORDER_NAMES[j])
                        .isEqualTo(Integer.compare(i, j));
            }
        }
    }

    @Test
    @DisplayName("Sorter.compareValues(Object,Object): full pairwise order nullValue < -Inf < -1 < 0 < 1 < NaN < +Inf")
    void sorterObjectOverloadPairwiseOrder() {
        for (int i = 0; i < OBJECT_ORDER.length; i++) {
            for (int j = 0; j < OBJECT_ORDER.length; j++) {
                assertThat(Sorter.compareValues(OBJECT_ORDER[i], OBJECT_ORDER[j]))
                        .as("Sorter.compareValues(%s, %s)", OBJECT_ORDER_NAMES[i], OBJECT_ORDER_NAMES[j])
                        .isEqualTo(Integer.compare(i, j));
            }
        }
    }

    @Test
    @DisplayName("Object overloads: Java null sorts below everything, even below Util.nullValue")
    void javaNullSortsFirstInObjectOverloads() {
        assertThat(FunUtil.compareValues(null, Util.nullValue)).isEqualTo(-1);
        assertThat(FunUtil.compareValues(Util.nullValue, null)).isEqualTo(1);
        assertThat(FunUtil.compareValues((Object) null, null)).isEqualTo(0);
        assertThat(FunUtil.compareValues(null, Double.NEGATIVE_INFINITY)).isEqualTo(-1);

        assertThat(Sorter.compareValues(null, Util.nullValue)).isEqualTo(-1);
        assertThat(Sorter.compareValues(Util.nullValue, null)).isEqualTo(1);
        assertThat(Sorter.compareValues((Object) null, null)).isEqualTo(0);
        assertThat(Sorter.compareValues(null, Double.NEGATIVE_INFINITY)).isEqualTo(-1);
    }

    @Test
    @DisplayName("COLLISION: a genuine double equal to 0.000000012345 sorts as NULL in the double overloads")
    void computedSentinelValueSortsAsNull() {
        // The double overloads compare BY VALUE against the sentinel, so a
        // real measure value of exactly 0.000000012345 sorts as NULL: below
        // -1 although it is numerically greater. This is the value-collision
        // bug; these assertions flips once the sentinel encoding is gone; see
        // the null-semantics notes
        assertThat(FunUtil.compareValues(SENTINEL_VALUE, -1.0)).isEqualTo(-1);
        assertThat(FunUtil.compareValues(-1.0, SENTINEL_VALUE)).isEqualTo(1);
        assertThat(Sorter.compareValues(SENTINEL_VALUE, -1.0)).isEqualTo(-1);
        assertThat(Sorter.compareValues(-1.0, SENTINEL_VALUE)).isEqualTo(1);

        // Via the Object overloads a boxed computed sentinel value is not
        // reference-identical to Util.nullValue, but it falls through to the
        // Number branch which delegates to the value-based double overload —
        // so it STILL sorts as NULL.
        Double boxedComputed = Double.valueOf(SENTINEL_VALUE);
        assertThat(boxedComputed).isNotSameAs(Util.nullValue);
        assertThat(FunUtil.compareValues((Object) boxedComputed, (Object) Double.valueOf(-1.0))).isEqualTo(-1);
        assertThat(Sorter.compareValues((Object) boxedComputed, (Object) Double.valueOf(-1.0))).isEqualTo(-1);
    }
}
