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

import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.def.operators.divide.DivideCalc;
import org.eclipse.daanse.olap.function.def.operators.minus.MinusCalc;
import org.eclipse.daanse.olap.function.def.operators.minus.MinusPrefixCalc;
import org.eclipse.daanse.olap.function.def.operators.multiply.MultiplyCalc;
import org.eclipse.daanse.olap.function.def.operators.plus.PlusCalc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * regression tests for the arithmetic operator calcs and the MDX NULL
 * semantics of the calc layer.
 *
 * Since (. MDX NULL is
 * represented as Java {@code null}; the former {@code FunUtil.DOUBLE_NULL}
 * sentinel ({@code Double.valueOf(0.000000012345)}) is an ordinary value
 * EVERYWHERE. These tests keep the MSAS behavior matrix (null + x = x,
 * null * x = null, ...) and act as regression tests of the collision healing
 * .
 */
class OperatorNullSemanticsTest {

    /**
     * The former sentinel singleton — now an ORDINARY value, kept in
     * the matrices as a regression test of the healing.
 */
    @SuppressWarnings("deprecation")
    private static final Double SENTINEL = FunUtil.DOUBLE_NULL;

    /** The former sentinel's primitive value. */
    private static final double SENTINEL_VALUE = Double.parseDouble("0.000000012345");

    private Evaluator evaluator;
    private DoubleCalc calc0;
    private DoubleCalc calc1;

    // The operator calcs have protected constructors; minimal subclasses make
    // them instantiable from this dedicated test package.
    private static final class TestablePlusCalc extends PlusCalc {
        TestablePlusCalc(Type type, DoubleCalc c0, DoubleCalc c1) {
            super(type, c0, c1);
        }
    }

    private static final class TestableMinusCalc extends MinusCalc {
        TestableMinusCalc(Type type, DoubleCalc c0, DoubleCalc c1) {
            super(type, c0, c1);
        }
    }

    private static final class TestableMinusPrefixCalc extends MinusPrefixCalc {
        TestableMinusPrefixCalc(Type type, DoubleCalc c0) {
            super(type, c0);
        }
    }

    private static final class TestableMultiplyCalc extends MultiplyCalc {
        TestableMultiplyCalc(Type type, DoubleCalc c0, DoubleCalc c1) {
            super(type, c0, c1);
        }
    }

    private static final class TestableDivideCalc extends DivideCalc {
        TestableDivideCalc(Type type, DoubleCalc c0, DoubleCalc c1, boolean nullDenominatorProducesNull) {
            super(type, c0, c1, nullDenominatorProducesNull);
        }
    }

    @BeforeEach
    void setUp() {
        evaluator = mock(Evaluator.class);
        calc0 = mock(DoubleCalc.class);
        calc1 = mock(DoubleCalc.class);
    }

    /** Boxes the sentinel value into a fresh instance distinct from the singleton. */
    private static Double distinctSentinelValue() {
        return Double.valueOf(SENTINEL_VALUE);
    }

    private Double evaluateBinary(Double v0, Double v1,
            java.util.function.BiFunction<DoubleCalc, DoubleCalc, DoubleCalc> factory) {
        when(calc0.evaluate(evaluator)).thenReturn(v0);
        when(calc1.evaluate(evaluator)).thenReturn(v1);
        return factory.apply(calc0, calc1).evaluate(evaluator);
    }

    @Test
    @DisplayName("Guard: runtime-computed 0.000000012345 is value-equal but not identical to the former sentinel")
    void computedSentinelValueIsDistinctInstance() {
        Double computed = distinctSentinelValue();
        assertThat(computed).isEqualTo(SENTINEL);
        assertThat(computed).isNotSameAs(SENTINEL);
    }

    // --- PlusCalc -------------------------------------------------------

    @ParameterizedTest(name = "{0}: {1} + {2} = {3}")
    @MethodSource("plusArguments")
    @DisplayName("PlusCalc treats Java-null operands as neutral element")
    void plusCharacterization(String testName, Double v0, Double v1, Double expected) {
        Double result = evaluateBinary(v0, v1, (c0, c1) -> new TestablePlusCalc(NumericType.INSTANCE, c0, c1));
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> plusArguments() {
        return Stream.of(
                Arguments.of("both real values", 2.0, 3.0, 5.0),
                Arguments.of("Java null left is ignored (null + 3 = 3)", null, 3.0, 3.0),
                Arguments.of("Java null right is ignored (2 + null = 2)", 2.0, null, 2.0),
                Arguments.of("both Java null -> null", null, null, null),
                // Regression of the healed sentinel collision: the former sentinel — whether the old singleton
                // or a computed instance — is an ordinary tiny number.
                Arguments.of("former sentinel singleton is added as a real value",
                        SENTINEL, 3.0, SENTINEL_VALUE + 3.0),
                Arguments.of("computed 0.000000012345 is treated as a real value",
                        distinctSentinelValue(), 3.0, SENTINEL_VALUE + 3.0));
    }

    @Test
    @DisplayName("PlusCalc returns Java null for null + null")
    void plusReturnsNullForBothNull() {
        Double result = evaluateBinary(null, null, (c0, c1) -> new TestablePlusCalc(NumericType.INSTANCE, c0, c1));
        assertThat(result).isNull();
    }

    // --- MinusCalc ------------------------------------------------------

    @ParameterizedTest(name = "{0}: {1} - {2} = {3}")
    @MethodSource("minusArguments")
    @DisplayName("MinusCalc treats Java-null operands as neutral element")
    void minusCharacterization(String testName, Double v0, Double v1, Double expected) {
        Double result = evaluateBinary(v0, v1, (c0, c1) -> new TestableMinusCalc(NumericType.INSTANCE, c0, c1));
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> minusArguments() {
        return Stream.of(
                Arguments.of("both real values", 5.0, 3.0, 2.0),
                Arguments.of("Java null left negates the right operand (null - 3 = -3)", null, 3.0, -3.0),
                Arguments.of("Java null right is ignored (5 - null = 5)", 5.0, null, 5.0),
                Arguments.of("both Java null -> null", null, null, null),
                // Collision healing : former
                // sentinel is an ordinary value in both positions.
                Arguments.of("former sentinel singleton is subtracted as a real value",
                        5.0, SENTINEL, 5.0 - SENTINEL_VALUE),
                Arguments.of("computed 0.000000012345 is treated as a real value",
                        5.0, distinctSentinelValue(), 5.0 - SENTINEL_VALUE));
    }

    // --- MinusPrefixCalc --------------------------------------------------

    @ParameterizedTest(name = "{0}: -({1}) = {2}")
    @MethodSource("minusPrefixArguments")
    @DisplayName("MinusPrefixCalc maps Java null to Java null")
    void minusPrefixCharacterization(String testName, Double v, Double expected) {
        when(calc0.evaluate(evaluator)).thenReturn(v);
        Double result = new TestableMinusPrefixCalc(NumericType.INSTANCE, calc0).evaluate(evaluator);
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> minusPrefixArguments() {
        return Stream.of(
                Arguments.of("real value", 3.0, -3.0),
                Arguments.of("Java null -> null", null, null),
                // The former sentinel value is an ordinary number.
                Arguments.of("former sentinel singleton is negated as a real value",
                        SENTINEL, -SENTINEL_VALUE),
                Arguments.of("computed 0.000000012345 is negated as a real value",
                        distinctSentinelValue(), -SENTINEL_VALUE));
    }

    // --- MultiplyCalc -----------------------------------------------------

    @ParameterizedTest(name = "{0}: {1} * {2} = {3}")
    @MethodSource("multiplyArguments")
    @DisplayName("MultiplyCalc returns Java null if either operand is null")
    void multiplyCharacterization(String testName, Double v0, Double v1, Double expected) {
        Double result = evaluateBinary(v0, v1, (c0, c1) -> new TestableMultiplyCalc(NumericType.INSTANCE, c0, c1));
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> multiplyArguments() {
        return Stream.of(
                Arguments.of("both real values", 2.0, 3.0, 6.0),
                Arguments.of("Java null left -> null (null * x = null)", null, 3.0, null),
                Arguments.of("Java null right -> null (x * null = null)", 3.0, null, null),
                // The former sentinel value is an ordinary number.
                Arguments.of("former sentinel singleton is multiplied as a real value",
                        SENTINEL, 3.0, SENTINEL_VALUE * 3.0),
                Arguments.of("computed 0.000000012345 is multiplied as a real value",
                        distinctSentinelValue(), 3.0, SENTINEL_VALUE * 3.0));
    }

    // --- DivideCalc, default flag (nullDenominatorProducesNull = false) ----

    @ParameterizedTest(name = "{0}: {1} / {2} = {3}")
    @MethodSource("divideDefaultArguments")
    @DisplayName("DivideCalc (default flag): Java-null denominator produces +Infinity")
    void divideDefaultCharacterization(String testName, Double v0, Double v1, Double expected) {
        Double result = evaluateBinary(v0, v1,
                (c0, c1) -> new TestableDivideCalc(NumericType.INSTANCE, c0, c1, false));
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> divideDefaultArguments() {
        return Stream.of(
                Arguments.of("both real values", 6.0, 3.0, 2.0),
                Arguments.of("Java null numerator -> null (null / x = null)", null, 3.0, null),
                Arguments.of("Java null denominator -> +Infinity (x / null = +Inf)",
                        6.0, null, Double.POSITIVE_INFINITY),
                Arguments.of("Java null over Java null -> null (numerator wins)", null, null, null),
                // Collision healing : the former
                // sentinel divides as an ordinary tiny number in BOTH positions.
                Arguments.of("former sentinel numerator is divided as a real value",
                        SENTINEL, 2.0, SENTINEL_VALUE / 2.0),
                Arguments.of("former sentinel denominator is divided as a real value",
                        6.0, SENTINEL, 6.0 / SENTINEL_VALUE),
                Arguments.of("computed 0.000000012345 numerator is divided as a real value",
                        distinctSentinelValue(), 2.0, SENTINEL_VALUE / 2.0),
                Arguments.of("computed 0.000000012345 denominator is divided as a real value",
                        6.0, distinctSentinelValue(), 6.0 / SENTINEL_VALUE));
    }

    // --- DivideCalc, nullDenominatorProducesNull = true ---------------------

    @ParameterizedTest(name = "{0}: {1} / {2} = {3}")
    @MethodSource("divideNullFlagArguments")
    @DisplayName("DivideCalc (nullDenominatorProducesNull=true): Java null anywhere produces null")
    void divideNullFlagCharacterization(String testName, Double v0, Double v1, Double expected) {
        Double result = evaluateBinary(v0, v1,
                (c0, c1) -> new TestableDivideCalc(NumericType.INSTANCE, c0, c1, true));
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> divideNullFlagArguments() {
        return Stream.of(
                Arguments.of("both real values", 6.0, 3.0, 2.0),
                Arguments.of("Java null numerator -> null", null, 3.0, null),
                Arguments.of("Java null denominator -> null", 6.0, null, null),
                // The former sentinel value is an ordinary number.
                Arguments.of("former sentinel denominator is divided as a real value",
                        6.0, SENTINEL, 6.0 / SENTINEL_VALUE),
                Arguments.of("computed 0.000000012345 denominator is divided as a real value",
                        6.0, distinctSentinelValue(), 6.0 / SENTINEL_VALUE));
    }
}
