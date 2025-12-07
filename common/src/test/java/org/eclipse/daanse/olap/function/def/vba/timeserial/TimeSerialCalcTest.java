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
package org.eclipse.daanse.olap.function.def.vba.timeserial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

class TimeSerialCalcTest {

    private TimeSerialCalc timeSerialCalc;
    private IntegerCalc hourCalc;
    private IntegerCalc minuteCalc;
    private IntegerCalc secondCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        hourCalc = mock(IntegerCalc.class);
        minuteCalc = mock(IntegerCalc.class);
        secondCalc = mock(IntegerCalc.class);
        evaluator = mock(Evaluator.class);
        timeSerialCalc = new TimeSerialCalc(DateTimeType.INSTANCE, hourCalc, minuteCalc, secondCalc);
    }

    @ParameterizedTest(name = "{0}: TimeSerial({1}, {2}, {3})")
    @MethodSource("timeSerialArguments")
    @DisplayName("Should create time correctly")
    void shouldCreateTimeCorrectly(String testName, Integer hour, Integer minute, Integer second,
            Integer expectedHour, Integer expectedMinute, Integer expectedSecond) {
        when(hourCalc.evaluate(evaluator)).thenReturn(hour);
        when(minuteCalc.evaluate(evaluator)).thenReturn(minute);
        when(secondCalc.evaluate(evaluator)).thenReturn(second);

        LocalDateTime result = timeSerialCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(expectedHour);
        assertThat(result.getMinute()).isEqualTo(expectedMinute);
        assertThat(result.getSecond()).isEqualTo(expectedSecond);
    }

    static Stream<Arguments> timeSerialArguments() {
        return Stream.of(
                Arguments.of("midnight", 0, 0, 0, 0, 0, 0),
                Arguments.of("noon", 12, 0, 0, 12, 0, 0),
                Arguments.of("morning time", 9, 30, 45, 9, 30, 45),
                Arguments.of("evening time", 18, 15, 30, 18, 15, 30),
                Arguments.of("late night", 23, 59, 59, 23, 59, 59));
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        when(hourCalc.evaluate(evaluator)).thenReturn(null);
        when(minuteCalc.evaluate(evaluator)).thenReturn(30);
        when(secondCalc.evaluate(evaluator)).thenReturn(0);

        assertThatThrownBy(() -> timeSerialCalc.evaluate(evaluator)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(timeSerialCalc.getType()).isEqualTo(DateTimeType.INSTANCE);
    }

    @Test
    @DisplayName("Should create time with epoch date")
    void shouldCreateTimeWithEpochDate() {
        when(hourCalc.evaluate(evaluator)).thenReturn(15);
        when(minuteCalc.evaluate(evaluator)).thenReturn(30);
        when(secondCalc.evaluate(evaluator)).thenReturn(45);

        LocalDateTime result = timeSerialCalc.evaluate(evaluator);

        assertThat(result.getHour()).isEqualTo(15);
        assertThat(result.getMinute()).isEqualTo(30);
        assertThat(result.getSecond()).isEqualTo(45);
        // Date components should be epoch date (January 1, 1970)
        assertThat(result.getYear()).isEqualTo(1970);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should create consistent results")
    void shouldCreateConsistentResults() {
        when(hourCalc.evaluate(evaluator)).thenReturn(14);
        when(minuteCalc.evaluate(evaluator)).thenReturn(30);
        when(secondCalc.evaluate(evaluator)).thenReturn(15);

        LocalDateTime result1 = timeSerialCalc.evaluate(evaluator);
        LocalDateTime result2 = timeSerialCalc.evaluate(evaluator);

        assertThat(result1).isEqualTo(result2);
    }
}
