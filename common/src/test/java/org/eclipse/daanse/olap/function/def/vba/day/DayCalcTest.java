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
package org.eclipse.daanse.olap.function.def.vba.day;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DayCalcTest {

    private DayCalc dayCalc;
    private DateTimeCalc dateTimeCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        dateTimeCalc = mock(DateTimeCalc.class);
        evaluator = mock(Evaluator.class);
        dayCalc = new DayCalc(NumericType.INSTANCE, dateTimeCalc);
    }

    @ParameterizedTest(name = "{0}: day({1}) = {2}")
    @MethodSource("arguments")
    @DisplayName("Should extract day from date correctly")
    void shouldExtractDay(String testName, LocalDateTime inputDate, Integer expected) {
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(inputDate);

        Integer result = dayCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of("first day of month", LocalDateTime.of(2023, 1, 1, 0, 0, 0), 1),
                Arguments.of("last day of February non-leap", LocalDateTime.of(2023, 2, 28, 12, 30, 45), 28),
                Arguments.of("last day of March", LocalDateTime.of(2024, 3, 31, 23, 59, 59), 31),
                Arguments.of("leap year February 29", LocalDateTime.of(2024, 2, 29, 6, 15, 30), 29),
                Arguments.of("last day of year", LocalDateTime.of(2025, 12, 31, 18, 45, 0), 31),
                Arguments.of("mid-month date", LocalDateTime.of(2023, 6, 15, 9, 30, 0), 15));
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(dayCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}
