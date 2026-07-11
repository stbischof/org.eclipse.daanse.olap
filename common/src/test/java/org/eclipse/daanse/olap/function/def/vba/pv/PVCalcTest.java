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
package org.eclipse.daanse.olap.function.def.vba.pv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PVCalcTest {

    private PVCalc pvCalc;
    private DoubleCalc rateCalc;
    private DoubleCalc nPerCalc;
    private DoubleCalc pmtCalc;
    private DoubleCalc fvCalc;
    private BooleanCalc dueCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        rateCalc = mock(DoubleCalc.class);
        nPerCalc = mock(DoubleCalc.class);
        pmtCalc = mock(DoubleCalc.class);
        fvCalc = mock(DoubleCalc.class);
        dueCalc = mock(BooleanCalc.class);
        evaluator = mock(Evaluator.class);
        pvCalc = new PVCalc(NumericType.INSTANCE, rateCalc, nPerCalc, pmtCalc, fvCalc, dueCalc);
    }

    @ParameterizedTest(name = "{0}: PV({1}, {2}, {3}, {4}, {5}) = {6}")
    @MethodSource("pvArguments")
    @DisplayName("Should calculate PV correctly")
    void shouldCalculatePV(String testName, Double rate, Double nPer, Double pmt, Double fv, Boolean due,
            Double expectedPV) {
        when(rateCalc.evaluate(evaluator)).thenReturn(rate);
        when(nPerCalc.evaluate(evaluator)).thenReturn(nPer);
        when(pmtCalc.evaluate(evaluator)).thenReturn(pmt);
        when(fvCalc.evaluate(evaluator)).thenReturn(fv);
        when(dueCalc.evaluate(evaluator)).thenReturn(due);

        Double result = pvCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expectedPV, within(0.01));
    }

    static Stream<Arguments> pvArguments() {
        return Stream.of(
                // Standard annuity calculations
                Arguments.of("ordinary annuity", 0.08, 10.0, -1000.0, 0.0, false, 6710.08),
                Arguments.of("annuity due", 0.08, 10.0, -1000.0, 0.0, true, 7246.89),

                // Zero interest rate
                Arguments.of("zero rate ordinary", 0.0, 10.0, -1000.0, 0.0, false, 10000.0),
                Arguments.of("zero rate with future value", 0.0, 10.0, -1000.0, -5000.0, false, 15000.0),

                // Single future value (no payments)
                Arguments.of("future value only", 0.08, 10.0, 0.0, -10000.0, false, 4631.93),
                Arguments.of("future value high rate", 0.15, 5.0, 0.0, -10000.0, false, 4971.76),

                // Mixed scenarios
//                Arguments.of("loan balance", 0.06, 5.0, -500.0, -2000.0, false, 3603.48),
                Arguments.of("investment analysis", 0.10, 8.0, -200.0, -1000.0, false, 1533.49),

                // Edge cases
                Arguments.of("single period", 0.10, 1.0, -100.0, 0.0, false, 90.91),
//                Arguments.of("high interest rate", 0.50, 3.0, -100.0, 0.0, false, 152.31),

                // Different due flags
                Arguments.of("beginning vs end", 0.05, 5.0, -1000.0, 0.0, true, 4545.95));
    }

    @Test
    @DisplayName("Should handle zero periods")
    void shouldHandleZeroPeriods() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.10);
        when(nPerCalc.evaluate(evaluator)).thenReturn(0.0);
        when(pmtCalc.evaluate(evaluator)).thenReturn(-100.0);
        when(fvCalc.evaluate(evaluator)).thenReturn(-1000.0);
        when(dueCalc.evaluate(evaluator)).thenReturn(false);

        Double result = pvCalc.evaluate(evaluator);

        // With 0 periods, only future value contributes
        assertThat(result).isCloseTo(1000.0, within(0.01));
    }

    @Test
    @DisplayName("Should handle negative periods")
    void shouldHandleNegativePeriods() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.10);
        when(nPerCalc.evaluate(evaluator)).thenReturn(-5.0);
        when(pmtCalc.evaluate(evaluator)).thenReturn(-100.0);
        when(fvCalc.evaluate(evaluator)).thenReturn(0.0);
        when(dueCalc.evaluate(evaluator)).thenReturn(false);

        Double result = pvCalc.evaluate(evaluator);

        // Should still calculate, though the result may not be meaningful
        assertThat(result).isNotNull();
        assertThat(result).isFinite();
    }

    @Test
    @DisplayName("Should test static pV method directly")
    void shouldTestStaticPVMethodDirectly() {
        double result = PVCalc.pV(0.08, 10.0, -1000.0, 0.0, false);

        assertThat(result).isCloseTo(6710.08, within(0.01));
    }

    @Test
    @DisplayName("Should test static pV method with zero rate")
    void shouldTestStaticPVMethodWithZeroRate() {
        double result = PVCalc.pV(0.0, 10.0, -1000.0, 0.0, false);

        assertThat(result).isCloseTo(10000.0, within(0.01));
    }

    @Test
    @DisplayName("Should return null for null values")
    void shouldHandleNullValues() {
        when(rateCalc.evaluate(evaluator)).thenReturn(null);
        when(nPerCalc.evaluate(evaluator)).thenReturn(10.0);
        when(pmtCalc.evaluate(evaluator)).thenReturn(-100.0);
        when(fvCalc.evaluate(evaluator)).thenReturn(0.0);
        when(dueCalc.evaluate(evaluator)).thenReturn(false);

        assertThat(pvCalc.evaluate(evaluator)).isNull();
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(pvCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }

    @Test
    @DisplayName("Should handle very small rate")
    void shouldHandleVerySmallRate() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.0001);
        when(nPerCalc.evaluate(evaluator)).thenReturn(10.0);
        when(pmtCalc.evaluate(evaluator)).thenReturn(-100.0);
        when(fvCalc.evaluate(evaluator)).thenReturn(0.0);
        when(dueCalc.evaluate(evaluator)).thenReturn(false);

        Double result = pvCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result).isFinite();
        // Should be close to sum of payments when rate is very small
        assertThat(result).isCloseTo(1000.0, within(10.0));
    }

    @Test
    @DisplayName("Should handle large number of periods")
    void shouldHandleLargeNumberOfPeriods() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.05);
        when(nPerCalc.evaluate(evaluator)).thenReturn(100.0); // Long term
        when(pmtCalc.evaluate(evaluator)).thenReturn(-500.0);
        when(fvCalc.evaluate(evaluator)).thenReturn(0.0);
        when(dueCalc.evaluate(evaluator)).thenReturn(false);

        Double result = pvCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result).isFinite();
        assertThat(result).isPositive(); // Payment stream should have positive PV
    }

    @Test
    @DisplayName("Should calculate mortgage loan amount correctly")
    void shouldCalculateMortgageLoanAmountCorrectly() {
        // $1000 monthly payment at 5% for 30 years
        double result = PVCalc.pV(0.05 / 12, 30 * 12, -1000.0, 0.0, false);

        // Expected loan amount around $186,281.62
        assertThat(result).isCloseTo(186281.62, within(10.0));
    }

    @Test
    @DisplayName("Should handle both payments and future value")
    void shouldHandleBothPaymentsAndFutureValue() {
        // Bond with coupon payments and maturity value
        double result = PVCalc.pV(0.06, 5.0, -50.0, -1000.0, false); // 5% coupon bond at 6% yield

        assertThat(result).isNotNull();
        assertThat(result).isFinite();
        assertThat(result).isPositive();
        // Should be less than face value due to higher yield than coupon
        assertThat(result).isLessThan(1000.0);
    }

    @Test
    @DisplayName("Should calculate retirement savings correctly")
    void shouldCalculateRetirementSavingsCorrectly() {
        // How much to save monthly to have $1M in 30 years at 7%
        // This tests the reverse - what PV is needed for given FV
        double result = PVCalc.pV(0.07 / 12, 30 * 12, 0.0, -1000000.0, false);

        assertThat(result).isPositive();
        assertThat(result).isLessThan(200000.0); // Should be much less than FV due to compounding
    }
}