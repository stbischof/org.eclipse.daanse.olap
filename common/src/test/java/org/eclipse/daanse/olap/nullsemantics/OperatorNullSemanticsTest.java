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
 * characterization tests for the arithmetic operator calcs and their
 * treatment of the {@code FunUtil.DOUBLE_NULL} sentinel
 * ({@code Double.valueOf(0.000000012345)}).
 *
 * These tests freeze TODAY's behavior as a safety net for the NULL-semantics
 * refactoring (. They assert
 * what the code actually does, including known collision bugs.
 *
 * Key characteristic: the operator calcs compare boxed Doubles with
 * {@code v0 == FunUtil.DOUBLE_NULL}, which is a REFERENCE comparison. Only the
 * sentinel singleton instance is recognized as NULL; a value-equal but
 * distinct {@code Double} instance is treated as an ordinary number.
 */
class OperatorNullSemanticsTest {

    /** The NULL sentinel singleton. */
    private static final Double SENTINEL = FunUtil.DOUBLE_NULL;

    /**
     * The sentinel's primitive value, computed at runtime so that boxing it
     * yields a Double instance distinct from the {@code DOUBLE_NULL} singleton.
 */
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
    @DisplayName("Guard: runtime-computed 0.000000012345 is value-equal but not identical to DOUBLE_NULL")
    void computedSentinelValueIsDistinctInstance() {
        Double computed = distinctSentinelValue();
        assertThat(computed).isEqualTo(SENTINEL);
        assertThat(computed).isNotSameAs(SENTINEL);
    }

    // --- PlusCalc -------------------------------------------------------

    @ParameterizedTest(name = "{0}: {1} + {2} = {3}")
    @MethodSource("plusArguments")
    @DisplayName("PlusCalc treats sentinel/null operands as neutral element")
    void plusCharacterization(String testName, Double v0, Double v1, Double expected) {
        Double result = evaluateBinary(v0, v1, (c0, c1) -> new TestablePlusCalc(NumericType.INSTANCE, c0, c1));
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> plusArguments() {
        return Stream.of(
                Arguments.of("both real values", 2.0, 3.0, 5.0),
                Arguments.of("sentinel left is ignored (null + 3 = 3)", SENTINEL, 3.0, 3.0),
                Arguments.of("sentinel right is ignored (2 + null = 2)", 2.0, SENTINEL, 2.0),
                Arguments.of("both sentinel -> sentinel", SENTINEL, SENTINEL, SENTINEL),
                Arguments.of("Java null left is ignored", null, 3.0, 3.0),
                Arguments.of("Java null right is ignored", 2.0, null, 2.0),
                Arguments.of("both Java null -> sentinel", null, null, SENTINEL),
                // Reference check misses a value-equal distinct instance: it is
                // treated as an ordinary tiny number. Will be inverted in
                //
                Arguments.of("computed 0.000000012345 is treated as a real value",
                        distinctSentinelValue(), 3.0, SENTINEL_VALUE + 3.0));
    }

    @Test
    @DisplayName("PlusCalc returns the DOUBLE_NULL singleton itself for null + null")
    void plusReturnsSentinelSingleton() {
        Double result = evaluateBinary(null, null, (c0, c1) -> new TestablePlusCalc(NumericType.INSTANCE, c0, c1));
        assertThat(result).isSameAs(FunUtil.DOUBLE_NULL);
    }

    // --- MinusCalc ------------------------------------------------------

    @ParameterizedTest(name = "{0}: {1} - {2} = {3}")
    @MethodSource("minusArguments")
    @DisplayName("MinusCalc treats sentinel/null operands as neutral element")
    void minusCharacterization(String testName, Double v0, Double v1, Double expected) {
        Double result = evaluateBinary(v0, v1, (c0, c1) -> new TestableMinusCalc(NumericType.INSTANCE, c0, c1));
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> minusArguments() {
        return Stream.of(
                Arguments.of("both real values", 5.0, 3.0, 2.0),
                Arguments.of("sentinel left negates the right operand (null - 3 = -3)", SENTINEL, 3.0, -3.0),
                Arguments.of("sentinel right is ignored (5 - null = 5)", 5.0, SENTINEL, 5.0),
                Arguments.of("both sentinel -> sentinel", SENTINEL, SENTINEL, SENTINEL),
                Arguments.of("Java null left negates the right operand", null, 3.0, -3.0),
                Arguments.of("Java null right is ignored", 5.0, null, 5.0),
                // Identity check misses the distinct instance; see the note above.
                Arguments.of("computed 0.000000012345 is treated as a real value",
                        5.0, distinctSentinelValue(), 5.0 - SENTINEL_VALUE));
    }

    // --- MinusPrefixCalc --------------------------------------------------

    @ParameterizedTest(name = "{0}: -({1}) = {2}")
    @MethodSource("minusPrefixArguments")
    @DisplayName("MinusPrefixCalc maps sentinel/null to sentinel")
    void minusPrefixCharacterization(String testName, Double v, Double expected) {
        when(calc0.evaluate(evaluator)).thenReturn(v);
        Double result = new TestableMinusPrefixCalc(NumericType.INSTANCE, calc0).evaluate(evaluator);
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> minusPrefixArguments() {
        return Stream.of(
                Arguments.of("real value", 3.0, -3.0),
                Arguments.of("sentinel -> sentinel", SENTINEL, SENTINEL),
                Arguments.of("Java null -> sentinel", null, SENTINEL),
                // Identity check misses the distinct instance; negated as an ordinary value.
                Arguments.of("computed 0.000000012345 is negated as a real value",
                        distinctSentinelValue(), -SENTINEL_VALUE));
    }

    // --- MultiplyCalc -----------------------------------------------------

    @ParameterizedTest(name = "{0}: {1} * {2} = {3}")
    @MethodSource("multiplyArguments")
    @DisplayName("MultiplyCalc returns sentinel if either operand is sentinel/null")
    void multiplyCharacterization(String testName, Double v0, Double v1, Double expected) {
        Double result = evaluateBinary(v0, v1, (c0, c1) -> new TestableMultiplyCalc(NumericType.INSTANCE, c0, c1));
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> multiplyArguments() {
        return Stream.of(
                Arguments.of("both real values", 2.0, 3.0, 6.0),
                Arguments.of("sentinel left -> sentinel (null * x = null)", SENTINEL, 3.0, SENTINEL),
                Arguments.of("sentinel right -> sentinel (x * null = null)", 3.0, SENTINEL, SENTINEL),
                Arguments.of("Java null left -> sentinel", null, 3.0, SENTINEL),
                Arguments.of("Java null right -> sentinel", 3.0, null, SENTINEL),
                // Identity check misses the distinct instance; multiplied as an ordinary value.
                Arguments.of("computed 0.000000012345 is multiplied as a real value",
                        distinctSentinelValue(), 3.0, SENTINEL_VALUE * 3.0));
    }

    // --- DivideCalc, default flag (nullDenominatorProducesNull = false) ----

    @ParameterizedTest(name = "{0}: {1} / {2} = {3}")
    @MethodSource("divideDefaultArguments")
    @DisplayName("DivideCalc (default flag): null denominator produces +Infinity")
    void divideDefaultCharacterization(String testName, Double v0, Double v1, Double expected) {
        Double result = evaluateBinary(v0, v1,
                (c0, c1) -> new TestableDivideCalc(NumericType.INSTANCE, c0, c1, false));
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> divideDefaultArguments() {
        return Stream.of(
                Arguments.of("both real values", 6.0, 3.0, 2.0),
                Arguments.of("sentinel numerator -> sentinel (null / x = null)", SENTINEL, 3.0, SENTINEL),
                Arguments.of("Java null numerator -> sentinel", null, 3.0, SENTINEL),
                Arguments.of("sentinel denominator -> +Infinity (x / null = +Inf)",
                        6.0, SENTINEL, Double.POSITIVE_INFINITY),
                Arguments.of("Java null denominator -> +Infinity", 6.0, null, Double.POSITIVE_INFINITY),
                Arguments.of("sentinel over sentinel -> sentinel (numerator wins)", SENTINEL, SENTINEL, SENTINEL),
                // Identity check misses the distinct instance in BOTH positions:
                // it divides as an ordinary tiny number instead of producing
                // sentinel/+Infinity. flips once the sentinel encoding is gone.
                Arguments.of("computed 0.000000012345 numerator is divided as a real value",
                        distinctSentinelValue(), 2.0, SENTINEL_VALUE / 2.0),
                Arguments.of("computed 0.000000012345 denominator is divided as a real value",
                        6.0, distinctSentinelValue(), 6.0 / SENTINEL_VALUE));
    }

    // --- DivideCalc, nullDenominatorProducesNull = true ---------------------

    @ParameterizedTest(name = "{0}: {1} / {2} = {3}")
    @MethodSource("divideNullFlagArguments")
    @DisplayName("DivideCalc (nullDenominatorProducesNull=true): null anywhere produces sentinel")
    void divideNullFlagCharacterization(String testName, Double v0, Double v1, Double expected) {
        Double result = evaluateBinary(v0, v1,
                (c0, c1) -> new TestableDivideCalc(NumericType.INSTANCE, c0, c1, true));
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> divideNullFlagArguments() {
        return Stream.of(
                Arguments.of("both real values", 6.0, 3.0, 2.0),
                Arguments.of("sentinel numerator -> sentinel", SENTINEL, 3.0, SENTINEL),
                Arguments.of("sentinel denominator -> sentinel", 6.0, SENTINEL, SENTINEL),
                Arguments.of("Java null numerator -> sentinel", null, 3.0, SENTINEL),
                Arguments.of("Java null denominator -> sentinel", 6.0, null, SENTINEL),
                // Identity check misses the distinct instance; see the note above.
                Arguments.of("computed 0.000000012345 denominator is divided as a real value",
                        6.0, distinctSentinelValue(), 6.0 / SENTINEL_VALUE));
    }
}
