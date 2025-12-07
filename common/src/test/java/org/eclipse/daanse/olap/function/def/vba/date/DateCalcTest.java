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
package org.eclipse.daanse.olap.function.def.vba.date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.DateTimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DateCalcTest {

    private DateCalc dateCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = mock(Evaluator.class);
        dateCalc = new DateCalc(DateTimeType.INSTANCE);
    }

    @Test
    @DisplayName("Should return current date with time set to midnight")
    void shouldReturnCurrentDateAtMidnight() {
        LocalDateTime result = dateCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(0);
        assertThat(result.getMinute()).isEqualTo(0);
        assertThat(result.getSecond()).isEqualTo(0);
        assertThat(result.getNano()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return today's date")
    @Disabled
    void shouldReturnTodaysDate() {
        LocalDateTime result = dateCalc.evaluate(evaluator);

        LocalDate today = LocalDate.now();
        LocalDate resultDate = result.toLocalDate();

        assertThat(resultDate).isEqualTo(today);
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(dateCalc.getType()).isEqualTo(DateTimeType.INSTANCE);
    }

    @Test
    @DisplayName("Should be consistent across multiple calls")
    void shouldBeConsistentAcrossMultipleCalls() {
        LocalDateTime first = dateCalc.evaluate(evaluator);
        LocalDateTime second = dateCalc.evaluate(evaluator);

        // Both should represent the same day
        assertThat(first.getYear()).isEqualTo(second.getYear());
        assertThat(first.getMonth()).isEqualTo(second.getMonth());
        assertThat(first.getDayOfMonth()).isEqualTo(second.getDayOfMonth());
        assertThat(first.getHour()).isEqualTo(0);
        assertThat(second.getHour()).isEqualTo(0);
    }
}
