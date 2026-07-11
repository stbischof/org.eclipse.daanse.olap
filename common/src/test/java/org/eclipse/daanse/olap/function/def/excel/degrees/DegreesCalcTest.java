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
package org.eclipse.daanse.olap.function.def.excel.degrees;

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

class DegreesCalcTest {

    private DegreesCalc degreesCalc;
    private DoubleCalc doubleCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        doubleCalc = mock(DoubleCalc.class);
        evaluator = mock(Evaluator.class);
        degreesCalc = new DegreesCalc(NumericType.INSTANCE, doubleCalc);
    }

    @ParameterizedTest(name = "{0}: Degrees({1}) = {2}")
    @MethodSource("validArguments")
    @DisplayName("Should convert radians to degrees correctly")
    void shouldConvertRadiansToDegrees(String testName, Double input, Double expected) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = degreesCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-10));
    }

    static Stream<Arguments> validArguments() {
        return Stream.of(Arguments.of("0 radians to degrees", 0.0, 0.0),
                Arguments.of("π/6 radians to degrees", Math.PI / 6, 30.0),
                Arguments.of("π/4 radians to degrees", Math.PI / 4, 45.0),
                Arguments.of("π/3 radians to degrees", Math.PI / 3, 60.0),
                Arguments.of("π/2 radians to degrees", Math.PI / 2, 90.0),
                Arguments.of("2π/3 radians to degrees", 2 * Math.PI / 3, 120.0),
                Arguments.of("3π/4 radians to degrees", 3 * Math.PI / 4, 135.0),
                Arguments.of("5π/6 radians to degrees", 5 * Math.PI / 6, 150.0),
                Arguments.of("π radians to degrees", Math.PI, 180.0),
                Arguments.of("2π radians to degrees", 2 * Math.PI, 360.0),
                Arguments.of("negative π/4 radians to degrees", -Math.PI / 4, -45.0),
                Arguments.of("negative π radians to degrees", -Math.PI, -180.0),
                Arguments.of("1 radian to degrees", 1.0, 180.0 / Math.PI));
    }

    @Test
    @DisplayName("Should handle positive infinity")
    void shouldHandlePositiveInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.POSITIVE_INFINITY);

        Double result = degreesCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    @DisplayName("Should handle negative infinity")
    void shouldHandleNegativeInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.NEGATIVE_INFINITY);

        Double result = degreesCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @Test
    @DisplayName("Should handle NaN")
    void shouldHandleNaN() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.NaN);

        Double result = degreesCalc.evaluate(evaluator);

        assertThat(result).isNaN();
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        when(doubleCalc.evaluate(evaluator)).thenReturn((Double) null);

        Double result = degreesCalc.evaluate(evaluator);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(degreesCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}