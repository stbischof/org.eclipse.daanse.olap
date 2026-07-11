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
package org.eclipse.daanse.olap.function.def.excel.sinh;

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

class SinhCalcTest {

    private SinhCalc sinhCalc;
    private DoubleCalc doubleCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        doubleCalc = mock(DoubleCalc.class);
        evaluator = mock(Evaluator.class);
        sinhCalc = new SinhCalc(NumericType.INSTANCE, doubleCalc);
    }

    @ParameterizedTest(name = "{0}: Sinh({1}) = {2}")
    @MethodSource("validArguments")
    @DisplayName("Should calculate hyperbolic sine correctly")
    void shouldCalculateHyperbolicSine(String testName, Double input, Double expected) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = sinhCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-10));
    }

    static Stream<Arguments> validArguments() {
        return Stream.of(Arguments.of("sinh of 0", 0.0, 0.0), Arguments.of("sinh of 1", 1.0, (Math.E - 1 / Math.E) / 2),
                Arguments.of("sinh of -1", -1.0, -(Math.E - 1 / Math.E) / 2),
                Arguments.of("sinh of 2", 2.0, (Math.E * Math.E - 1 / (Math.E * Math.E)) / 2),
                Arguments.of("sinh of -2", -2.0, -(Math.E * Math.E - 1 / (Math.E * Math.E)) / 2),
                Arguments.of("sinh of 0.5", 0.5, Math.sinh(0.5)), Arguments.of("sinh of -0.5", -0.5, Math.sinh(-0.5)),
                Arguments.of("small positive value", 0.1, Math.sinh(0.1)),
                Arguments.of("small negative value", -0.1, Math.sinh(-0.1)),
                Arguments.of("large positive value", 5.0, Math.sinh(5.0)),
                Arguments.of("large negative value", -5.0, Math.sinh(-5.0)));
    }

    @Test
    @DisplayName("Should handle positive infinity")
    void shouldHandlePositiveInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.POSITIVE_INFINITY);

        Double result = sinhCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    @DisplayName("Should handle negative infinity")
    void shouldHandleNegativeInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.NEGATIVE_INFINITY);

        Double result = sinhCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @Test
    @DisplayName("Should handle NaN")
    void shouldHandleNaN() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.NaN);

        Double result = sinhCalc.evaluate(evaluator);

        assertThat(result).isNaN();
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        when(doubleCalc.evaluate(evaluator)).thenReturn((Double) null);

        Double result = sinhCalc.evaluate(evaluator);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should be odd function property")
    void shouldBeOddFunction() {
        // sinh(-x) = -sinh(x)
        double testValue = 2.5;

        when(doubleCalc.evaluate(evaluator)).thenReturn(testValue);
        Double positiveResult = sinhCalc.evaluate(evaluator);

        when(doubleCalc.evaluate(evaluator)).thenReturn(-testValue);
        Double negativeResult = sinhCalc.evaluate(evaluator);

        assertThat(negativeResult).isCloseTo(-positiveResult, org.assertj.core.data.Offset.offset(1e-10));
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(sinhCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}