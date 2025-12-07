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
package org.eclipse.daanse.olap.function.def.vba.second;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SecondCalcTest {

    private SecondCalc secondCalc;
    private DateTimeCalc dateTimeCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        dateTimeCalc = mock(DateTimeCalc.class);
        evaluator = mock(Evaluator.class);
        secondCalc = new SecondCalc(NumericType.INSTANCE, dateTimeCalc);
    }

    @ParameterizedTest(name = "{0}: second({1}) = {2}")
    @MethodSource("arguments")
    @DisplayName("Should extract second from date correctly")
    void shouldExtractSecondFromDate(String testName, LocalDateTime input, Integer expected) {
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(input);

        Integer result = secondCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of("start of minute", LocalDateTime.of(2024, 1, 1, 12, 30, 0), 0),
                Arguments.of("fifteen seconds", LocalDateTime.of(2024, 1, 1, 12, 30, 15), 15),
                Arguments.of("thirty seconds", LocalDateTime.of(2024, 1, 1, 12, 30, 30), 30),
                Arguments.of("forty-five seconds", LocalDateTime.of(2024, 1, 1, 12, 30, 45), 45),
                Arguments.of("end of minute", LocalDateTime.of(2024, 1, 1, 12, 30, 59), 59),
                Arguments.of("midnight", LocalDateTime.of(2024, 1, 1, 0, 0, 5), 5),
                Arguments.of("early morning", LocalDateTime.of(2024, 1, 1, 1, 23, 42), 42),
                Arguments.of("late morning", LocalDateTime.of(2024, 1, 1, 11, 37, 18), 18),
                Arguments.of("afternoon", LocalDateTime.of(2024, 1, 1, 15, 42, 33), 33),
                Arguments.of("evening", LocalDateTime.of(2024, 1, 1, 19, 18, 27), 27),
                Arguments.of("late night", LocalDateTime.of(2024, 1, 1, 23, 55, 51), 51),
                Arguments.of("different year", LocalDateTime.of(2023, 12, 31, 14, 25, 37), 37),
                Arguments.of("leap year", LocalDateTime.of(2024, 2, 29, 16, 47, 8), 8));
    }
}
