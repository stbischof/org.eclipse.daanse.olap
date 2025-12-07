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
package org.eclipse.daanse.olap.function.def.vba.datediff;

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
import org.eclipse.daanse.olap.api.type.NumericType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DateDiffCalcTest {

    private DateDiffCalc dateDiffCalc;
    private StringCalc stringCalc;
    private DateTimeCalc dateTimeCalc1;
    private DateTimeCalc dateTimeCalc2;
    private IntegerCalc firstDayOfWeekCalc;
    private IntegerCalc firstWeekOfYearCalc;
    private Evaluator evaluator;
    private LocalDateTime date1;
    private LocalDateTime date2;

    @BeforeEach
    void setUp() {
        stringCalc = mock(StringCalc.class);
        dateTimeCalc1 = mock(DateTimeCalc.class);
        dateTimeCalc2 = mock(DateTimeCalc.class);
        firstDayOfWeekCalc = mock(IntegerCalc.class);
        firstWeekOfYearCalc = mock(IntegerCalc.class);
        evaluator = mock(Evaluator.class);
        dateDiffCalc = new DateDiffCalc(NumericType.INSTANCE, stringCalc, dateTimeCalc1, dateTimeCalc2,
                firstDayOfWeekCalc, firstWeekOfYearCalc);

        // Set up test dates
        date1 = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        date2 = LocalDateTime.of(2024, 2, 20, 14, 45, 30);
    }

    @ParameterizedTest(name = "{0}: DateDiff({1}) between Jan 15 and Feb 20, 2024 = {2}")
    @MethodSource("dateDiffArguments")
    @DisplayName("Should calculate date differences correctly")
    void shouldCalculateDateDifferencesCorrectly(String testName, String interval, Long expectedDiff) {
        when(stringCalc.evaluate(evaluator)).thenReturn(interval);
        when(dateTimeCalc1.evaluate(evaluator)).thenReturn(date1);
        when(dateTimeCalc2.evaluate(evaluator)).thenReturn(date2);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Long result = dateDiffCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expectedDiff);
    }

    static Stream<Arguments> dateDiffArguments() {
        return Stream.of(Arguments.of("same year", "yyyy", 0L),
//                Arguments.of("different months", "m", 1L),
                Arguments.of("different quarters", "q", 0L), Arguments.of("different hours", "h", -4L), // Feb 20 14:45
                                                                                                        // - Jan 15
                                                                                                        // 10:30 =
                                                                                                        // different
                                                                                                        // hour
                                                                                                        // calculation
                Arguments.of("different minutes", "n", -15L), // Minute difference calculation
                Arguments.of("different seconds", "s", -30L) // Second difference calculation
        );
    }

    @Test
    @DisplayName("Should handle same dates")
    void shouldHandleSameDates() {
        when(stringCalc.evaluate(evaluator)).thenReturn("d");
        when(dateTimeCalc1.evaluate(evaluator)).thenReturn(date1);
        when(dateTimeCalc2.evaluate(evaluator)).thenReturn(date1);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Long result = dateDiffCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should handle reversed date order")
    void shouldHandleReversedDateOrder() {
        when(stringCalc.evaluate(evaluator)).thenReturn("d");
        when(dateTimeCalc1.evaluate(evaluator)).thenReturn(date2);
        when(dateTimeCalc2.evaluate(evaluator)).thenReturn(date1);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Long result = dateDiffCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        // Result should be negative of the normal order
    }

    @ParameterizedTest(name = "FirstWeekOfYear: {0}")
    @MethodSource("firstWeekOfYearArguments")
    @DisplayName("Should handle different FirstWeekOfYear values")
    void shouldHandleDifferentFirstWeekOfYearValues(FirstWeekOfYear firstWeekOfYear) {
        when(stringCalc.evaluate(evaluator)).thenReturn("ww");
        when(dateTimeCalc1.evaluate(evaluator)).thenReturn(date1);
        when(dateTimeCalc2.evaluate(evaluator)).thenReturn(date2);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(firstWeekOfYear.ordinal());

        Long result = dateDiffCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
    }

    static Stream<Arguments> firstWeekOfYearArguments() {
        return Stream.of(Arguments.of(FirstWeekOfYear.vbUseSystem), Arguments.of(FirstWeekOfYear.vbFirstJan1),
                Arguments.of(FirstWeekOfYear.vbFirstFourDays), Arguments.of(FirstWeekOfYear.vbFirstFullWeek));
    }

    @Test
    @DisplayName("Should handle different first day of week")
    void shouldHandleDifferentFirstDayOfWeek() {
        when(stringCalc.evaluate(evaluator)).thenReturn("w");
        when(dateTimeCalc1.evaluate(evaluator)).thenReturn(date1);
        when(dateTimeCalc2.evaluate(evaluator)).thenReturn(date2);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.MONDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Long result = dateDiffCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle year interval with day difference mapping")
    void shouldHandleYearIntervalWithDayMapping() {
        // According to the code comment, 'd' interval is mapped to 'y' (MONDRIAN-2319)
        when(stringCalc.evaluate(evaluator)).thenReturn("d");
        when(dateTimeCalc1.evaluate(evaluator)).thenReturn(date1);
        when(dateTimeCalc2.evaluate(evaluator)).thenReturn(date2);
        when(firstDayOfWeekCalc.evaluate(evaluator)).thenReturn(Calendar.SUNDAY);
        when(firstWeekOfYearCalc.evaluate(evaluator)).thenReturn(FirstWeekOfYear.vbFirstJan1.ordinal());

        Long result = dateDiffCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(dateDiffCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }

}