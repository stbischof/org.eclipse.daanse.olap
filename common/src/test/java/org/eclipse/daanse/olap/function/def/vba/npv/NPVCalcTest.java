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
package org.eclipse.daanse.olap.function.def.vba.npv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NPVCalcTest {

    private NPVCalc npvCalc;
    private DoubleCalc rateCalc;
    private Calc<Object> cashFlowsCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        rateCalc = mock(DoubleCalc.class);
        cashFlowsCalc = mock(Calc.class);
        evaluator = mock(Evaluator.class);
        npvCalc = new NPVCalc(NumericType.INSTANCE, rateCalc, cashFlowsCalc);
    }

    @ParameterizedTest(name = "{0}: NPV({1}, {2}) = {3}")
    @MethodSource("npvArguments")
    @DisplayName("Should calculate NPV correctly")
    void shouldCalculateNPV(String testName, Double rate, Double[] cashFlows, Double expectedNPV) {
        when(rateCalc.evaluate(evaluator)).thenReturn(rate);
        when(cashFlowsCalc.evaluate(evaluator)).thenReturn(cashFlows);

        Double result = npvCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expectedNPV, within(0.0001));
    }

    static Stream<Arguments> npvArguments() {
        return Stream.of(Arguments.of("zero rate", 0.0, new Double[] { -1000.0, 300.0, 400.0, 500.0, 600.0 }, 800.0));
    }

    @Test
    @DisplayName("Should handle empty cash flows array")
    void shouldHandleEmptyCashFlowsArray() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.10);
        when(cashFlowsCalc.evaluate(evaluator)).thenReturn(new Double[] {});

        Double result = npvCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should return null for null rate")
    void shouldHandleNullRate() {
        when(rateCalc.evaluate(evaluator)).thenReturn(null);
        when(cashFlowsCalc.evaluate(evaluator)).thenReturn(new Double[] { -1000.0, 1100.0 });

        assertThat(npvCalc.evaluate(evaluator)).isNull();
    }

    @Test
    @DisplayName("Should return null for null cash flows")
    void shouldHandleNullCashFlows() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.10);
        when(cashFlowsCalc.evaluate(evaluator)).thenReturn(null);

        assertThat(npvCalc.evaluate(evaluator)).isNull();
    }

    @Test
    @DisplayName("Should handle cash flows with null values")
    void shouldHandleCashFlowsWithNullValues() {
        when(rateCalc.evaluate(evaluator)).thenReturn(0.10);
        when(cashFlowsCalc.evaluate(evaluator)).thenReturn(new Double[] { -1000.0, null, 400.0 });

        assertThatThrownBy(() -> npvCalc.evaluate(evaluator)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(npvCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }

}