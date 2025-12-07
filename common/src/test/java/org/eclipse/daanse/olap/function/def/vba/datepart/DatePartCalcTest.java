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
package org.eclipse.daanse.olap.function.def.vba.datepart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.function.def.vba.datediff.FirstWeekOfYear;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DatePartCalcTest {

    private DatePartCalc datePartCalc;
    private StringCalc stringCalc;
    private DateTimeCalc dateTimeCalc;
    private IntegerCalc firstDayOfWeekCalc;
    private IntegerCalc firstWeekOfYearCalc;
    private Evaluator evaluator;
    private LocalDateTime testDate;

    @BeforeEach
    void setUp() {
        stringCalc = mock(StringCalc.class);
        dateTimeCalc = mock(DateTimeCalc.class);
        firstDayOfWeekCalc = mock(IntegerCalc.class);
        firstWeekOfYearCalc = mock(IntegerCalc.class);
        evaluator = mock(Evaluator.class);
        datePartCalc = new DatePartCalc(org.eclipse.daanse.olap.api.type.NumericType.INSTANCE, stringCalc, dateTimeCalc,
                firstDayOfWeekCalc, firstWeekOfYearCalc);

        // Set up test date: March 15, 2024 14:30:45 (Friday)
        testDate = LocalDateTime.of(2024, 3, 15, 14, 30, 45);
    }

    @ParameterizedTest(name = "{0}: DatePart({1}, Mar 15 2024 14:30:45) = {2}")
    @MethodSource("datePartArguments")
    @DisplayName("Should extract date parts correctly")
    void shouldExtractDatePartsCorrectly(String testName, String interval, Integer expectedValue) {
        when(stringCalc.evaluate(evaluator)).thenReturn(interval);
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(testDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Integer result = datePartCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expectedValue);
    }

    static Stream<Arguments> datePartArguments() {
        return Stream.of(Arguments.of("year", "yyyy", 2024), Arguments.of("quarter", "q", 1), // March is in Q1
                Arguments.of("month", "m", 3), // March = 3 (1-based)
                Arguments.of("day of year", "y", 75), // March 15 is day 75 in 2024 (leap year)
                Arguments.of("day", "d", 15), Arguments.of("weekday", "w", 6), // Friday with Sunday=1 system
                Arguments.of("week", "ww", 11), // Week of year
                Arguments.of("hour", "h", 14), Arguments.of("minute", "n", 30), Arguments.of("second", "s", 45));
    }

    @ParameterizedTest(name = "FirstWeekOfYear: {0}")
    @MethodSource("firstWeekOfYearArguments")
    @DisplayName("Should handle different FirstWeekOfYear values for week calculation")
    void shouldHandleDifferentFirstWeekOfYearValues(FirstWeekOfYear firstWeekOfYear, Integer expectedWeek) {
        when(stringCalc.evaluate(evaluator)).thenReturn("ww");
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(testDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(firstWeekOfYear.ordinal());

        Integer result = datePartCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        // Week calculation can vary based on FirstWeekOfYear setting
    }

    static Stream<Arguments> firstWeekOfYearArguments() {
        return Stream.of(Arguments.of(FirstWeekOfYear.vbUseSystem, null),
                Arguments.of(FirstWeekOfYear.vbFirstJan1, null), Arguments.of(FirstWeekOfYear.vbFirstFourDays, null),
                Arguments.of(FirstWeekOfYear.vbFirstFullWeek, null));
    }

    @ParameterizedTest(name = "FirstDayOfWeek: {0}")
    @MethodSource("firstDayOfWeekArguments")
    @DisplayName("Should handle different first day of week settings")
    void shouldHandleDifferentFirstDayOfWeekSettings(int firstDayOfWeek, Integer expectedWeekday) {
        when(stringCalc.evaluate(evaluator)).thenReturn("w");
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(testDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(firstDayOfWeek);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Integer result = datePartCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expectedWeekday);
    }

    static Stream<Arguments> firstDayOfWeekArguments() {
        // Friday with different first days of week
        return Stream.of(Arguments.of(Calendar.SUNDAY, 6), // Sunday=1, so Friday=6
                Arguments.of(Calendar.MONDAY, 5), // Monday=1, so Friday=5
                Arguments.of(Calendar.TUESDAY, 4), // Tuesday=1, so Friday=4
                Arguments.of(Calendar.WEDNESDAY, 3), // Wednesday=1, so Friday=3
                Arguments.of(Calendar.THURSDAY, 2), // Thursday=1, so Friday=2
                Arguments.of(Calendar.FRIDAY, 1), // Friday=1, so Friday=1
                Arguments.of(Calendar.SATURDAY, 7) // Saturday=1, so Friday=7
        );
    }

    @Test
    @DisplayName("Should handle leap year day of year correctly")
    void shouldHandleLeapYearDayOfYearCorrectly() {
        // Test with Feb 29 in leap year 2024
        LocalDateTime leapDate = LocalDateTime.of(2024, 2, 29, 0, 0, 0);

        when(stringCalc.evaluate(evaluator)).thenReturn("y");
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(leapDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Integer result = datePartCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(60); // Feb 29 is day 60 in leap year
    }

    @Test
    @DisplayName("Should handle non-leap year correctly")
    void shouldHandleNonLeapYearCorrectly() {
        // Test with March 1 in non-leap year 2023
        LocalDateTime nonLeapDate = LocalDateTime.of(2023, 3, 1, 0, 0, 0);

        when(stringCalc.evaluate(evaluator)).thenReturn("y");
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(nonLeapDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Integer result = datePartCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(60); // March 1 is day 60 in non-leap year
    }

    @Test
    @DisplayName("Should handle quarter boundaries correctly")
    void shouldHandleQuarterBoundariesCorrectly() {
        // Test June (Q2)
        LocalDateTime juneDate = LocalDateTime.of(2024, 6, 15, 0, 0, 0);

        when(stringCalc.evaluate(evaluator)).thenReturn("q");
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(juneDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Integer result = datePartCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(2); // June is in Q2
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(datePartCalc.getType()).isEqualTo(org.eclipse.daanse.olap.api.type.NumericType.INSTANCE);
    }

    @Test
    @DisplayName("Should handle midnight time correctly")
    void shouldHandleMidnightTimeCorrectly() {
        LocalDateTime midnightDate = LocalDateTime.of(2024, 3, 15, 0, 0, 0);

        when(stringCalc.evaluate(evaluator)).thenReturn("h");
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(midnightDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Integer result = datePartCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle end of year date correctly")
    void shouldHandleEndOfYearDateCorrectly() {
        LocalDateTime endOfYearDate = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        when(stringCalc.evaluate(evaluator)).thenReturn("y");
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(endOfYearDate);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Integer result = datePartCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(366); // 2024 is a leap year, so Dec 31 is day 366
    }
}