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

import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.Type;
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
 * MDX NULL semantics of a sample of Excel/VBA function calcs (MDX NULL is
 * Java {@code null} in the calc layer): every sampled calc maps a Java-null
 * argument to a Java-null result instead of throwing, and computes real
 * values normally.
 */
class ExcelVbaNullSemanticsTest {

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

    private void stubArgument(Double value) {
        when(argCalc.evaluate(evaluator)).thenReturn(value);
    }

    @Nested
    @DisplayName("AcosCalc (excel): Java null maps to null")
    class AcosCalcCharacterization {

        @Test
        @DisplayName("real value is computed")
        void realValue() {
            stubArgument(1.0);
            assertThat(new TestableAcosCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator))
                    .isEqualTo(Math.acos(1.0));
        }

        @Test
        @DisplayName("Java null input maps to Java null instead of throwing")
        void javaNullMapsToJavaNull() {
            stubArgument(null);
            assertThat(new TestableAcosCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }
    }

    @Nested
    @DisplayName("AsinhCalc (excel): Java null maps to null")
    class AsinhCalcCharacterization {

        @Test
        @DisplayName("real value is computed")
        void realValue() {
            stubArgument(0.0);
            assertThat(new TestableAsinhCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Java null input maps to Java null")
        void javaNullMapsToJavaNull() {
            stubArgument(null);
            assertThat(new TestableAsinhCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }
    }

    @Nested
    @DisplayName("Log10Calc (excel): Java null maps to null")
    class Log10CalcCharacterization {

        @Test
        @DisplayName("real value is computed")
        void realValue() {
            stubArgument(100.0);
            assertThat(new TestableLog10Calc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Java null input maps to Java null instead of throwing")
        void javaNullMapsToJavaNull() {
            stubArgument(null);
            assertThat(new TestableLog10Calc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }
    }

    @Nested
    @DisplayName("SqrtPiCalc (excel): Java null maps to null")
    class SqrtPiCalcCharacterization {

        @Test
        @DisplayName("real value is computed")
        void realValue() {
            stubArgument(4.0);
            assertThat(new TestableSqrtPiCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator))
                    .isEqualTo(Math.sqrt(4.0 * Math.PI));
        }

        @Test
        @DisplayName("Java null input maps to Java null instead of throwing")
        void javaNullMapsToJavaNull() {
            stubArgument(null);
            assertThat(new TestableSqrtPiCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }
    }

    @Nested
    @DisplayName("ExpCalc (vba): Java null maps to null")
    class ExpCalcCharacterization {

        @Test
        @DisplayName("real value is computed")
        void realValue() {
            stubArgument(1.0);
            assertThat(new TestableExpCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isEqualTo(Math.E);
        }

        @Test
        @DisplayName("Java null input maps to Java null instead of throwing")
        void javaNullMapsToJavaNull() {
            stubArgument(null);
            assertThat(new TestableExpCalc(NumericType.INSTANCE, argCalc).evaluate(evaluator)).isNull();
        }
    }
}
