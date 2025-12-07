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
package org.eclipse.daanse.olap.function.def.vba.timevalue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.type.DateTimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TimeValueCalcTest {

    private TimeValueCalc timeValueCalc;
    private DateTimeCalc dateTimeCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        dateTimeCalc = mock(DateTimeCalc.class);
        evaluator = mock(Evaluator.class);
        timeValueCalc = new TimeValueCalc(DateTimeType.INSTANCE, dateTimeCalc);
    }

    @ParameterizedTest(name = "{0}: TimeValue should have time {2}:{3}:{4}")
    @MethodSource("timeValueArguments")
    @DisplayName("Should extract time portion correctly")
    void shouldExtractTimePortionCorrectly(String testName, LocalDateTime inputDate,
            Integer expectedHour, Integer expectedMinute, Integer expectedSecond) {
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(inputDate);

        LocalDateTime result = timeValueCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(expectedHour);
        assertThat(result.getMinute()).isEqualTo(expectedMinute);
        assertThat(result.getSecond()).isEqualTo(expectedSecond);
        // Should be set to epoch date (January 1, 1970)
        assertThat(result.getYear()).isEqualTo(1970);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(1);
    }

    static Stream<Arguments> timeValueArguments() {
        return Stream.of(
                Arguments.of("morning time", LocalDateTime.of(2023, 12, 25, 9, 30, 45), 9, 30, 45),
                Arguments.of("afternoon time", LocalDateTime.of(2024, 7, 4, 14, 15, 20), 14, 15, 20),
                Arguments.of("late night", LocalDateTime.of(2023, 12, 31, 23, 59, 59), 23, 59, 59),
                Arguments.of("midnight", LocalDateTime.of(2024, 1, 1, 0, 0, 0), 0, 0, 0),
                Arguments.of("noon", LocalDateTime.of(2024, 6, 15, 12, 0, 0), 12, 0, 0));
    }

    @Test
    @DisplayName("Should handle null input date")
    void shouldHandleNullInputDate() {
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(null);

        assertThatThrownBy(() -> timeValueCalc.evaluate(evaluator)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(timeValueCalc.getType()).isEqualTo(DateTimeType.INSTANCE);
    }

    @Test
    @DisplayName("Should handle different dates with same time consistently")
    void shouldHandleDifferentDatesWithSameTimeConsistently() {
        LocalDateTime date1 = LocalDateTime.of(2020, 1, 1, 14, 30, 0);
        LocalDateTime date2 = LocalDateTime.of(2025, 12, 31, 14, 30, 0);

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(date1);
        LocalDateTime result1 = timeValueCalc.evaluate(evaluator);

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(date2);
        LocalDateTime result2 = timeValueCalc.evaluate(evaluator);

        // Both results should have same time portion
        assertThat(result1.getHour()).isEqualTo(result2.getHour());
        assertThat(result1.getMinute()).isEqualTo(result2.getMinute());
        assertThat(result1.getSecond()).isEqualTo(result2.getSecond());

        // Both should have epoch date
        assertThat(result1.getYear()).isEqualTo(1970);
        assertThat(result2.getYear()).isEqualTo(1970);
    }
}
