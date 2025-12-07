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
package org.eclipse.daanse.olap.function.def.vba.formatdatetime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FormatDateTimeCalcTest {

    private FormatDateTimeCalc formatDateTimeCalc;
    private DateTimeCalc dateCalc;
    private IntegerCalc namedFormatCalc;
    private Evaluator evaluator;
    private LocalDateTime testDate;

    @BeforeEach
    void setUp() {
        dateCalc = mock(DateTimeCalc.class);
        namedFormatCalc = mock(IntegerCalc.class);
        evaluator = mock(Evaluator.class);
        formatDateTimeCalc = new FormatDateTimeCalc(StringType.INSTANCE, dateCalc, namedFormatCalc);

        // Create a test date: June 15, 2023, 14:30:45
        testDate = LocalDateTime.of(2023, 6, 15, 14, 30, 45);
    }

    @ParameterizedTest(name = "{0}: formatDateTime(date, {1})")
    @MethodSource("formatArguments")
    @DisplayName("Should format date/time with different formats")
    void shouldFormatDateTime(String testName, Integer format) {
        when(dateCalc.evaluate(evaluator)).thenReturn(testDate);
        when(namedFormatCalc.evaluate(evaluator)).thenReturn(format);

        String result = formatDateTimeCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        // Basic validation that result contains some expected elements
        if (format == 1 || format == 2 || format == 0) {
            // Date formats should contain year info
            assertThat(result).containsAnyOf("2023", "23");
        }
        if (format == 3 || format == 4 || format == 0) {
            // Time formats should contain hour/minute info
            assertThat(result).matches(".*\\d{1,2}.*\\d{2}.*");
        }
    }

    static Stream<Arguments> formatArguments() {
        return Stream.of(Arguments.of("general date", 0), Arguments.of("long date", 1), Arguments.of("short date", 2),
                Arguments.of("long time", 3), Arguments.of("short time", 4),
                Arguments.of("invalid format uses default", 99));
    }

    @Test
    @DisplayName("Should test static formatDateTime method directly")
    void shouldTestStaticMethod() {
        String result = FormatDateTimeCalc.formatDateTime(testDate, 0);
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle different date formats")
    void shouldHandleDifferentFormats() {
        // Test long date format
        String longDate = FormatDateTimeCalc.formatDateTime(testDate, 1);
        assertThat(longDate).isNotNull();

        // Test short date format
        String shortDate = FormatDateTimeCalc.formatDateTime(testDate, 2);
        assertThat(shortDate).isNotNull();

        // Test long time format
        String longTime = FormatDateTimeCalc.formatDateTime(testDate, 3);
        assertThat(longTime).isNotNull();

        // Test short time format
        String shortTime = FormatDateTimeCalc.formatDateTime(testDate, 4);
        assertThat(shortTime).isNotNull();

        // Different formats should produce different results
        assertThat(longDate).isNotEqualTo(shortDate);
        assertThat(longTime).isNotEqualTo(shortTime);
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(formatDateTimeCalc.getType()).isEqualTo(StringType.INSTANCE);
    }

    @Test
    @DisplayName("Should handle null date")
    @Disabled
    void shouldHandleNullDate() {
        when(dateCalc.evaluate(evaluator)).thenReturn(null);
        when(namedFormatCalc.evaluate(evaluator)).thenReturn(0);

        String result = formatDateTimeCalc.evaluate(evaluator);

        assertThat(result).isEqualTo("null");
    }

    @Test
    @DisplayName("Should handle edge dates")
    void shouldHandleEdgeDates() {
        // Test with epoch date
        LocalDateTime epochDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        String result = FormatDateTimeCalc.formatDateTime(epochDate, 0);
        assertThat(result).isNotNull();

        // Test with current date
        LocalDateTime currentDate = LocalDateTime.now();
        result = FormatDateTimeCalc.formatDateTime(currentDate, 0);
        assertThat(result).isNotNull();
    }
}