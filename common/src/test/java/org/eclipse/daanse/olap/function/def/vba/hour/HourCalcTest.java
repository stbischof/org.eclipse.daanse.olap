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
package org.eclipse.daanse.olap.function.def.vba.hour;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HourCalcTest {

    private HourCalc hourCalc;
    private DateTimeCalc dateTimeCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        dateTimeCalc = mock(DateTimeCalc.class);
        evaluator = mock(Evaluator.class);
        hourCalc = new HourCalc(NumericType.INSTANCE, dateTimeCalc);
    }

    @ParameterizedTest(name = "{0}: hour({1}) = {2}")
    @MethodSource("arguments")
    @DisplayName("Should extract hour from datetime correctly")
    void shouldExtractHour(String testName, LocalDateTime inputDate, Integer expected) {
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(inputDate);

        Integer result = hourCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of("midnight", LocalDateTime.of(2023, 1, 1, 0, 0, 0), 0),
                Arguments.of("early morning", LocalDateTime.of(2023, 6, 15, 6, 30, 45), 6),
                Arguments.of("noon", LocalDateTime.of(2023, 3, 20, 12, 0, 0), 12),
                Arguments.of("afternoon", LocalDateTime.of(2023, 9, 10, 15, 45, 30), 15),
                Arguments.of("evening", LocalDateTime.of(2023, 12, 25, 18, 20, 10), 18),
                Arguments.of("late night", LocalDateTime.of(2023, 7, 4, 23, 59, 59), 23));
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(hourCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}
