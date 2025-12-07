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
package org.eclipse.daanse.olap.function.def.vba.month;

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

class MonthCalcTest {

    private MonthCalc monthCalc;
    private DateTimeCalc dateTimeCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        dateTimeCalc = mock(DateTimeCalc.class);
        evaluator = mock(Evaluator.class);
        monthCalc = new MonthCalc(NumericType.INSTANCE, dateTimeCalc);
    }

    @ParameterizedTest(name = "{0}: month({1}) = {2}")
    @MethodSource("arguments")
    @DisplayName("Should extract month from date correctly")
    void shouldExtractMonth(String testName, LocalDateTime inputDate, Integer expected) {
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(inputDate);

        Integer result = monthCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of("January", LocalDateTime.of(2023, 1, 15, 10, 30, 0), 1),
                Arguments.of("February", LocalDateTime.of(2023, 2, 28, 0, 0, 0), 2),
                Arguments.of("March", LocalDateTime.of(2024, 3, 1, 12, 0, 0), 3),
                Arguments.of("June", LocalDateTime.of(2023, 6, 15, 18, 45, 30), 6),
                Arguments.of("September", LocalDateTime.of(2023, 9, 30, 23, 59, 59), 9),
                Arguments.of("December", LocalDateTime.of(2025, 12, 31, 6, 0, 0), 12));
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(monthCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}
