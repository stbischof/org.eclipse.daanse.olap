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
package org.eclipse.daanse.olap.function.def.vba.datevalue;

import static org.assertj.core.api.Assertions.assertThat;
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

class DateValueCalcTest {

    private DateValueCalc dateValueCalc;
    private DateTimeCalc dateCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        dateCalc = mock(DateTimeCalc.class);
        evaluator = mock(Evaluator.class);
        dateValueCalc = new DateValueCalc(DateTimeType.INSTANCE, dateCalc);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dateValueArguments")
    @DisplayName("Should strip time components and return date only")
    void shouldStripTimeComponentsAndReturnDateOnly(String testName, LocalDateTime inputDate,
            int expectedYear, int expectedMonth, int expectedDay) {
        when(dateCalc.evaluate(evaluator)).thenReturn(inputDate);

        LocalDateTime result = dateValueCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(expectedYear);
        assertThat(result.getMonthValue()).isEqualTo(expectedMonth);
        assertThat(result.getDayOfMonth()).isEqualTo(expectedDay);
        // Time components should be set to midnight
        assertThat(result.getHour()).isEqualTo(0);
        assertThat(result.getMinute()).isEqualTo(0);
        assertThat(result.getSecond()).isEqualTo(0);
        assertThat(result.getNano()).isEqualTo(0);
    }

    static Stream<Arguments> dateValueArguments() {
        return Stream.of(
                Arguments.of("datetime with time", LocalDateTime.of(2024, 3, 15, 14, 30, 45), 2024, 3, 15),
                Arguments.of("end of day", LocalDateTime.of(2024, 12, 31, 23, 59, 59), 2024, 12, 31),
                Arguments.of("already midnight", LocalDateTime.of(2024, 1, 1, 0, 0, 0), 2024, 1, 1),
                Arguments.of("leap year", LocalDateTime.of(2024, 2, 29, 12, 30, 45), 2024, 2, 29));
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(dateValueCalc.getType()).isEqualTo(DateTimeType.INSTANCE);
    }

    @Test
    @DisplayName("Should be consistent across multiple calls")
    void shouldBeConsistentAcrossMultipleCalls() {
        LocalDateTime inputDate = LocalDateTime.of(2024, 3, 15, 14, 30, 45);
        when(dateCalc.evaluate(evaluator)).thenReturn(inputDate);

        LocalDateTime first = dateValueCalc.evaluate(evaluator);
        LocalDateTime second = dateValueCalc.evaluate(evaluator);

        assertThat(first).isEqualTo(second);
    }
}
