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
package org.eclipse.daanse.olap.function.def.excel.asin;

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

class AsinhCalcTest {

    private AsinhCalc asinhCalc;
    private DoubleCalc doubleCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        doubleCalc = mock(DoubleCalc.class);
        evaluator = mock(Evaluator.class);
        asinhCalc = new AsinhCalc(NumericType.INSTANCE, doubleCalc);
    }

    @ParameterizedTest(name = "{0}: Asinh({1}) = {2}")
    @MethodSource("validArguments")
    @DisplayName("Should calculate inverse hyperbolic sine correctly")
    void shouldCalculateInverseHyperbolicSine(String testName, Double input, Double expected) {
        when(doubleCalc.evaluate(evaluator)).thenReturn(input);

        Double result = asinhCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-10));
    }

    static Stream<Arguments> validArguments() {
        return Stream.of(Arguments.of("asinh of 0", 0.0, 0.0),
                Arguments.of("asinh of 1", 1.0, Math.log(1 + Math.sqrt(2))),
                Arguments.of("asinh of -1", -1.0, -Math.log(1 + Math.sqrt(2))),
                Arguments.of("asinh of sinh(1)", Math.sinh(1.0), 1.0),
                Arguments.of("asinh of sinh(2)", Math.sinh(2.0), 2.0),
                Arguments.of("asinh of sinh(-1)", Math.sinh(-1.0), -1.0),
                Arguments.of("asinh of large positive", 10.0, Math.log(10.0 + Math.sqrt(101.0))),
                Arguments.of("asinh of large negative", -10.0, -Math.log(10.0 + Math.sqrt(101.0))),
                Arguments.of("asinh of small positive", 0.1, Math.log(0.1 + Math.sqrt(1.01))),
                Arguments.of("asinh of small negative", -0.1, -Math.log(0.1 + Math.sqrt(1.01))));
    }

    @Test
    @DisplayName("Should handle positive infinity")
    void shouldHandlePositiveInfinity() {
        when(doubleCalc.evaluate(evaluator)).thenReturn(Double.POSITIVE_INFINITY);

        Double result = asinhCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        when(doubleCalc.evaluate(evaluator)).thenReturn((Double) null);

        Double result = asinhCalc.evaluate(evaluator);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(asinhCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}