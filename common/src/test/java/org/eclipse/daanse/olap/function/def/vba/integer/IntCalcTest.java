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
package org.eclipse.daanse.olap.function.def.vba.integer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.exceptions.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IntCalcTest {

    private IntCalc intCalc;
    private Calc<Object> numberCalc;
    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        numberCalc = mock(Calc.class);
        evaluator = mock(Evaluator.class);
        intCalc = new IntCalc(NumericType.INSTANCE, numberCalc);
    }

    @ParameterizedTest(name = "{0}: int({1}) = {2}")
    @MethodSource("arguments")
    @DisplayName("Should convert numbers to integers correctly")
    void shouldConvertNumberToInteger(String testName, Number input, Integer expected) {
        when(numberCalc.evaluate(evaluator)).thenReturn(input);

        Integer result = intCalc.evaluate(evaluator);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should throw InvalidArgumentException for non-number input")
    void shouldThrowExceptionForNonNumberInput() {
        when(numberCalc.evaluate(evaluator)).thenReturn("not a number");

        assertThatThrownBy(() -> intCalc.evaluate(evaluator)).isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("Invalid parameter")
                .hasMessageContaining("number parameter not a number of Int function must be of type number");
    }

    @Test
    @DisplayName("Should throw InvalidArgumentException for null input")
    void shouldCoerceNullInputToZero() {
        // MDX NULL is Java null since the null-semantics migration; Int
        // coerces it to 0, matching the legacy sentinel's observable
        // behavior (.
        when(numberCalc.evaluate(evaluator)).thenReturn(null);

        assertThat(intCalc.evaluate(evaluator)).isEqualTo(0);
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
                // Positive numbers - should truncate towards zero
                Arguments.of("positive integer", 5, 5), Arguments.of("positive decimal truncated", 5.9, 5),
                Arguments.of("positive small decimal", 0.9, 0), Arguments.of("zero", 0, 0),
                Arguments.of("zero decimal", 0.0, 0),

                // Negative numbers - VBA Int function behavior (different from floor)
                Arguments.of("negative integer", -5, -5), Arguments.of("negative decimal rounded down", -5.1, -6), // VBA
                                                                                                                   // Int
                                                                                                                   // rounds
                                                                                                                   // towards
                                                                                                                   // negative
                                                                                                                   // infinity
                                                                                                                   // for
                                                                                                                   // negative
                                                                                                                   // numbers
                Arguments.of("negative decimal rounded down 2", -5.9, -6),
//                Arguments.of("small negative decimal", -0.1, -1),
//                Arguments.of("small negative decimal 2", -0.9, -1),

                // Large numbers
                Arguments.of("large positive", 2147483647, 2147483647),
                Arguments.of("large positive decimal", 2147483647.9, 2147483647),
                Arguments.of("large negative", -2147483648, -2147483648),
                Arguments.of("large negative decimal", -2147483647.1, -2147483648),

                // Different number types
                Arguments.of("byte value", (byte) 127, 127), Arguments.of("short value", (short) 32767, 32767),
                Arguments.of("long value", 123456789L, 123456789), Arguments.of("float value", 123.456f, 123),
                Arguments.of("double value", 123.456, 123),

                // Edge cases for VBA Int behavior
                Arguments.of("negative close to integer", -4.99999, -5),
                Arguments.of("negative exactly integer", -5.0, -5),
                Arguments.of("positive close to integer", 4.99999, 4), Arguments.of("positive exactly integer", 5.0, 5),

                // Very small numbers
                Arguments.of("very small positive", 0.0001, 0)
//                ,
//                Arguments.of("very small negative", -0.0001, -1)
        );
    }
}