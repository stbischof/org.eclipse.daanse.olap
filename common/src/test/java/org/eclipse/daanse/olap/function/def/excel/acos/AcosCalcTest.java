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
package org.eclipse.daanse.olap.function.def.excel.acos;

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

class AcosCalcTest {

    private AcosCalc acosCalc;
    private DoubleCalc doubleCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        doubleCalc = mock(DoubleCalc.class);
        evaluator = mock(Evaluator.class);
        acosCalc = new AcosCalc(NumericType.INSTANCE, doubleCalc);
    }

    @ParameterizedTest(name = "{0}: Acos({1}) = {2}")
    @MethodSource("validArguments")
    @DisplayName("Should calculate arccosine correctly")
    void shouldCalculateArcCosine(String testName, Double input, Double expected) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = acosCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-10));
    }

    static Stream<Arguments> validArguments() {
        return Stream.of(Arguments.of("cosine of 0 (π/2)", 0.0, Math.PI / 2), Arguments.of("cosine of 1 (0)", 1.0, 0.0),
                Arguments.of("cosine of -1 (π)", -1.0, Math.PI), Arguments.of("cosine of 0.5 (π/3)", 0.5, Math.PI / 3),
                Arguments.of("cosine of -0.5 (2π/3)", -0.5, 2 * Math.PI / 3),
                Arguments.of("cosine of √2/2 (π/4)", Math.sqrt(2) / 2, Math.PI / 4),
                Arguments.of("cosine of -√2/2 (3π/4)", -Math.sqrt(2) / 2, 3 * Math.PI / 4),
                Arguments.of("cosine of √3/2 (π/6)", Math.sqrt(3) / 2, Math.PI / 6),
                Arguments.of("cosine of -√3/2 (5π/6)", -Math.sqrt(3) / 2, 5 * Math.PI / 6),
                Arguments.of("very small positive value", 0.0001, Math.acos(0.0001)),
                Arguments.of("very small negative value", -0.0001, Math.acos(-0.0001)));
    }

    @ParameterizedTest(name = "{0}: Acos({1}) = NaN")
    @MethodSource("invalidArguments")
    @DisplayName("Should return NaN for values outside [-1, 1] range")
    void shouldReturnNaNForInvalidValues(String testName, Double input) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = acosCalc.evaluate(evaluator);

        assertThat(result).isNaN();
    }

    static Stream<Arguments> invalidArguments() {
        return Stream.of(Arguments.of("value greater than 1", 1.1), Arguments.of("value less than -1", -1.1),
                Arguments.of("large positive value", 10.0), Arguments.of("large negative value", -10.0),
                Arguments.of("positive infinity", Double.POSITIVE_INFINITY),
                Arguments.of("negative infinity", Double.NEGATIVE_INFINITY), Arguments.of("NaN input", Double.NaN));
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        when(doubleCalc.evaluate(evaluator)).thenReturn((Double) null);

        Double result = acosCalc.evaluate(evaluator);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(acosCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}