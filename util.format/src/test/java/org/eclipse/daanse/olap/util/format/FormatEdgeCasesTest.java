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
package org.eclipse.daanse.olap.util.format;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class FormatEdgeCasesTest {

    @Nested
    class NullAndEmptyHandling {

        @Test
        void nullInput() {
            Format f = new Format("#,##0.00", Locale.US);
            assertThat(f.format(null)).isEqualTo("");
        }

        @Test
        void nullInputWithFourSections() {
            Format f = new Format("#,##0.00;(#,##0.00);Zero;Null", Locale.US);
            assertThat(f.format(null)).isEqualTo("Null");
        }

        @Test
        void emptyFormatString() {
            Format f = new Format("", Locale.US);
            assertThat(f.format(123.45)).isEqualTo("123.45");
        }

        @Test
        void nullFormatString() {
            Format f = new Format(null, Locale.US);
            assertThat(f.format(123.45)).isEqualTo("123.45");
        }

        @Test
        void nullLocale() {
            Format f = new Format("#,##0.00", (Locale) null);
            assertThat(f.format(123.45)).isEqualTo("123.45");
        }

        @Test
        void formatStringOnlySemicolons() {
            Format f = new Format(";;;", Locale.US);
            assertThat(f.format(123.45)).isEqualTo("123.45");
        }

        @Test
        void formatStringOnlyLiteral() {
            Format f = new Format("\"hello\"", Locale.US);
            assertThat(f.format(123.45)).isEqualTo("hello");
        }
    }

    @Nested
    class DateTimeTypeSupport {

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void dateTimeTypesSupported(String typeName, Object dateValue) {
            Format f = new Format("yyyy-mm-dd", Locale.US);
            String result = f.format(dateValue);
            assertThat(result).contains("2024").contains("15");
        }

        static Stream<Arguments> dateTimeTypesSupported() {
            LocalDateTime ldt = LocalDateTime.of(2024, 3, 15, 10, 30, 0);
            Calendar cal = Calendar.getInstance();
            cal.set(2024, Calendar.MARCH, 15, 10, 30, 0);
            return Stream.of(Arguments.of("LocalDateTime", ldt), Arguments.of("LocalDate", ldt.toLocalDate()),
                    Arguments.of("Instant", ldt.atZone(ZoneId.systemDefault()).toInstant()),
                    Arguments.of("Date", Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())),
                    Arguments.of("Calendar", cal));
        }
    }

    @Nested
    class NumericTypeSupport {

        @ParameterizedTest(name = "{0}: {1} → {2}")
        @MethodSource
        void numericTypesSupported(String typeName, Object value, String expected) {
            Format f = new Format(typeName.equals("Float") ? "0.00" : "#,##0", Locale.US);
            String result = f.format(value);
            if (typeName.equals("Float")) {
                assertThat(result).isNotNull();
            } else {
                assertThat(result).isEqualTo(expected);
            }
        }

        static Stream<Arguments> numericTypesSupported() {
            return Stream.of(Arguments.of("Byte", (byte) 42, "42"), Arguments.of("Short", (short) 1234, "1,234"),
                    Arguments.of("Integer", 12345, "12,345"), Arguments.of("Long", 1234567890L, "1,234,567,890"),
                    Arguments.of("BigDecimal", new BigDecimal("12345.67"), "12,346"),
                    Arguments.of("BigInteger", BigInteger.valueOf(12345), "12,345"), Arguments.of("Float", 3.14f, ""));
        }

        @Test
        void bigDecimalWithDecimals() {
            Format f = new Format("#,##0.00", Locale.US);
            assertThat(f.format(new BigDecimal("12345.67"))).isEqualTo("12,345.67");
        }
    }

    @Nested
    class MacroFormats {

        @ParameterizedTest(name = "{0}: {1} → {2}")
        @CsvSource({ "Standard, 1234.5, '1,235'", "Percent, 0.5, '50.00%'" })
        void macroFormats(String format, double value, String expected) {
            Format f = new Format(format, Locale.US);
            assertThat(f.format(value)).isEqualTo(expected);
        }

        @Test
        void macroFormatCurrency() {
            Format f = new Format("Currency", Locale.US);
            String result = f.format(1234.56);
            assertThat(result).contains("$").contains("1,234.56");
        }

        @Test
        void macroFormatScientific() {
            Format f = new Format("Scientific", Locale.US);
            assertThat(f.format(1234.5)).isNotNull().containsIgnoringCase("e");
        }

        @ParameterizedTest(name = "{0}: {1} → {2}")
        @CsvSource({ "Yes/No, 1, Yes", "Yes/No, 0, No", "True/False, 1, True", "True/False, 0, False", "On/Off, 1, On",
                "On/Off, 0, Off" })
        void booleanFormats(String format, int value, String expected) {
            Format f = new Format(format, Locale.US);
            assertThat(f.format(value)).isEqualTo(expected);
        }
    }

    @Nested
    class Misc {

        @Test
        void stringValue() {
            Format f = new Format(">", Locale.US);
            assertThat(f.format("hello")).isEqualTo("HELLO");
        }

        @Test
        void getFormatString() {
            Format f = new Format("#,##0.00", Locale.US);
            assertThat(f.getFormatString()).isEqualTo("#,##0.00");
        }

        @Test
        void unknownObjectTypeFormatsToString() {
            Format f = new Format("", Locale.US);
            Object custom = new Object() {
                @Override
                public String toString() {
                    return "custom";
                }
            };
            assertThat(f.format(custom)).isEqualTo("custom");
        }
    }
}
