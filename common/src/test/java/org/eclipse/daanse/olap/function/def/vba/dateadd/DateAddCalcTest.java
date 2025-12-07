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
package org.eclipse.daanse.olap.function.def.vba.dateadd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.DateTimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DateAddCalcTest {

    private DateAddCalc dateAddCalc;
    private StringCalc stringCalc;
    private DoubleCalc doubleCalc;
    private DateTimeCalc dateTimeCalc;
    private Evaluator evaluator;
    private LocalDateTime baseDate;

    @BeforeEach
    void setUp() {
        stringCalc = mock(StringCalc.class);
        doubleCalc = mock(DoubleCalc.class);
        dateTimeCalc = mock(DateTimeCalc.class);
        evaluator = mock(Evaluator.class);
        dateAddCalc = new DateAddCalc(DateTimeType.INSTANCE, stringCalc, doubleCalc, dateTimeCalc);

        // Set up base date: Jan 15, 2024 12:00:00
        baseDate = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
    }

    @ParameterizedTest(name = "{0}: DateAdd({1}, {2}, baseDate) should add {3}")
    @MethodSource("dateAddArguments")
    @DisplayName("Should add intervals correctly")
    void shouldAddIntervalsCorrectly(String testName, String interval, Double amount, String expectedDescription) {
        when(stringCalc.evaluate(evaluator)).thenReturn(interval);
        when(doubleCalc.evaluate(evaluator)).thenReturn(amount);
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(baseDate);

        LocalDateTime result = dateAddCalc.evaluate(evaluator);

        assertThat(result).isNotNull();

        // Apply expected transformation based on interval
        LocalDateTime expected = switch (interval) {
            case "yyyy" -> baseDate.plusYears(amount.intValue());
            case "q" -> baseDate.plusMonths(amount.intValue() * 3L);
            case "m" -> baseDate.plusMonths(amount.intValue());
            case "d", "y" -> baseDate.plusDays(amount.intValue());
            case "h" -> baseDate.plusHours(amount.intValue());
            case "n" -> baseDate.plusMinutes(amount.intValue());
            case "s" -> baseDate.plusSeconds(amount.intValue());
            default -> baseDate;
        };

        // 1 second tolerance
        assertThat(Duration.between(expected, result).abs().getSeconds()).isLessThanOrEqualTo(1);
    }

    static Stream<Arguments> dateAddArguments() {
        return Stream.of(Arguments.of("add years", "yyyy", 2.0, "2 years"),
                Arguments.of("add quarters", "q", 1.0, "3 months"), Arguments.of("add months", "m", 6.0, "6 months"),
                Arguments.of("add days", "d", 10.0, "10 days"), Arguments.of("add day of year", "y", 5.0, "5 days"),
                Arguments.of("add hours", "h", 3.0, "3 hours"), Arguments.of("add minutes", "n", 30.0, "30 minutes"),
                Arguments.of("add seconds", "s", 45.0, "45 seconds"),
                Arguments.of("subtract years", "yyyy", -1.0, "subtract 1 year"),
                Arguments.of("subtract months", "m", -3.0, "subtract 3 months"),
                Arguments.of("zero addition", "d", 0.0, "no change"));
    }

    @ParameterizedTest(name = "{0}: DateAdd({1}, {2}, baseDate) with fractional amounts")
    @MethodSource("fractionalAmountArguments")
    @DisplayName("Should handle fractional amounts correctly")
    void shouldHandleFractionalAmountsCorrectly(String testName, String interval, Double amount,
            long expectedMillisDiff) {
        when(stringCalc.evaluate(evaluator)).thenReturn(interval);
        when(doubleCalc.evaluate(evaluator)).thenReturn(amount);
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(baseDate);

        LocalDateTime result = dateAddCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        long actualDiff = Duration.between(baseDate, result).toMillis();
        assertThat(actualDiff).isCloseTo(expectedMillisDiff, within(1000L)); // 1 second tolerance
    }

    static Stream<Arguments> fractionalAmountArguments() {
        return Stream.of(Arguments.of("half day", "d", 0.5, 12 * 60 * 60 * 1000L),
                Arguments.of("quarter day", "d", 0.25, 6 * 60 * 60 * 1000L),
                Arguments.of("one and half hours", "h", 1.5, 90 * 60 * 1000L),
                Arguments.of("half minute", "n", 0.5, 30 * 1000L));
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(dateAddCalc.getType()).isEqualTo(DateTimeType.INSTANCE);
    }

    @Test
    @DisplayName("Should handle large amounts correctly")
    void shouldHandleLargeAmountsCorrectly() {
        when(stringCalc.evaluate(evaluator)).thenReturn("yyyy");
        when(doubleCalc.evaluate(evaluator)).thenReturn(100.0);
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(baseDate);

        LocalDateTime result = dateAddCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(baseDate.getYear() + 100);
    }

    @Test
    @DisplayName("Should handle week interval correctly")
    void shouldHandleWeekIntervalCorrectly() {
        when(stringCalc.evaluate(evaluator)).thenReturn("w");
        when(doubleCalc.evaluate(evaluator)).thenReturn(2.0);
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(baseDate);

        LocalDateTime result = dateAddCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        long expectedDiff = 2 * 24 * 60 * 60 * 1000L; // 2 days in milliseconds
        long actualDiff = Duration.between(baseDate, result).toMillis();
        assertThat(actualDiff).isCloseTo(expectedDiff, within(1000L));
    }
}