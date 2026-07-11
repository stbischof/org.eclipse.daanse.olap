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

import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.def.excel.acos.AcosCalc;
import org.eclipse.daanse.olap.function.def.excel.asin.AsinhCalc;
import org.eclipse.daanse.olap.function.def.excel.log10.Log10Calc;
import org.eclipse.daanse.olap.function.def.excel.sqrtpi.SqrtPiCalc;
import org.eclipse.daanse.olap.function.def.vba.exp.ExpCalc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * characterization tests for a sample of Excel/VBA function calcs and
 * their treatment of the {@code FunUtil.DOUBLE_NULL} sentinel.
 *
 * These tests freeze TODAY's behavior as a safety net for the NULL-semantics
 * refactoring (.
 *
 * The sample deliberately covers all check variants found in excel/* and vba/*:
 * - {@code number == FunUtil.DOUBLE_NULL} only (AcosCalc, Log10Calc,
 *   SqrtPiCalc): identity-based, NPE on Java null input.
 * - {@code x == FunUtil.DOUBLE_NULL || x == null} (AsinhCalc): identity-based,
 *   Java null handled.
 * - vba ExpCalc: since (idiom normalized to
 *   {@code NullSemantics.isSentinelOnly}, see the design notes
 *   AcosCalc — a runtime-computed 0.000000012345 no longer collides and is
 *   treated as an ordinary value.
 */
class ExcelVbaNullSemanticsTest {

    private static final Double SENTINEL = FunUtil.DOUBLE_NULL;
    private static final double SENTINEL_VALUE = Double.parseDouble("0.000000012345");

    private Evaluator evaluator;
    private DoubleCalc argCalc;

    // The calcs have protected constructors; minimal subclasses make them
    // instantiable from this dedicated test package.
    private static final class TestableAcosCalc extends AcosCalc {
        TestableAcosCalc(Type type, DoubleCalc calc) {
            super(type, calc);
        }
    }

    private static final class TestableAsinhCalc extends AsinhCalc {
        TestableAsinhCalc(Type type, DoubleCalc calc) {
            super(type, calc);
        }
    }

    private static final class TestableLog10Calc extends Log10Calc {
        TestableLog10Calc(Type type, DoubleCalc calc) {
            super(type, calc);
        }
    }

    private static final class TestableSqrtPiCalc extends SqrtPiCalc {
        TestableSqrtPiCalc(Type type, DoubleCalc calc) {
            super(type, calc);
        }
    }

    private static final class TestableExpCalc extends ExpCalc {
        TestableExpCalc(Type type, DoubleCalc calc) {
            super(type, calc);
        }
    }

    @BeforeEach
    void setUp() {
        evaluator = mock(Evaluator.class);
        argCalc = mock(DoubleCalc.class);
    }

    private static Double distinctSentinelValue() {
        return Double.valueOf(SENTINEL_VALUE);
    }

    private void stubArgument(Double value) {
        when(argCalc.evaluate(evaluator)).thenReturn(value);
    }

    @Nested
    @DisplayName("AcosCalc (excel): identity check, no Java-null guard")
    class AcosCalcCharacterization {

        @Test
        @DisplayName("real value is computed")
        void realValue() {
            stubArgument(1.0);
            assertThat(new TestableAcosCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator))
                    .isEqualTo(Math.acos(1.0));
        }

        @Test
        @DisplayName("sentinel singleton maps to Java null")
        void sentinelMapsToJavaNull() {
            stubArgument(SENTINEL);
            assertThat(new TestableAcosCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }

        @Test
        @DisplayName("computed 0.000000012345 passes the identity check and is computed as a value")
        void computedSentinelValueIsTreatedAsValue() {
            // Identity-based check misses the value-equal distinct instance.
            // Will be inverted in , see the null-semantics notes
            stubArgument(distinctSentinelValue());
            assertThat(new TestableAcosCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator))
                    .isEqualTo(Math.acos(SENTINEL_VALUE));
        }

        @Test
        @DisplayName("Java null input throws NullPointerException (unboxing in Math.acos)")
        void javaNullThrows() {
            stubArgument(null);
            assertThatThrownBy(() -> new TestableAcosCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("AsinhCalc (excel): identity check WITH Java-null guard")
    class AsinhCalcCharacterization {

        @Test
        @DisplayName("real value is computed")
        void realValue() {
            stubArgument(0.0);
            assertThat(new TestableAsinhCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("sentinel singleton maps to Java null")
        void sentinelMapsToJavaNull() {
            stubArgument(SENTINEL);
            assertThat(new TestableAsinhCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }

        @Test
        @DisplayName("Java null input maps to Java null (explicit '|| x == null' variant)")
        void javaNullMapsToJavaNull() {
            stubArgument(null);
            assertThat(new TestableAsinhCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }
    }

    @Nested
    @DisplayName("Log10Calc (excel): identity check, no Java-null guard")
    class Log10CalcCharacterization {

        @Test
        @DisplayName("real value is computed")
        void realValue() {
            stubArgument(100.0);
            assertThat(new TestableLog10Calc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isEqualTo(2.0);
        }

        @Test
        @DisplayName("sentinel singleton maps to Java null")
        void sentinelMapsToJavaNull() {
            stubArgument(SENTINEL);
            assertThat(new TestableLog10Calc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }

        @Test
        @DisplayName("Java null input throws NullPointerException")
        void javaNullThrows() {
            stubArgument(null);
            assertThatThrownBy(() -> new TestableLog10Calc(NumericType.INSTANCE, argCalc).evaluate(evaluator))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("SqrtPiCalc (excel): identity check, no Java-null guard")
    class SqrtPiCalcCharacterization {

        @Test
        @DisplayName("real value is computed")
        void realValue() {
            stubArgument(4.0);
            assertThat(new TestableSqrtPiCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator))
                    .isEqualTo(Math.sqrt(4.0 * Math.PI));
        }

        @Test
        @DisplayName("sentinel singleton maps to Java null")
        void sentinelMapsToJavaNull() {
            stubArgument(SENTINEL);
            assertThat(new TestableSqrtPiCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }

        @Test
        @DisplayName("Java null input throws NullPointerException")
        void javaNullThrows() {
            stubArgument(null);
            assertThatThrownBy(() -> new TestableSqrtPiCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("ExpCalc (vba): identity-based sentinel check (since )")
    class ExpCalcCharacterization {

        @Test
        @DisplayName("real value is computed")
        void realValue() {
            stubArgument(1.0);
            assertThat(new TestableExpCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isEqualTo(Math.E);
        }

        @Test
        @DisplayName("sentinel singleton maps to Java null")
        void sentinelMapsToJavaNull() {
            stubArgument(SENTINEL);
            assertThat(new TestableExpCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }

        @Test
        @DisplayName("computed 0.000000012345 passes the identity check and is computed as a value")
        void computedSentinelValueCollides() {
            // Historically ExpCalc used Util.DOUBLE_NULL.equals(number) and
            // swallowed a genuine 0.000000012345 as NULL (value collision).
            // The idiom was normalized to the identity-based
            // NullSemantics.isSentinelOnly in (sanctioned hardening,
            //.
            // 06-migrationsplan-phasen.md), so a value-equal distinct Double
            // is now an ordinary value, in line with AcosCalc/DivideCalc.
            stubArgument(distinctSentinelValue());
            assertThat(new TestableExpCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator))
                    .isEqualTo(Math.exp(SENTINEL_VALUE));
        }

        @Test
        @DisplayName("Java null input throws NullPointerException (sentinel check is false, then Math.exp unboxes)")
        void javaNullThrows() {
            stubArgument(null);
            assertThatThrownBy(() -> new TestableExpCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
