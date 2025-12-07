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
package org.eclipse.daanse.olap.function.def.vba.dateserial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.DateTimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DateSerialCalcTest {

    private DateSerialCalc dateSerialCalc;
    private IntegerCalc yearCalc;
    private IntegerCalc monthCalc;
    private IntegerCalc dayCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        yearCalc = mock(IntegerCalc.class);
        monthCalc = mock(IntegerCalc.class);
        dayCalc = mock(IntegerCalc.class);
        evaluator = mock(Evaluator.class);
        dateSerialCalc = new DateSerialCalc(DateTimeType.INSTANCE, yearCalc, monthCalc, dayCalc);
    }

    @ParameterizedTest(name = "{0}: DateSerial({1}, {2}, {3})")
    @MethodSource("dateSerialArguments")
    @DisplayName("Should create dates correctly from year, month, day")
    void shouldCreateDatesCorrectly(String testName, Integer year, Integer month, Integer day) {
        when(yearCalc.evaluate(evaluator)).thenReturn(year);
        when(monthCalc.evaluate(evaluator)).thenReturn(month);
        when(dayCalc.evaluate(evaluator)).thenReturn(day);

        LocalDateTime result = dateSerialCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(year);
        assertThat(result.getMonthValue()).isEqualTo(month);
        assertThat(result.getDayOfMonth()).isEqualTo(day);
    }

    static Stream<Arguments> dateSerialArguments() {
        return Stream.of(
                Arguments.of("standard date", 2024, 3, 15),
                Arguments.of("new year", 2024, 1, 1),
                Arguments.of("end of year", 2024, 12, 31),
                Arguments.of("leap year feb 29", 2024, 2, 29),
                Arguments.of("early date", 1900, 1, 1),
                Arguments.of("future date", 2050, 6, 15));
    }

    @Test
    @DisplayName("Should create date with time set to midnight")
    void shouldCreateDateWithTimeAtMidnight() {
        when(yearCalc.evaluate(evaluator)).thenReturn(2024);
        when(monthCalc.evaluate(evaluator)).thenReturn(3);
        when(dayCalc.evaluate(evaluator)).thenReturn(15);

        LocalDateTime result = dateSerialCalc.evaluate(evaluator);

        assertThat(result.getHour()).isEqualTo(0);
        assertThat(result.getMinute()).isEqualTo(0);
        assertThat(result.getSecond()).isEqualTo(0);
        assertThat(result.getNano()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(dateSerialCalc.getType()).isEqualTo(DateTimeType.INSTANCE);
    }

    @Test
    @DisplayName("Should be consistent across multiple calls")
    void shouldBeConsistentAcrossMultipleCalls() {
        when(yearCalc.evaluate(evaluator)).thenReturn(2024);
        when(monthCalc.evaluate(evaluator)).thenReturn(3);
        when(dayCalc.evaluate(evaluator)).thenReturn(15);

        LocalDateTime first = dateSerialCalc.evaluate(evaluator);
        LocalDateTime second = dateSerialCalc.evaluate(evaluator);

        assertThat(first).isEqualTo(second);
    }
}
