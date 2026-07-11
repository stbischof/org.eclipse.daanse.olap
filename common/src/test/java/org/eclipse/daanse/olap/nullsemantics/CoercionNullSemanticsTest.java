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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.calc.base.type.booleanx.DoubleToBooleanCalc;
import org.eclipse.daanse.olap.calc.base.type.doublex.IntegerToDoubleCalc;
import org.eclipse.daanse.olap.calc.base.type.doublex.UnknownToDoubleCalc;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * characterization tests for the type-coercion calcs and their
 * treatment of the {@code FunUtil.DOUBLE_NULL} sentinel.
 *
 * These tests freeze TODAY's behavior as a safety net for the NULL-semantics
 * refactoring (.
 *
 * Frozen characteristics:
 * - DoubleToBooleanCalc: sentinel (identity) and NaN map to BOOLEAN_NULL,
 *   which is the primitive false; Java null input throws NPE.
 * - UnknownToDoubleCalc: uses Objects.equals — VALUE-based, so any incoming
 *   0.000000012345 collides and becomes NULL.
 * - IntegerToDoubleCalc: Java null input PRODUCES the DOUBLE_NULL singleton.
 */
class CoercionNullSemanticsTest {

    private static final double SENTINEL_VALUE = Double.parseDouble("0.000000012345");

    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = mock(Evaluator.class);
    }

    private static Double distinctSentinelValue() {
        return Double.valueOf(SENTINEL_VALUE);
    }

    @Nested
    @DisplayName("DoubleToBooleanCalc: identity sentinel check")
    class DoubleToBooleanCharacterization {

        private DoubleCalc doubleCalc;
        private DoubleToBooleanCalc calc;

        @BeforeEach
        void setUpCalc() {
            doubleCalc = mock(DoubleCalc.class);
            calc = new DoubleToBooleanCalc(BooleanType.INSTANCE, doubleCalc);
        }

        @Test
        @DisplayName("sentinel singleton maps to BOOLEAN_NULL, which is false")
        void sentinelMapsToBooleanNull() {
            when(doubleCalc.evaluate(evaluator)).thenReturn(FunUtil.DOUBLE_NULL);
            // FunUtil.BOOLEAN_NULL is the primitive false — a NULL boolean is
            // indistinguishable from a genuine FALSE today.
            assertThat(calc.evaluate(evaluator)).isEqualTo(FunUtil.BOOLEAN_NULL).isFalse();
        }

        @Test
        @DisplayName("NaN maps to BOOLEAN_NULL, which is false")
        void nanMapsToBooleanNull() {
            when(doubleCalc.evaluate(evaluator)).thenReturn(Double.NaN);
            assertThat(calc.evaluate(evaluator)).isFalse();
        }

        @Test
        @DisplayName("zero maps to false, non-zero maps to true")
        void realValues() {
            when(doubleCalc.evaluate(evaluator)).thenReturn(0.0);
            assertThat(calc.evaluate(evaluator)).isFalse();

            when(doubleCalc.evaluate(evaluator)).thenReturn(2.0);
            assertThat(calc.evaluate(evaluator)).isTrue();

            when(doubleCalc.evaluate(evaluator)).thenReturn(-2.0);
            assertThat(calc.evaluate(evaluator)).isTrue();
        }

        @Test
        @DisplayName("computed 0.000000012345 passes the identity check and maps to true (non-zero)")
        void computedSentinelValueIsTreatedAsValue() {
            // The '== FunUtil.DOUBLE_NULL' check is reference-based; a
            // value-equal distinct instance is treated as an ordinary non-zero
            // number. flips once the sentinel encoding is gone.
            when(doubleCalc.evaluate(evaluator)).thenReturn(distinctSentinelValue());
            assertThat(calc.evaluate(evaluator)).isTrue();
        }

        @Test
        @DisplayName("Java null input throws NullPointerException (Double.isNaN unboxes first)")
        void javaNullThrows() {
            when(doubleCalc.evaluate(evaluator)).thenReturn(null);
            assertThatThrownBy(() -> calc.evaluate(evaluator)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("UnknownToDoubleCalc: VALUE-based sentinel check (Objects.equals)")
    class UnknownToDoubleCharacterization {

        private Calc<Object> childCalc;
        private UnknownToDoubleCalc calc;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void setUpCalc() {
            childCalc = mock(Calc.class);
            calc = new UnknownToDoubleCalc(NumericType.INSTANCE, childCalc);
        }

        @Test
        @DisplayName("Java null input produces the DOUBLE_NULL singleton")
        void javaNullProducesSentinelSingleton() {
            when(childCalc.evaluate(evaluator)).thenReturn(null);
            assertThat(calc.evaluate(evaluator)).isSameAs(FunUtil.DOUBLE_NULL);
        }

        @Test
        @DisplayName("sentinel singleton input is passed through as the singleton")
        void sentinelSingletonIsPassedThrough() {
            when(childCalc.evaluate(evaluator)).thenReturn(FunUtil.DOUBLE_NULL);
            assertThat(calc.evaluate(evaluator)).isSameAs(FunUtil.DOUBLE_NULL);
        }

        @Test
        @DisplayName("COLLISION: any incoming 0.000000012345 is normalized to the DOUBLE_NULL singleton")
        void computedSentinelValueCollides() {
            // Objects.equals(o, FunUtil.DOUBLE_NULL) compares BY VALUE: a
            // genuine 0.000000012345 from ANY source is swallowed as NULL
            // here — unlike the identity-checking operator calcs. This is the
            // value-collision bug; the assertion flips once the sentinel encoding is gone.
            Double genuineTinyValue = distinctSentinelValue();
            assertThat(genuineTinyValue).isNotSameAs(FunUtil.DOUBLE_NULL);
            when(childCalc.evaluate(evaluator)).thenReturn(genuineTinyValue);

            assertThat(calc.evaluate(evaluator)).isSameAs(FunUtil.DOUBLE_NULL);
        }

        @Test
        @DisplayName("Double and other Number inputs are converted to Double")
        void numberInputsAreConverted() {
            when(childCalc.evaluate(evaluator)).thenReturn(2.5);
            assertThat(calc.evaluate(evaluator)).isEqualTo(2.5);

            when(childCalc.evaluate(evaluator)).thenReturn(Integer.valueOf(3));
            assertThat(calc.evaluate(evaluator)).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("IntegerToDoubleCalc: Java null produces the sentinel")
    class IntegerToDoubleCharacterization {

        private IntegerCalc integerCalc;
        private IntegerToDoubleCalc calc;

        @BeforeEach
        void setUpCalc() {
            integerCalc = mock(IntegerCalc.class);
            calc = new IntegerToDoubleCalc(NumericType.INSTANCE, integerCalc);
        }

        @Test
        @DisplayName("Java null input produces the DOUBLE_NULL singleton")
        void javaNullProducesSentinelSingleton() {
            when(integerCalc.evaluate(evaluator)).thenReturn(null);
            assertThat(calc.evaluate(evaluator)).isSameAs(FunUtil.DOUBLE_NULL);
        }

        @Test
        @DisplayName("integer input is widened to Double")
        void integerIsWidened() {
            when(integerCalc.evaluate(evaluator)).thenReturn(3);
            assertThat(calc.evaluate(evaluator)).isEqualTo(3.0);

            when(integerCalc.evaluate(evaluator)).thenReturn(0);
            assertThat(calc.evaluate(evaluator)).isEqualTo(0.0);
        }
    }
}
