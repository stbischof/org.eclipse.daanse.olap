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
package org.eclipse.daanse.olap.function.def.excel.sqrtpi;

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

class SqrtPiCalcTest {

    private SqrtPiCalc sqrtPiCalc;
    private DoubleCalc doubleCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        doubleCalc = mock(DoubleCalc.class);
        evaluator = mock(Evaluator.class);
        sqrtPiCalc = new SqrtPiCalc(NumericType.INSTANCE, doubleCalc);
    }

    @ParameterizedTest(name = "{0}: SqrtPi({1}) = {2}")
    @MethodSource("validArguments")
    @DisplayName("Should calculate square root of (number * Pi) correctly")
    void shouldCalculateSqrtPi(String testName, Double input, Double expected) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = sqrtPiCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-10));
    }

    static Stream<Arguments> validArguments() {
        return Stream.of(Arguments.of("sqrtpi of 0", 0.0, 0.0), Arguments.of("sqrtpi of 1", 1.0, Math.sqrt(Math.PI)),
                Arguments.of("sqrtpi of 4", 4.0, Math.sqrt(4.0 * Math.PI)),
                Arguments.of("sqrtpi of 9", 9.0, Math.sqrt(9.0 * Math.PI)),
                Arguments.of("sqrtpi of 16", 16.0, Math.sqrt(16.0 * Math.PI)),
                Arguments.of("sqrtpi of 0.25", 0.25, Math.sqrt(0.25 * Math.PI)),
                Arguments.of("sqrtpi of 2", 2.0, Math.sqrt(2.0 * Math.PI)),
                Arguments.of("sqrtpi of 10", 10.0, Math.sqrt(10.0 * Math.PI)),
                Arguments.of("sqrtpi of Pi (gives Pi)", Math.PI, Math.sqrt(Math.PI * Math.PI)),
                Arguments.of("sqrtpi of 1/Pi", 1.0 / Math.PI, Math.sqrt(1.0)),
                Arguments.of("small positive value", 0.01, Math.sqrt(0.01 * Math.PI)));
    }

    @ParameterizedTest(name = "{0}: SqrtPi({1}) = NaN")
    @MethodSource("invalidArguments")
    @DisplayName("Should return NaN for negative values")
    void shouldReturnNaNForInvalidValues(String testName, Double input) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = sqrtPiCalc.evaluate(evaluator);

        assertThat(result).isNaN();
    }

    static Stream<Arguments> invalidArguments() {
        return Stream.of(Arguments.of("negative value", -1.0), Arguments.of("large negative value", -10.0),
                Arguments.of("negative infinity", Double.NEGATIVE_INFINITY), Arguments.of("NaN input", Double.NaN));
    }

    @Test
    @DisplayName("Should handle positive infinity")
    void shouldHandlePositiveInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.POSITIVE_INFINITY);

        Double result = sqrtPiCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        when(doubleCalc.evaluate(evaluator)).thenReturn((Double) null);

        Double result = sqrtPiCalc.evaluate(evaluator);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should verify mathematical relationships")
    void shouldVerifyMathematicalRelationships() {
        // For input 1, result should be sqrt(Pi)
        when(doubleCalc.evaluate(evaluator)).thenReturn(1.0);
        Double resultFor1 = sqrtPiCalc.evaluate(evaluator);
        assertThat(resultFor1 * resultFor1).isCloseTo(Math.PI, org.assertj.core.data.Offset.offset(1e-10));

        // For input 4, result should be 2 * sqrt(Pi)
        when(doubleCalc.evaluate(evaluator)).thenReturn(4.0);
        Double resultFor4 = sqrtPiCalc.evaluate(evaluator);
        assertThat(resultFor4).isCloseTo(2.0 * Math.sqrt(Math.PI), org.assertj.core.data.Offset.offset(1e-10));
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(sqrtPiCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}