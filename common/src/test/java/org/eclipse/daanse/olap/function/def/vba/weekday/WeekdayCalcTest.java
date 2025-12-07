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
package org.eclipse.daanse.olap.function.def.vba.weekday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WeekdayCalcTest {

    private WeekdayCalc weekdayCalc;
    private DateTimeCalc dateTimeCalc;
    private IntegerCalc firstDayOfWeekCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        dateTimeCalc = mock(DateTimeCalc.class);
        firstDayOfWeekCalc = mock(IntegerCalc.class);
        evaluator = mock(Evaluator.class);
        weekdayCalc = new WeekdayCalc(NumericType.INSTANCE, dateTimeCalc, firstDayOfWeekCalc);
    }

    @ParameterizedTest(name = "{0}: Weekday with firstDay={1} should return {2}")
    @MethodSource("weekdayArguments")
    @DisplayName("Should calculate weekday correctly")
    void shouldCalculateWeekdayCorrectly(String testName, Integer firstDayOfWeek, Integer expectedWeekday) {
        // Create a known date - let's use January 1, 2024 (which is a Monday)
        LocalDateTime testDate = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(testDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(firstDayOfWeek);

        Integer result = weekdayCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expectedWeekday);
    }

    static Stream<Arguments> weekdayArguments() {
        // January 1, 2024 is a Monday
        // Java Calendar: SUNDAY=1, MONDAY=2, TUESDAY=3, WEDNESDAY=4, THURSDAY=5,
        // FRIDAY=6, SATURDAY=7
        // So January 1, 2024 has Calendar.DAY_OF_WEEK = 2 (Monday)

        return Stream.of(
                // With Sunday as first day (Calendar.SUNDAY = 1)
                Arguments.of("Sunday first day", Calendar.SUNDAY, 2), // Monday = 2nd day of week

                // With Monday as first day (Calendar.MONDAY = 2)
                Arguments.of("Monday first day", Calendar.MONDAY, 1), // Monday = 1st day of week

                // With Tuesday as first day (Calendar.TUESDAY = 3)
                Arguments.of("Tuesday first day", Calendar.TUESDAY, 7), // Monday = 7th day of week

                // With Wednesday as first day (Calendar.WEDNESDAY = 4)
                Arguments.of("Wednesday first day", Calendar.WEDNESDAY, 6), // Monday = 6th day of week

                // With Thursday as first day (Calendar.THURSDAY = 5)
                Arguments.of("Thursday first day", Calendar.THURSDAY, 5), // Monday = 5th day of week

                // With Friday as first day (Calendar.FRIDAY = 6)
                Arguments.of("Friday first day", Calendar.FRIDAY, 4), // Monday = 4th day of week

                // With Saturday as first day (Calendar.SATURDAY = 7)
                Arguments.of("Saturday first day", Calendar.SATURDAY, 3) // Monday = 3rd day of week
        );
    }

    @Test
    @DisplayName("Should handle different weekdays consistently")
    void shouldHandleDifferentWeekdaysConsistently() {
        // Test with a known Sunday (January 7, 2024)
        LocalDateTime sunday = LocalDateTime.of(2024, 1, 7, 12, 0, 0);

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(sunday);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);

        Integer result = weekdayCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(1); // Sunday should be 1 when Sunday is first day
    }

    @Test
    @DisplayName("Should handle Saturday correctly")
    void shouldHandleSaturdayCorrectly() {
        // Test with a known Saturday (January 6, 2024)
        LocalDateTime saturday = LocalDateTime.of(2024, 1, 6, 12, 0, 0);

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(saturday);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);

        Integer result = weekdayCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(7); // Saturday should be 7 when Sunday is first day
    }

    @Test
    @DisplayName("Should handle null date")
    void shouldHandleNullDate() {
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(null);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);

        assertThatThrownBy(() -> weekdayCalc.evaluate(evaluator)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle null first day of week")
    void shouldHandleNullFirstDayOfWeek() {
        LocalDateTime testDate = LocalDateTime.now();

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(testDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(null);

        assertThatThrownBy(() -> weekdayCalc.evaluate(evaluator)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(weekdayCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }

    @Test
    @DisplayName("Should return values between 1 and 7")
    void shouldReturnValuesBetween1And7() {
        // Test multiple dates throughout the week
        for (int day = 1; day <= 7; day++) {
            LocalDateTime testDate = LocalDateTime.of(2024, 1, day, 12, 0, 0);

            when(dateTimeCalc.evaluate(evaluator)).thenReturn(testDate);
            when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);

            Integer result = weekdayCalc.evaluate(evaluator);

            assertThat(result).isBetween(1, 7);
        }
    }

    @Test
    @DisplayName("Should handle year boundaries")
    void shouldHandleYearBoundaries() {
        // Test December 31, 2023 (Sunday)
        LocalDateTime yearEnd = LocalDateTime.of(2023, 12, 31, 12, 0, 0);

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(yearEnd);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);

        Integer result = weekdayCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(1); // Sunday should be 1
    }

    @Test
    @DisplayName("Should handle leap year dates")
    void shouldHandleLeapYearDates() {
        // Test February 29, 2024 (leap year, Thursday)
        LocalDateTime leapDay = LocalDateTime.of(2024, 2, 29, 12, 0, 0);

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(leapDay);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);

        Integer result = weekdayCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result).isBetween(1, 7);
    }

    @Test
    @DisplayName("Should handle different times on same day")
    void shouldHandleDifferentTimesOnSameDay() {
        LocalDateTime midnight = LocalDateTime.of(2024, 3, 15, 0, 0, 0);
        LocalDateTime almostMidnight = LocalDateTime.of(2024, 3, 15, 23, 59, 59);

        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);

        // Test midnight
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(midnight);
        Integer result1 = weekdayCalc.evaluate(evaluator);

        // Test end of day
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(almostMidnight);
        Integer result2 = weekdayCalc.evaluate(evaluator);

        // Both should return the same weekday
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("Should verify VBA Weekday function behavior")
    void shouldVerifyVBAWeekdayFunctionBehavior() {
        // VBA Weekday function returns 1-7 representing the day of the week
        // with adjustable first day of the week

        // Test with a known Wednesday (February 14, 2024)
        LocalDateTime wednesday = LocalDateTime.of(2024, 2, 14, 12, 0, 0);

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(wednesday);

        // With Sunday as first day, Wednesday should be 4
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        Integer sundayFirst = weekdayCalc.evaluate(evaluator);

        // With Monday as first day, Wednesday should be 3
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.MONDAY);
        Integer mondayFirst = weekdayCalc.evaluate(evaluator);

        assertThat(sundayFirst).isEqualTo(4);
        assertThat(mondayFirst).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle invalid first day of week values gracefully")
    void shouldHandleInvalidFirstDayOfWeekValuesGracefully() {
        LocalDateTime testDate = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(testDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(0); // Invalid value

        Integer result = weekdayCalc.evaluate(evaluator);

        // Should still return a valid weekday number
        assertThat(result).isBetween(1, 7);
    }

    @Test
    @DisplayName("Should handle large first day of week values")
    @Disabled
    void shouldHandleLargeFirstDayOfWeekValues() {
        LocalDateTime testDate = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        when(dateTimeCalc.evaluate(evaluator)).thenReturn(testDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(10); // Large value

        Integer result = weekdayCalc.evaluate(evaluator);

        // Should still return a valid weekday number
        assertThat(result).isBetween(1, 7);
    }
}
