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
package org.eclipse.daanse.olap.function.def.excel.radians;

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

class RadiansCalcTest {

    private RadiansCalc radiansCalc;
    private DoubleCalc doubleCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        doubleCalc = mock(DoubleCalc.class);
        evaluator = mock(Evaluator.class);
        radiansCalc = new RadiansCalc(NumericType.INSTANCE, doubleCalc);
    }

    @ParameterizedTest(name = "{0}: Radians({1}) = {2}")
    @MethodSource("validArguments")
    @DisplayName("Should convert degrees to radians correctly")
    void shouldConvertDegreesToRadians(String testName, Double input, Double expected) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = radiansCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-10));
    }

    static Stream<Arguments> validArguments() {
        return Stream.of(Arguments.of("0 degrees to radians", 0.0, 0.0),
                Arguments.of("30 degrees to radians", 30.0, Math.PI / 6),
                Arguments.of("45 degrees to radians", 45.0, Math.PI / 4),
                Arguments.of("60 degrees to radians", 60.0, Math.PI / 3),
                Arguments.of("90 degrees to radians", 90.0, Math.PI / 2),
                Arguments.of("120 degrees to radians", 120.0, 2 * Math.PI / 3),
                Arguments.of("135 degrees to radians", 135.0, 3 * Math.PI / 4),
                Arguments.of("150 degrees to radians", 150.0, 5 * Math.PI / 6),
                Arguments.of("180 degrees to radians", 180.0, Math.PI),
                Arguments.of("360 degrees to radians", 360.0, 2 * Math.PI),
                Arguments.of("-45 degrees to radians", -45.0, -Math.PI / 4),
                Arguments.of("-180 degrees to radians", -180.0, -Math.PI),
                Arguments.of("57.29578 degrees to radians", 180.0 / Math.PI, 1.0));
    }

    @Test
    @DisplayName("Should handle positive infinity")
    void shouldHandlePositiveInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.POSITIVE_INFINITY);

        Double result = radiansCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    @DisplayName("Should handle negative infinity")
    void shouldHandleNegativeInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.NEGATIVE_INFINITY);

        Double result = radiansCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @Test
    @DisplayName("Should handle NaN")
    void shouldHandleNaN() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.NaN);

        Double result = radiansCalc.evaluate(evaluator);

        assertThat(result).isNaN();
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        when(doubleCalc.evaluate(evaluator)).thenReturn((Double) null);

        Double result = radiansCalc.evaluate(evaluator);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(radiansCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}