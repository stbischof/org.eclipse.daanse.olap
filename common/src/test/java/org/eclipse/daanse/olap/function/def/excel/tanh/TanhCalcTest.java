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
package org.eclipse.daanse.olap.function.def.excel.tanh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TanhCalcTest {

    private TanhCalc tanhCalc;
    private DoubleCalc doubleCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        doubleCalc = mock(DoubleCalc.class);
        evaluator = mock(Evaluator.class);
        tanhCalc = new TanhCalc(NumericType.INSTANCE, doubleCalc);
    }

    @ParameterizedTest(name = "{0}: Tanh({1}) = {2}")
    @MethodSource("validArguments")
    @DisplayName("Should calculate hyperbolic tangent correctly")
    void shouldCalculateHyperbolicTangent(String testName, Double input, Double expected) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = tanhCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-10));
    }

    static Stream<Arguments> validArguments() {
        return Stream.of(Arguments.of("tanh of 0", 0.0, 0.0), Arguments.of("tanh of 1", 1.0, Math.tanh(1.0)),
                Arguments.of("tanh of -1", -1.0, Math.tanh(-1.0)), Arguments.of("tanh of 2", 2.0, Math.tanh(2.0)),
                Arguments.of("tanh of -2", -2.0, Math.tanh(-2.0)), Arguments.of("tanh of 0.5", 0.5, Math.tanh(0.5)),
                Arguments.of("tanh of -0.5", -0.5, Math.tanh(-0.5)),
                Arguments.of("small positive value", 0.1, Math.tanh(0.1)),
                Arguments.of("small negative value", -0.1, Math.tanh(-0.1)),
                Arguments.of("large positive value (approaches 1)", 5.0, Math.tanh(5.0)),
                Arguments.of("large negative value (approaches -1)", -5.0, Math.tanh(-5.0)));
    }

    @Test
    @DisplayName("Should approach 1 for large positive values")
    void shouldApproachOneForLargePositiveValues() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(10.0);

        Double result = tanhCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-8));
        assertThat(result).isLessThan(1.0); // Should never reach exactly 1
    }

    @Test
    @DisplayName("Should approach -1 for large negative values")
    void shouldApproachNegativeOneForLargeNegativeValues() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(-10.0);

        Double result = tanhCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(-1.0, org.assertj.core.data.Offset.offset(1e-8));
        assertThat(result).isGreaterThan(-1.0); // Should never reach exactly -1
    }

    @Test
    @DisplayName("Should handle positive infinity (approaches 1)")
    void shouldHandlePositiveInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.POSITIVE_INFINITY);

        Double result = tanhCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle negative infinity (approaches -1)")
    void shouldHandleNegativeInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.NEGATIVE_INFINITY);

        Double result = tanhCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(-1.0);
    }

    @Test
    @DisplayName("Should handle NaN")
    void shouldHandleNaN() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.NaN);

        Double result = tanhCalc.evaluate(evaluator);

        assertThat(result).isNaN();
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        when(doubleCalc.evaluate(evaluator)).thenReturn((Double) null);

        Double result = tanhCalc.evaluate(evaluator);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should be odd function property")
    void shouldBeOddFunction() {
        // tanh(-x) = -tanh(x)
        double testValue = 1.5;

        when(doubleCalc.evaluate(evaluator)).thenReturn(testValue);
        Double positiveResult = tanhCalc.evaluate(evaluator);

        when(doubleCalc.evaluate(evaluator)).thenReturn(-testValue);
        Double negativeResult = tanhCalc.evaluate(evaluator);

        assertThat(negativeResult).isCloseTo(-positiveResult, org.assertj.core.data.Offset.offset(1e-10));
    }

    @Test
    @DisplayName("Should verify bounded property")
    void shouldVerifyBoundedProperty() {
        // tanh(x) is always in the range (-1, 1)
        double[] testValues = { -100.0, -10.0, -1.0, -0.1, 0.0, 0.1, 1.0, 10.0, 100.0 };

        for (double value : testValues) {
            when(doubleCalc.evaluate(evaluator)).thenReturn(value);
            Double result = tanhCalc.evaluate(evaluator);

            assertThat(result).isBetween(-1.0, 1.0);
        }
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(tanhCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}