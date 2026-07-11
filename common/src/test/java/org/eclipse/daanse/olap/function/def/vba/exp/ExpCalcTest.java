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
package org.eclipse.daanse.olap.function.def.vba.exp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.common.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExpCalcTest {

    private ExpCalc expCalc;
    private DoubleCalc doubleCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        doubleCalc = mock(DoubleCalc.class);
        evaluator = mock(Evaluator.class);
        expCalc = new ExpCalc(NumericType.INSTANCE, doubleCalc);
    }

    @ParameterizedTest(name = "{0}: exp({1}) = {2}")
    @MethodSource("arguments")
    @DisplayName("Should calculate exponential correctly")
    void shouldCalculateExponential(String testName, Double input, Double expected) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = expCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-10));
    }

    static Stream<Arguments> arguments() {
        return Stream.of(Arguments.of("zero", 0.0, 1.0), Arguments.of("one", 1.0, Math.E),
                Arguments.of("negative one", -1.0, 1.0 / Math.E), Arguments.of("two", 2.0, Math.E * Math.E),
                Arguments.of("negative two", -2.0, 1.0 / (Math.E * Math.E)), Arguments.of("ln(2)", Math.log(2), 2.0),
                Arguments.of("ln(10)", Math.log(10), 10.0), Arguments.of("small positive", 0.1, Math.exp(0.1)),
                Arguments.of("small negative", -0.1, Math.exp(-0.1)),
//                Arguments.of("large positive", 100.0, Double.POSITIVE_INFINITY),
                Arguments.of("large negative", -100.0, 0.0),
                Arguments.of("positive infinity", Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
                Arguments.of("negative infinity", Double.NEGATIVE_INFINITY, 0.0),
                Arguments.of("not a number", Double.NaN, Double.NaN));
    }

    @Test
    @DisplayName("Should return null for null value")
    void shouldReturnNullForNullValue() {
        when(doubleCalc.evaluate(evaluator)).thenReturn((Double) null);

        Double result = expCalc.evaluate(evaluator);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(expCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}