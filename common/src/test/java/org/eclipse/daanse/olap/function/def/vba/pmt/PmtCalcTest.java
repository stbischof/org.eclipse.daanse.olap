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
package org.eclipse.daanse.olap.function.def.vba.pmt;

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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PmtCalcTest {

    private PmtCalc pmtCalc;
    private DoubleCalc rateCalc;
    private DoubleCalc nPerCalc;
    private DoubleCalc pvCalc;
    private DoubleCalc fvCalc;
    private BooleanCalc dueCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        rateCalc = mock(DoubleCalc.class);
        nPerCalc = mock(DoubleCalc.class);
        pvCalc = mock(DoubleCalc.class);
        fvCalc = mock(DoubleCalc.class);
        dueCalc = mock(BooleanCalc.class);
        evaluator = mock(Evaluator.class);
        pmtCalc = new PmtCalc(NumericType.INSTANCE, rateCalc, nPerCalc, pvCalc, fvCalc, dueCalc);
    }

    @ParameterizedTest(name = "{0}: PMT({1}, {2}, {3}, {4}, {5}) = {6}")
    @MethodSource("pmtArguments")
    @DisplayName("Should calculate PMT correctly")
    void shouldCalculatePMT(String testName, Double rate, Double nPer, Double pv, Double fv, Boolean due,
            Double expectedPMT) {
        when(rateCalc.evaluate(evaluator)).thenReturn(rate);
        when(nPerCalc.evaluate(evaluator)).thenReturn(nPer);
        when(pvCalc.evaluate(evaluator)).thenReturn(pv);
        when(fvCalc.evaluate(evaluator)).thenReturn(fv);
        when(dueCalc.evaluate(evaluator)).thenReturn(due);

        Double result = pmtCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expectedPMT, within(0.01));
    }

    static Stream<Arguments> pmtArguments() {
        return Stream.of(
                // Standard loan calculations
                Arguments.of("standard loan end of period", 0.08 / 12, 12.0, 10000.0, 0.0, false, -869.88),
                Arguments.of("standard loan beginning of period", 0.08 / 12, 12.0, 10000.0, 0.0, true, -864.13),

                // Zero interest rate (simple division)
                Arguments.of("zero rate loan", 0.0, 12.0, 12000.0, 0.0, false, -1000.0),
                Arguments.of("zero rate with future value", 0.0, 12.0, 10000.0, 2000.0, false, -1000.0),

                Arguments.of("single payment", 0.10, 1.0, 1000.0, 0.0, false, -1100.0));
    }

    @Test
    @DisplayName("Should handle zero periods")
    @Disabled
    void shouldHandleZeroPeriods() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.10);
        when(nPerCalc.evaluate(evaluator)).thenReturn(0.0);
        when(pvCalc.evaluate(evaluator)).thenReturn(1000.0);
        when(fvCalc.evaluate(evaluator)).thenReturn(0.0);
        when(dueCalc.evaluate(evaluator)).thenReturn(false);

        assertThatThrownBy(() -> pmtCalc.evaluate(evaluator)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("Should handle negative periods")
    void shouldHandleNegativePeriods() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.10);
        when(nPerCalc.evaluate(evaluator)).thenReturn(-5.0);
        when(pvCalc.evaluate(evaluator)).thenReturn(1000.0);
        when(fvCalc.evaluate(evaluator)).thenReturn(0.0);
        when(dueCalc.evaluate(evaluator)).thenReturn(false);

        Double result = pmtCalc.evaluate(evaluator);

        // Should still calculate, though the result may not be meaningful
        assertThat(result).isNotNull();
        assertThat(result).isFinite();
    }

    @Test
    @DisplayName("Should test static pmt method directly")
    void shouldTestStaticPmtMethodDirectly() {
        double result = PmtCalc.pmt(0.08 / 12, 12.0, 10000.0, 0.0, false);

        assertThat(result).isCloseTo(-869.88, within(0.01));
    }

    @Test
    @DisplayName("Should test static pmt method with zero rate")
    void shouldTestStaticPmtMethodWithZeroRate() {
        double result = PmtCalc.pmt(0.0, 12.0, 12000.0, 0.0, false);

        assertThat(result).isCloseTo(-1000.0, within(0.01));
    }

    @Test
    @DisplayName("Should return null for null values")
    void shouldHandleNullValues() {
        when(rateCalc.evaluate(evaluator)).thenReturn(null);
        when(nPerCalc.evaluate(evaluator)).thenReturn(12.0);
        when(pvCalc.evaluate(evaluator)).thenReturn(1000.0);
        when(fvCalc.evaluate(evaluator)).thenReturn(0.0);
        when(dueCalc.evaluate(evaluator)).thenReturn(false);

        assertThat(pmtCalc.evaluate(evaluator)).isNull();
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(pmtCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }

    @Test
    @DisplayName("Should handle very small rate")
    void shouldHandleVerySmallRate() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.0001 / 12); // Very small monthly rate
        when(nPerCalc.evaluate(evaluator)).thenReturn(12.0);
        when(pvCalc.evaluate(evaluator)).thenReturn(1000.0);
        when(fvCalc.evaluate(evaluator)).thenReturn(0.0);
        when(dueCalc.evaluate(evaluator)).thenReturn(false);

        Double result = pmtCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result).isFinite();
        // Should be close to simple division when rate is very small
        assertThat(result).isCloseTo(-83.33, within(1.0));
    }

    @Test
    @DisplayName("Should handle large number of periods")
    void shouldHandleLargeNumberOfPeriods() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.05 / 12);
        when(nPerCalc.evaluate(evaluator)).thenReturn(360.0); // 30 years
        when(pvCalc.evaluate(evaluator)).thenReturn(200000.0);
        when(fvCalc.evaluate(evaluator)).thenReturn(0.0);
        when(dueCalc.evaluate(evaluator)).thenReturn(false);

        Double result = pmtCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result).isFinite();
        assertThat(result).isNegative(); // Payment should be negative
        assertThat(Math.abs(result)).isLessThan(2000.0); // Reasonable monthly payment
    }

    @Test
    @DisplayName("Should calculate mortgage payment correctly")
    void shouldCalculateMortgagePaymentCorrectly() {
        // $200,000 mortgage at 5% for 30 years
        double result = PmtCalc.pmt((Double) 0.05 / 12, (Double) 30.0D * 12.0D, 200000.0D, 0.0D, Boolean.FALSE);

        // Expected monthly payment around $1073.64
        assertThat(result).isCloseTo(-1073.64, within(1.0));
    }
}