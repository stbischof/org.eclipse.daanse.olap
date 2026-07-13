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
 * MDX NULL semantics of the arithmetic operator calcs (MDX NULL is Java
 * {@code null} in the calc layer). Pins the MSAS behavior matrix:
 * {@code null + x = x}, {@code null - x = -x}, {@code null * x = null},
 * {@code null / x = null}, {@code x / null = +Infinity} (unless the divide
 * calc is configured to produce NULL for a NULL denominator).
 */
class OperatorNullSemanticsTest {

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

    private Double evaluateBinary(Double v0, Double v1,
            java.util.function.BiFunction<DoubleCalc, DoubleCalc, DoubleCalc> factory) {
        when(calc0.evaluate(evaluator)).thenReturn(v0);
        when(calc1.evaluate(evaluator)).thenReturn(v1);
        return factory.apply(calc0, calc1).evaluate(evaluator);
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
                Arguments.of("both Java null -> null", null, null, null));
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
                Arguments.of("both Java null -> null", null, null, null));
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
                Arguments.of("Java null -> null", null, null));
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
                Arguments.of("Java null right -> null (x * null = null)", 3.0, null, null));
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
                Arguments.of("Java null over Java null -> null (numerator wins)", null, null, null));
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
                Arguments.of("Java null denominator -> null", 6.0, null, null));
    }
}
