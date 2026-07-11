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

class AcoshCalcTest {

    private AcoshCalc acoshCalc;
    private DoubleCalc doubleCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        doubleCalc = mock(DoubleCalc.class);
        evaluator = mock(Evaluator.class);
        acoshCalc = new AcoshCalc(NumericType.INSTANCE, doubleCalc);
    }

    @ParameterizedTest(name = "{0}: Acosh({1}) = {2}")
    @MethodSource("validArguments")
    @DisplayName("Should calculate inverse hyperbolic cosine correctly")
    void shouldCalculateInverseHyperbolicCosine(String testName, Double input, Double expected) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = acoshCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-10));
    }

    static Stream<Arguments> validArguments() {
        return Stream.of(Arguments.of("acosh of 1 (0)", 1.0, 0.0),
                Arguments.of("acosh of cosh(1)", Math.cosh(1.0), 1.0),
                Arguments.of("acosh of cosh(2)", Math.cosh(2.0), 2.0),
                Arguments.of("acosh of cosh(0.5)", Math.cosh(0.5), 0.5),
                Arguments.of("acosh of 2", 2.0, Math.log(2 + Math.sqrt(3))),
                Arguments.of("acosh of 3", 3.0, Math.log(3 + Math.sqrt(8))),
                Arguments.of("acosh of 10", 10.0, Math.log(10 + Math.sqrt(99))),
                Arguments.of("very large value", 1000.0, Math.log(1000.0 + Math.sqrt(999999.0))),
                Arguments.of("small value just above 1", 1.01, Math.log(1.01 + Math.sqrt(1.01 * 1.01 - 1.0))));
    }

    @ParameterizedTest(name = "{0}: Acosh({1}) = NaN")
    @MethodSource("invalidArguments")
    @DisplayName("Should return NaN for values less than 1")
    void shouldReturnNaNForInvalidValues(String testName, Double input) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = acoshCalc.evaluate(evaluator);

        assertThat(result).isNaN();
    }

    static Stream<Arguments> invalidArguments() {
        return Stream.of(Arguments.of("value less than 1", 0.9), Arguments.of("zero", 0.0),
                Arguments.of("negative value", -1.0), Arguments.of("large negative value", -10.0),
                Arguments.of("negative infinity", Double.NEGATIVE_INFINITY), Arguments.of("NaN input", Double.NaN));
    }

    @Test
    @DisplayName("Should handle positive infinity")
    void shouldHandlePositiveInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.POSITIVE_INFINITY);

        Double result = acoshCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        when(doubleCalc.evaluate(evaluator)).thenReturn((Double) null);

        Double result = acoshCalc.evaluate(evaluator);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(acoshCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}