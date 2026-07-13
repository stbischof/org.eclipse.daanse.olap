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
 * MDX NULL semantics of the type-coercion calcs (MDX NULL is Java
 * {@code null} throughout the calc layer):
 *
 * <ul>
 * <li>DoubleToBooleanCalc: Java null and NaN map to BOOLEAN_NULL, which is
 * the primitive false (no 3VL).</li>
 * <li>UnknownToDoubleCalc: Java null input produces Java null.</li>
 * <li>IntegerToDoubleCalc: Java null input produces Java null.</li>
 * </ul>
 */
class CoercionNullSemanticsTest {

    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = mock(Evaluator.class);
    }

    @Nested
    @DisplayName("DoubleToBooleanCalc: Java-null NULL check ")
    class DoubleToBooleanCharacterization {

        private DoubleCalc doubleCalc;
        private DoubleToBooleanCalc calc;

        @BeforeEach
        void setUpCalc() {
            doubleCalc = mock(DoubleCalc.class);
            calc = new DoubleToBooleanCalc(BooleanType.INSTANCE, doubleCalc);
        }

        @Test
        @DisplayName("Java null maps to BOOLEAN_NULL, which is false")
        void javaNullMapsToBooleanNull() {
            when(doubleCalc.evaluate(evaluator)).thenReturn(null);
            // FunUtil.BOOLEAN_NULL is the primitive false — no 3VL: a NULL
            // boolean is indistinguishable from a genuine FALSE.
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
    }

    @Nested
    @DisplayName("UnknownToDoubleCalc: NULL inputs map to Java null ")
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
        @DisplayName("Java null input produces Java null")
        void javaNullProducesJavaNull() {
            when(childCalc.evaluate(evaluator)).thenReturn(null);
            assertThat(calc.evaluate(evaluator)).isNull();
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
    @DisplayName("IntegerToDoubleCalc: Java null produces Java null ")
    class IntegerToDoubleCharacterization {

        private IntegerCalc integerCalc;
        private IntegerToDoubleCalc calc;

        @BeforeEach
        void setUpCalc() {
            integerCalc = mock(IntegerCalc.class);
            calc = new IntegerToDoubleCalc(NumericType.INSTANCE, integerCalc);
        }

        @Test
        @DisplayName("Java null input produces Java null")
        void javaNullProducesJavaNull() {
            when(integerCalc.evaluate(evaluator)).thenReturn(null);
            assertThat(calc.evaluate(evaluator)).isNull();
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
