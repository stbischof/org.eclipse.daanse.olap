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
 * regression tests for the ordering comparators.
 *
 * Since (. a primitive
 * {@code double} cannot carry MDX NULL anymore:
 * - double overloads implement the total order
 *   -Inf &lt; values &lt; NaN &lt; +Inf with NO NULL slot; the former sentinel
 *   value 0.000000012345 sorts as the plain tiny number it is (collision
 *   healing).
 * - NULL ordering exists only at the boxed/object level: the Object overloads
 *   sort Java {@code null} and the {@code Util.nullValue} singleton (cell
 *   layer, for external consumers) below EVERYTHING, including -Infinity — unchanged.
 */
class NullOrderingTest {

    private static final double SENTINEL_VALUE = Double.parseDouble("0.000000012345");

    /**
     * The total order of the double overloads: no NULL slot;
     * the former sentinel value sorts numerically (between 0 and 1).
 */
    private static final double[] DOUBLE_ORDER = {
            Double.NEGATIVE_INFINITY,
            -1.0,
            0.0,
            SENTINEL_VALUE, // former sentinel: a plain tiny number
            1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY };

    private static final String[] DOUBLE_ORDER_NAMES = {
            "-Inf", "-1", "0", "1.2345e-8(former sentinel)", "1", "NaN", "+Inf" };

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
    @DisplayName("FunUtil.compareValues(double,double): full pairwise order -Inf < -1 < 0 < 1.2345e-8 < 1 < NaN < +Inf")
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
    @DisplayName("Sorter.compareValues(double,double): full pairwise order -Inf < -1 < 0 < 1.2345e-8 < 1 < NaN < +Inf")
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
        // NOTE: NULL ordering exists ONLY at this object level since // Util.nullValue (the cell-layer sentinel) sorts below
        // everything, including -Infinity.
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
    @DisplayName("HEALED: a genuine double equal to 0.000000012345 sorts as a real value")
    void computedSentinelValueSortsAsRealValue() {
        // Before the double overloads compared BY VALUE against the
        // sentinel and sorted a real 0.000000012345 as NULL (below -1). Since
        // there is no primitive sentinel: it sorts numerically.
        assertThat(FunUtil.compareValues(SENTINEL_VALUE, -1.0)).isEqualTo(1);
        assertThat(FunUtil.compareValues(-1.0, SENTINEL_VALUE)).isEqualTo(-1);
        assertThat(Sorter.compareValues(SENTINEL_VALUE, -1.0)).isEqualTo(1);
        assertThat(Sorter.compareValues(-1.0, SENTINEL_VALUE)).isEqualTo(-1);

        // Via the Object overloads a boxed computed value falls through to
        // the Number branch and sorts numerically as well.
        Double boxedComputed = Double.valueOf(SENTINEL_VALUE);
        assertThat(boxedComputed).isNotSameAs(Util.nullValue);
        assertThat(FunUtil.compareValues((Object) boxedComputed, (Object) Double.valueOf(-1.0))).isEqualTo(1);
        assertThat(Sorter.compareValues((Object) boxedComputed, (Object) Double.valueOf(-1.0))).isEqualTo(1);
    }
}
