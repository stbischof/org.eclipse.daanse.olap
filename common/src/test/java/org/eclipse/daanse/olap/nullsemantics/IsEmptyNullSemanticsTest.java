/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.def.logical.IsEmptyCalc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the uniform {@code IsEmpty} semantics: MDX NULL is Java null
 * everywhere, so {@code IsEmpty} of any NULL-valued expression is
 * {@code true} — including computed numeric NULLs such as
 * {@code NullValue()}. This matches MSAS.
 */
class IsEmptyNullSemanticsTest {

    // IsEmptyCalc has a protected constructor; a minimal subclass makes it
    // instantiable from this dedicated test package.
    private static final class TestableIsEmptyCalc extends IsEmptyCalc {
        TestableIsEmptyCalc(Type type, Calc<?> calc) {
            super(type, calc);
        }
    }

    private Evaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = mock(Evaluator.class);
    }

    @Test
    @DisplayName("IsEmpty over a numeric (DoubleCalc) NULL is true - e.g. IsEmpty(NullValue())")
    void numericNullIsEmpty() {
        DoubleCalc doubleCalc = mock(DoubleCalc.class);
        when(doubleCalc.evaluate(evaluator)).thenReturn(null);
        assertThat(new TestableIsEmptyCalc(BooleanType.INSTANCE, doubleCalc).evaluate(evaluator)).isTrue();
    }

    @Test
    @DisplayName("IsEmpty over a numeric (DoubleCalc) value is false")
    void numericValueIsNotEmpty() {
        DoubleCalc doubleCalc = mock(DoubleCalc.class);
        when(doubleCalc.evaluate(evaluator)).thenReturn(42.5);
        assertThat(new TestableIsEmptyCalc(BooleanType.INSTANCE, doubleCalc).evaluate(evaluator)).isFalse();
    }

    @Test
    @DisplayName("IsEmpty over an object-lane null (NULL cell read) is true")
    @SuppressWarnings("unchecked")
    void objectNullIsEmpty() {
        Calc<Object> objectCalc = mock(Calc.class);
        when(objectCalc.evaluate(evaluator)).thenReturn(null);
        assertThat(new TestableIsEmptyCalc(BooleanType.INSTANCE, objectCalc).evaluate(evaluator)).isTrue();
    }

    @Test
    @DisplayName("IsEmpty over an object-lane value is false")
    @SuppressWarnings("unchecked")
    void objectValueIsNotEmpty() {
        Calc<Object> objectCalc = mock(Calc.class);
        when(objectCalc.evaluate(evaluator)).thenReturn("value");
        assertThat(new TestableIsEmptyCalc(BooleanType.INSTANCE, objectCalc).evaluate(evaluator)).isFalse();
    }
}
