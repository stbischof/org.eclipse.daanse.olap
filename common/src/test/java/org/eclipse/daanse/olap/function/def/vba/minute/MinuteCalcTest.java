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
package org.eclipse.daanse.olap.function.def.vba.minute;

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

class MinuteCalcTest {

    private MinuteCalc minuteCalc;
    private DateTimeCalc dateTimeCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        dateTimeCalc = mock(DateTimeCalc.class);
        evaluator = mock(Evaluator.class);
        minuteCalc = new MinuteCalc(NumericType.INSTANCE, dateTimeCalc);
    }

    @ParameterizedTest(name = "{0}: minute({1}) = {2}")
    @MethodSource("arguments")
    @DisplayName("Should extract minute from date correctly")
    void shouldExtractMinuteFromDate(String testName, LocalDateTime input, Integer expected) {
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(input);

        Integer result = minuteCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of("midnight", LocalDateTime.of(2024, 1, 1, 0, 0, 0), 0),
                Arguments.of("start of hour", LocalDateTime.of(2024, 1, 1, 12, 0, 0), 0),
                Arguments.of("fifteen minutes", LocalDateTime.of(2024, 1, 1, 12, 15, 0), 15),
                Arguments.of("thirty minutes", LocalDateTime.of(2024, 1, 1, 12, 30, 0), 30),
                Arguments.of("forty-five minutes", LocalDateTime.of(2024, 1, 1, 12, 45, 0), 45),
                Arguments.of("end of hour", LocalDateTime.of(2024, 1, 1, 12, 59, 0), 59),
                Arguments.of("early morning", LocalDateTime.of(2024, 1, 1, 1, 23, 0), 23),
                Arguments.of("late morning", LocalDateTime.of(2024, 1, 1, 11, 37, 0), 37),
                Arguments.of("afternoon", LocalDateTime.of(2024, 1, 1, 15, 42, 0), 42),
                Arguments.of("evening", LocalDateTime.of(2024, 1, 1, 19, 18, 0), 18),
                Arguments.of("late night", LocalDateTime.of(2024, 1, 1, 23, 55, 0), 55),
                Arguments.of("different year", LocalDateTime.of(2023, 12, 31, 14, 25, 0), 25),
                Arguments.of("leap year", LocalDateTime.of(2024, 2, 29, 16, 47, 0), 47));
    }
}
