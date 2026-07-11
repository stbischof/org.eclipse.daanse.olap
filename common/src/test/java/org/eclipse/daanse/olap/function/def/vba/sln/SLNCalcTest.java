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
package org.eclipse.daanse.olap.function.def.vba.sln;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SLNCalcTest {

    private SLNCalc slnCalc;
    private DoubleCalc costCalc;
    private DoubleCalc salvageCalc;
    private DoubleCalc lifeCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        costCalc = mock(DoubleCalc.class);
        salvageCalc = mock(DoubleCalc.class);
        lifeCalc = mock(DoubleCalc.class);
        evaluator = mock(Evaluator.class);
        slnCalc = new SLNCalc(NumericType.INSTANCE, costCalc, salvageCalc, lifeCalc);
    }

    @ParameterizedTest(name = "{0}: SLN({1}, {2}, {3}) = {4}")
    @MethodSource("slnArguments")
    @DisplayName("Should calculate straight-line depreciation correctly")
    void shouldCalculateSLN(String testName, Double cost, Double salvage, Double life, Double expectedSLN) {
        when(costCalc.evaluate(evaluator)).thenReturn(cost);
        when(salvageCalc.evaluate(evaluator)).thenReturn(salvage);
        when(lifeCalc.evaluate(evaluator)).thenReturn(life);

        Double result = slnCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expectedSLN, within(0.001));
    }

    static Stream<Arguments> slnArguments() {
        return Stream.of(
                // Standard depreciation scenarios
                Arguments.of("equipment depreciation", 50000.0, 5000.0, 10.0, 4500.0),
                Arguments.of("vehicle depreciation", 30000.0, 3000.0, 5.0, 5400.0),
                Arguments.of("machinery depreciation", 100000.0, 10000.0, 15.0, 6000.0),

                // Zero salvage value
                Arguments.of("zero salvage value", 20000.0, 0.0, 8.0, 2500.0),
                Arguments.of("computer equipment", 5000.0, 0.0, 3.0, 1666.667),

                // High salvage value
                Arguments.of("high salvage value", 50000.0, 40000.0, 10.0, 1000.0),
                Arguments.of("collectible asset", 100000.0, 95000.0, 20.0, 250.0),

                // Short life assets
                Arguments.of("one year asset", 12000.0, 2000.0, 1.0, 10000.0),
                Arguments.of("two year asset", 8000.0, 1000.0, 2.0, 3500.0),

                // Long life assets
                Arguments.of("building depreciation", 500000.0, 50000.0, 40.0, 11250.0),
                Arguments.of("infrastructure", 1000000.0, 100000.0, 50.0, 18000.0),

                // Edge cases with small values
                Arguments.of("small cost asset", 100.0, 10.0, 5.0, 18.0),
                Arguments.of("minimal depreciation", 1000.0, 999.0, 10.0, 0.1),

                // Equal cost and salvage (no depreciation)
                Arguments.of("no depreciation", 10000.0, 10000.0, 5.0, 0.0),

                // Fractional life
                Arguments.of("fractional life", 15000.0, 3000.0, 2.5, 4800.0),
                Arguments.of("half year life", 6000.0, 1000.0, 0.5, 10000.0));
    }

    @Test
    @DisplayName("Should handle negative life")
    void shouldHandleNegativeLife() {
        when(costCalc.evaluate(evaluator)).thenReturn(10000.0);
        when(salvageCalc.evaluate(evaluator)).thenReturn(1000.0);
        when(lifeCalc.evaluate(evaluator)).thenReturn(-5.0);

        Double result = slnCalc.evaluate(evaluator);

        // With negative life, result will be negative
        assertThat(result).isNegative();
        assertThat(result).isCloseTo(-1800.0, within(0.001));
    }

    @Test
    @DisplayName("Should handle salvage greater than cost")
    void shouldHandleSalvageGreaterThanCost() {
        when(costCalc.evaluate(evaluator)).thenReturn(10000.0);
        when(salvageCalc.evaluate(evaluator)).thenReturn(15000.0);
        when(lifeCalc.evaluate(evaluator)).thenReturn(5.0);

        Double result = slnCalc.evaluate(evaluator);

        // Should result in negative depreciation
        assertThat(result).isNegative();
        assertThat(result).isCloseTo(-1000.0, within(0.001));
    }

    @Test
    @DisplayName("Should return null for null values")
    void shouldHandleNullValues() {
        when(costCalc.evaluate(evaluator)).thenReturn(null);
        when(salvageCalc.evaluate(evaluator)).thenReturn(1000.0);
        when(lifeCalc.evaluate(evaluator)).thenReturn(5.0);

        assertThat(slnCalc.evaluate(evaluator)).isNull();
    }

    @Test
    @DisplayName("Should preserve type information")
    void shouldPreserveTypeInformation() {
        assertThat(slnCalc.getType()).isEqualTo(NumericType.INSTANCE);
    }

    @Test
    @DisplayName("Should handle very large values")
    void shouldHandleVeryLargeValues() {
        when(costCalc.evaluate(evaluator)).thenReturn(1000000000.0); // $1 billion
        when(salvageCalc.evaluate(evaluator)).thenReturn(100000000.0); // $100 million
        when(lifeCalc.evaluate(evaluator)).thenReturn(25.0);

        Double result = slnCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result).isFinite();
        assertThat(result).isCloseTo(36000000.0, within(1.0)); // $36 million per year
    }

    @Test
    @DisplayName("Should handle very small values")
    void shouldHandleVerySmallValues() {
        when(costCalc.evaluate(evaluator)).thenReturn(1.0);
        when(salvageCalc.evaluate(evaluator)).thenReturn(0.10);
        when(lifeCalc.evaluate(evaluator)).thenReturn(10.0);

        Double result = slnCalc.evaluate(evaluator);

        assertThat(result).isNotNull();
        assertThat(result).isFinite();
        assertThat(result).isCloseTo(0.09, within(0.001));
    }

    @Test
    @DisplayName("Should calculate annual depreciation for office equipment")
    void shouldCalculateAnnualDepreciationForOfficeEquipment() {
        // $25,000 office equipment, $2,500 salvage, 7 years
        when(costCalc.evaluate(evaluator)).thenReturn(25000.0);
        when(salvageCalc.evaluate(evaluator)).thenReturn(2500.0);
        when(lifeCalc.evaluate(evaluator)).thenReturn(7.0);

        Double result = slnCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(3214.286, within(0.01));
    }

    @Test
    @DisplayName("Should handle fractional currency values")
    void shouldHandleFractionalCurrencyValues() {
        when(costCalc.evaluate(evaluator)).thenReturn(1234.56);
        when(salvageCalc.evaluate(evaluator)).thenReturn(234.56);
        when(lifeCalc.evaluate(evaluator)).thenReturn(3.0);

        Double result = slnCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(333.333, within(0.001));
    }

    @Test
    @DisplayName("Should verify formula (cost - salvage) / life")
    void shouldVerifyFormula() {
        double cost = 50000.0;
        double salvage = 8000.0;
        double life = 12.0;
        double expected = (cost - salvage) / life;

        when(costCalc.evaluate(evaluator)).thenReturn(cost);
        when(salvageCalc.evaluate(evaluator)).thenReturn(salvage);
        when(lifeCalc.evaluate(evaluator)).thenReturn(life);

        Double result = slnCalc.evaluate(evaluator);

        assertThat(result).isCloseTo(expected, within(0.0001));
    }

}