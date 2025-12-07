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
package org.eclipse.daanse.olap.function.def.vba.year;

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

class YearCalcTest {

    private YearCalc yearCalc;
    private DateTimeCalc dateTimeCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        dateTimeCalc = mock(DateTimeCalc.class);
        evaluator = mock(Evaluator.class);
        yearCalc = new YearCalc(NumericType.INSTANCE, dateTimeCalc);
    }

    @ParameterizedTest(name = "{0}: year({1}) = {2}")
    @MethodSource("arguments")
    @DisplayName("Should extract year from date correctly")
    void shouldExtractYear(String testName, LocalDateTime inputDate, Integer expected) {
        when(dateTimeCalc.evaluate(evaluator)).thenReturn(inputDate);

        Integer result = yearCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of("Y2K year", LocalDateTime.of(2000, 1, 1, 0, 0, 0), 2000),
                Arguments.of("recent year", LocalDateTime.of(2023, 7, 15, 12, 30, 45), 2023),
                Arguments.of("leap year 2024", LocalDateTime.of(2024, 2, 29, 6, 15, 30), 2024),
                Arguments.of("pre-Y2K year", LocalDateTime.of(1999, 12, 31, 23, 59, 59), 1999),
                Arguments.of("Unix epoch year", LocalDateTime.of(1970, 1, 1, 0, 0, 0), 1970),
                Arguments.of("future year", LocalDateTime.of(2025, 6, 10, 18, 0, 0), 2025));
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(yearCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }
}