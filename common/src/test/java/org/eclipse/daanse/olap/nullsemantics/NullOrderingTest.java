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

import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.fun.sort.Sorter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ordering semantics of the value comparators:
 *
 * <ul>
 * <li>the double overloads implement the MSAS-compatible total order
 * {@code -Inf < values < NaN < +Inf}; a primitive {@code double} cannot carry
 * MDX NULL, so this order has no NULL slot.</li>
 * <li>NULL ordering exists only at the boxed/object level: the Object
 * overloads sort Java {@code null} (MDX NULL) below EVERYTHING, including
 * -Infinity.</li>
 * </ul>
 */
class NullOrderingTest {

    /**
     * The total order of the double overloads: no NULL slot.
 */
    private static final double[] DOUBLE_ORDER = {
            Double.NEGATIVE_INFINITY,
            -1.0,
            0.0,
            1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY };

    private static final String[] DOUBLE_ORDER_NAMES = {
            "-Inf", "-1", "0", "1", "NaN", "+Inf" };

    /**
     * The total order of the Object overloads: Java {@code null} sorts below
     * everything, EVEN below -Infinity.
 */
    private static final Object[] OBJECT_ORDER = {
            null,
            Double.NEGATIVE_INFINITY,
            -1.0,
            0.0,
            1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY };

    private static final String[] OBJECT_ORDER_NAMES = {
            "null", "-Inf", "-1", "0", "1", "NaN", "+Inf" };

    @Test
    @DisplayName("FunUtil.compareValues(double,double): full pairwise order -Inf < -1 < 0 < 1 < NaN < +Inf")
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
    @DisplayName("Sorter.compareValues(double,double): full pairwise order -Inf < -1 < 0 < 1 < NaN < +Inf")
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
    @DisplayName("FunUtil.compareValues(Object,Object): full pairwise order null < -Inf < -1 < 0 < 1 < NaN < +Inf")
    void funUtilObjectOverloadPairwiseOrder() {
        // NULL ordering exists ONLY at this object level: Java null (MDX
        // NULL) sorts below everything, including -Infinity.
        for (int i = 0; i < OBJECT_ORDER.length; i++) {
            for (int j = 0; j < OBJECT_ORDER.length; j++) {
                assertThat(FunUtil.compareValues(OBJECT_ORDER[i], OBJECT_ORDER[j]))
                        .as("FunUtil.compareValues(%s, %s)", OBJECT_ORDER_NAMES[i], OBJECT_ORDER_NAMES[j])
                        .isEqualTo(Integer.compare(i, j));
            }
        }
    }

    @Test
    @DisplayName("Sorter.compareValues(Object,Object): full pairwise order null < -Inf < -1 < 0 < 1 < NaN < +Inf")
    void sorterObjectOverloadPairwiseOrder() {
        for (int i = 0; i < OBJECT_ORDER.length; i++) {
            for (int j = 0; j < OBJECT_ORDER.length; j++) {
                assertThat(Sorter.compareValues(OBJECT_ORDER[i], OBJECT_ORDER[j]))
                        .as("Sorter.compareValues(%s, %s)", OBJECT_ORDER_NAMES[i], OBJECT_ORDER_NAMES[j])
                        .isEqualTo(Integer.compare(i, j));
            }
        }
    }
}
