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
package org.eclipse.daanse.olap.function.def.vba.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.DateTimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimeCalcTest {

    private TimeCalc timeCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = mock(Evaluator.class);
        timeCalc = new TimeCalc(DateTimeType.INSTANCE);
    }

    @Test
    @DisplayName("Should return current time as LocalDateTime")
    void shouldReturnCurrentTimeAsDate() {
        LocalDateTime before = LocalDateTime.now();

        LocalDateTime result = timeCalc.evaluate(evaluator);

        LocalDateTime after = LocalDateTime.now();

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(LocalDateTime.class);

        // Result should be between before and after timestamps
        assertThat(result).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    @DisplayName("Should return different times on consecutive calls")
    void shouldReturnDifferentTimesOnConsecutiveCalls() throws Exception {
        LocalDateTime result1 = timeCalc.evaluate(evaluator);

        // Sleep for a small amount to ensure time difference
        Thread.sleep(10);

        LocalDateTime result2 = timeCalc.evaluate(evaluator);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();

        // Second call should return a later or equal time
        assertThat(result2).isAfterOrEqualTo(result1);
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(timeCalc.getType()).isEqualTo(DateTimeType.INSTANCE);
    }

    @Test
    @DisplayName("Should return time close to system current time")
    void shouldReturnTimeCloseToSystemCurrentTime() {
        LocalDateTime systemTimeBefore = LocalDateTime.now();

        LocalDateTime result = timeCalc.evaluate(evaluator);

        LocalDateTime systemTimeAfter = LocalDateTime.now();

        // Result should be within the time window of the test execution
        assertThat(result).isBetween(systemTimeBefore, systemTimeAfter.plusNanos(100_000_000));
    }

    @Test
    @DisplayName("Should consistently return LocalDateTime objects")
    void shouldConsistentlyReturnDateObjects() {
        for (int i = 0; i < 10; i++) {
            LocalDateTime result = timeCalc.evaluate(evaluator);
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(LocalDateTime.class);
        }
    }

    @Test
    @DisplayName("Should return time that is reasonable recent")
    void shouldReturnTimeThatIsReasonableRecent() {
        LocalDateTime result = timeCalc.evaluate(evaluator);
        LocalDateTime now = LocalDateTime.now();

        // The returned time should be very close to "now"
        long timeDifference = Math.abs(ChronoUnit.MILLIS.between(result, now));

        // Should be within 5 seconds (very generous for test execution)
        assertThat(timeDifference).isLessThan(5000L);
    }

    @Test
    @DisplayName("Should return time after Unix epoch")
    void shouldReturnTimeAfterUnixEpoch() {
        LocalDateTime result = timeCalc.evaluate(evaluator);

        // Should be after year 2000 (reasonable for current system)
        LocalDateTime year2000 = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        assertThat(result).isAfter(year2000);
    }

    @Test
    @DisplayName("Should handle multiple evaluations efficiently")
    void shouldHandleMultipleEvaluationsEfficiently() {
        long startTime = System.currentTimeMillis();

        // Perform multiple evaluations
        for (int i = 0; i < 100; i++) {
            LocalDateTime result = timeCalc.evaluate(evaluator);
            assertThat(result).isNotNull();
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Should complete quickly (less than 1 second for 100 calls)
        assertThat(totalTime).isLessThan(1000L);
    }

    @Test
    @DisplayName("Should return time that can be formatted")
    void shouldReturnTimeThatCanBeFormatted() {
        LocalDateTime result = timeCalc.evaluate(evaluator);

        // Should be able to format the date
        String formatted = result.toString();
        assertThat(formatted).isNotNull();
        assertThat(formatted).isNotEmpty();
    }

    @Test
    @DisplayName("Should verify Time function behavior matches VBA")
    void shouldVerifyTimeFunctionBehaviorMatchesVBA() {
        // In VBA, Time function returns the current system time
        LocalDateTime vbaTimeEquivalent = LocalDateTime.now();
        LocalDateTime result = timeCalc.evaluate(evaluator);

        // Both should be very close to each other
        long timeDifference = Math.abs(ChronoUnit.MILLIS.between(result, vbaTimeEquivalent));
        assertThat(timeDifference).isLessThan(1000L); // Within 1 second
    }
}
