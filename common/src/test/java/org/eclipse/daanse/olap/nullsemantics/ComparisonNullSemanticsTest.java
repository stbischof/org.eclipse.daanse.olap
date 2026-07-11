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
package org.eclipse.daanse.olap.nullsemantics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.def.operators.equal.EqualCalc;
import org.eclipse.daanse.olap.function.def.operators.greater.GreaterCalc;
import org.eclipse.daanse.olap.function.def.operators.greater.GreaterOrEqualCalc;
import org.eclipse.daanse.olap.function.def.operators.less.LessCalc;
import org.eclipse.daanse.olap.function.def.operators.less.LessOrEqualCalc;
import org.eclipse.daanse.olap.function.def.operators.notequal.NotEqualCalc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * characterization tests for the numeric comparison operator calcs and
 * their treatment of the {@code FunUtil.DOUBLE_NULL} sentinel.
 *
 * These tests freeze TODAY's behavior as a safety net for the NULL-semantics
 * refactoring (.
 *
 * Characteristics frozen here:
 * - A sentinel (or NaN) operand makes every comparison return
 *   {@code FunUtil.BOOLEAN_NULL}, which is the primitive {@code false} — so a
 *   NULL comparison is indistinguishable from a genuine FALSE. Notably even
 *   {@code NULL <> x} yields false.
 * - The sentinel check on boxed Doubles ({@code v0 == FunUtil.DOUBLE_NULL}) is
 *   a REFERENCE comparison; a value-equal distinct instance compares as an
 *   ordinary number.
 * - A Java {@code null} operand causes a NullPointerException, because
 *   {@code Double.isNaN(v0)} auto-unboxes before any null check.
 */
class ComparisonNullSemanticsTest {

    private static final Double SENTINEL = FunUtil.DOUBLE_NULL;
    private static final double SENTINEL_VALUE = Double.parseDouble("0.000000012345");
    private static final String[] ALL_OPERATORS = { "=", "<>", ">", ">=", "<", "<=" };

    private Evaluator evaluator;
    private DoubleCalc calc0;
    private DoubleCalc calc1;

    // The comparison calcs have protected constructors; minimal subclasses make
    // them instantiable from this dedicated test package.
    private static final class TestableEqualCalc extends EqualCalc {
        TestableEqualCalc(Type type, DoubleCalc c0, DoubleCalc c1) {
            super(type, c0, c1);
        }
    }

    private static final class TestableNotEqualCalc extends NotEqualCalc {
        TestableNotEqualCalc(Type type, DoubleCalc c0, DoubleCalc c1) {
            super(type, c0, c1);
        }
    }

    private static final class TestableGreaterCalc extends GreaterCalc {
        TestableGreaterCalc(Type type, DoubleCalc c0, DoubleCalc c1) {
            super(type, c0, c1);
        }
    }

    private static final class TestableGreaterOrEqualCalc extends GreaterOrEqualCalc {
        TestableGreaterOrEqualCalc(Type type, DoubleCalc c0, DoubleCalc c1) {
            super(type, c0, c1);
        }
    }

    private static final class TestableLessCalc extends LessCalc {
        TestableLessCalc(Type type, DoubleCalc c0, DoubleCalc c1) {
            super(type, c0, c1);
        }
    }

    private static final class TestableLessOrEqualCalc extends LessOrEqualCalc {
        TestableLessOrEqualCalc(Type type, DoubleCalc c0, DoubleCalc c1) {
            super(type, c0, c1);
        }
    }

    @BeforeEach
    void setUp() {
        evaluator = mock(Evaluator.class);
        calc0 = mock(DoubleCalc.class);
        calc1 = mock(DoubleCalc.class);
    }

    private static Double distinctSentinelValue() {
        return Double.valueOf(SENTINEL_VALUE);
    }

    private Boolean compare(String operator, Double v0, Double v1) {
        when(calc0.evaluate(evaluator)).thenReturn(v0);
        when(calc1.evaluate(evaluator)).thenReturn(v1);
        BooleanType type = BooleanType.INSTANCE;
        return switch (operator) {
        case "=" -> new TestableEqualCalc(type, calc0, calc1).evaluate(evaluator);
        case "<>" -> new TestableNotEqualCalc(type, calc0, calc1).evaluate(evaluator);
        case ">" -> new TestableGreaterCalc(type, calc0, calc1).evaluate(evaluator);
        case ">=" -> new TestableGreaterOrEqualCalc(type, calc0, calc1).evaluate(evaluator);
        case "<" -> new TestableLessCalc(type, calc0, calc1).evaluate(evaluator);
        case "<=" -> new TestableLessOrEqualCalc(type, calc0, calc1).evaluate(evaluator);
        default -> throw new IllegalArgumentException(operator);
        };
    }

    @ParameterizedTest(name = "{0}: {1} {0} {2} = {3}")
    @MethodSource("realValueArguments")
    @DisplayName("Comparisons of two real values follow ordinary Java semantics")
    void realValueCharacterization(String operator, Double v0, Double v1, Boolean expected) {
        assertThat(compare(operator, v0, v1)).isEqualTo(expected);
    }

    static Stream<Arguments> realValueArguments() {
        return Stream.of(
                Arguments.of("=", 2.0, 2.0, true),
                Arguments.of("=", 2.0, 3.0, false),
                Arguments.of("<>", 2.0, 3.0, true),
                Arguments.of("<>", 2.0, 2.0, false),
                Arguments.of(">", 3.0, 2.0, true),
                Arguments.of(">", 2.0, 3.0, false),
                Arguments.of(">=", 2.0, 2.0, true),
                Arguments.of(">=", 1.0, 2.0, false),
                Arguments.of("<", 2.0, 3.0, true),
                Arguments.of("<", 3.0, 2.0, false),
                Arguments.of("<=", 2.0, 2.0, true),
                Arguments.of("<=", 3.0, 2.0, false));
    }

    @ParameterizedTest(name = "sentinel {0} 2.0 and 2.0 {0} sentinel = false")
    @ValueSource(strings = { "=", "<>", ">", ">=", "<", "<=" })
    @DisplayName("A sentinel operand makes every comparison return BOOLEAN_NULL, which is false")
    void sentinelOperandReturnsBooleanNull(String operator) {
        // FunUtil.BOOLEAN_NULL is the primitive false ("placeholder until we
        // actually implement 3VL") — NULL comparisons are indistinguishable
        // from FALSE today. Even NULL <> x is false.
        assertThat(compare(operator, SENTINEL, 2.0)).isEqualTo(FunUtil.BOOLEAN_NULL).isFalse();
        assertThat(compare(operator, 2.0, SENTINEL)).isEqualTo(FunUtil.BOOLEAN_NULL).isFalse();
    }

    @ParameterizedTest(name = "NaN {0} 2.0 = false")
    @ValueSource(strings = { "=", "<>", ">", ">=", "<", "<=" })
    @DisplayName("A NaN operand makes every comparison return BOOLEAN_NULL, which is false")
    void nanOperandReturnsBooleanNull(String operator) {
        assertThat(compare(operator, Double.NaN, 2.0)).isFalse();
        assertThat(compare(operator, 2.0, Double.NaN)).isFalse();
    }

    @Test
    @DisplayName("A Java null operand causes a NullPointerException (Double.isNaN unboxes before any null check)")
    void javaNullOperandThrowsNullPointerException() {
        for (String operator : ALL_OPERATORS) {
            assertThatThrownBy(() -> compare(operator, null, 2.0))
                    .as("operator %s with null left operand", operator)
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> compare(operator, 2.0, null))
                    .as("operator %s with null right operand", operator)
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    @DisplayName("A value-equal but distinct 0.000000012345 instance compares as a real value")
    void computedSentinelValueComparesAsRealValue() {
        // The '== FunUtil.DOUBLE_NULL' check is reference-based: a distinct
        // Double with the same value is NOT recognized as NULL and compares
        // as an ordinary tiny positive number. flips once the sentinel encoding is gone.
        assertThat(compare("=", distinctSentinelValue(), distinctSentinelValue())).isTrue();
        assertThat(compare("<>", distinctSentinelValue(), distinctSentinelValue())).isFalse();
        assertThat(compare(">", distinctSentinelValue(), 0.0)).isTrue();
        assertThat(compare("<", distinctSentinelValue(), 1.0)).isTrue();

        // But against the sentinel singleton itself, the reference check wins
        // and the comparison degrades to BOOLEAN_NULL (false).
        assertThat(compare("=", distinctSentinelValue(), SENTINEL)).isFalse();
    }
}
