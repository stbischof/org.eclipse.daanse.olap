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
 * MDX NULL semantics of the numeric comparison operator calcs (MDX NULL is
 * Java {@code null} in the calc layer): a Java-null (or NaN) operand makes
 * every comparison return {@code FunUtil.BOOLEAN_NULL}, which is the
 * primitive {@code false} — no three-valued logic, so a NULL comparison is
 * indistinguishable from a genuine FALSE; notably even {@code NULL <> x}
 * yields false.
 */
class ComparisonNullSemanticsTest {

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

    @ParameterizedTest(name = "null {0} 2.0 and 2.0 {0} null = false")
    @ValueSource(strings = { "=", "<>", ">", ">=", "<", "<=" })
    @DisplayName("A Java-null operand makes every comparison return BOOLEAN_NULL, which is false")
    void javaNullOperandReturnsBooleanNull(String operator) {
        // FunUtil.BOOLEAN_NULL is the primitive false — no three-valued logic: NULL comparisons are indistinguishable from FALSE. Even
        // NULL <> x is false.
        assertThat(compare(operator, null, 2.0)).isEqualTo(FunUtil.BOOLEAN_NULL).isFalse();
        assertThat(compare(operator, 2.0, null)).isEqualTo(FunUtil.BOOLEAN_NULL).isFalse();
    }

    @ParameterizedTest(name = "NaN {0} 2.0 = false")
    @ValueSource(strings = { "=", "<>", ">", ">=", "<", "<=" })
    @DisplayName("A NaN operand makes every comparison return BOOLEAN_NULL, which is false")
    void nanOperandReturnsBooleanNull(String operator) {
        assertThat(compare(operator, Double.NaN, 2.0)).isFalse();
        assertThat(compare(operator, 2.0, Double.NaN)).isFalse();
    }

    @Test
    @DisplayName("Both operands Java null: every comparison returns false")
    void bothJavaNullOperandsReturnFalse() {
        for (String operator : ALL_OPERATORS) {
            assertThat(compare(operator, null, null))
                    .as("operator %s with both operands null", operator)
                    .isFalse();
        }
    }
}
