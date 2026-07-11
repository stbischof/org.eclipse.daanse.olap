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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * characterization tests for the static aggregate helpers in
 * {@code FunUtil} and their treatment of the {@code DOUBLE_NULL} /
 * {@code Util.nullValue} sentinels.
 *
 * These tests freeze TODAY's behavior as a safety net for the NULL-semantics
 * refactoring (.
 *
 * Frozen characteristics:
 * - Empty sets: sumDouble/percentile/quartile return the primitive sentinel
 *   0.000000012345; sum/min/max/avg return the Util.nullValue singleton.
 * - NULL cells (Java null or the Util.nullValue singleton) are skipped by
 *   evaluateSet, so aggregates ignore them.
 * - Error cells (Util.valueNotReadyException) turn the aggregate into NaN.
 * - COLLISION: a genuine sum of exactly 0.000000012345 is reported as NULL
 *   by FunUtil.sum.
 */
class AggregateNullSemanticsTest {

    private static final double SENTINEL_VALUE = Double.parseDouble("0.000000012345");

    private Evaluator evaluator;
    private Calc<Object> exp;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        evaluator = mock(Evaluator.class);

        // FunUtil.evaluateSet reaches for
        // evaluator.getQuery().getStatement().getCurrentExecution().
        Query query = mock(Query.class);
        Statement statement = mock(Statement.class);
        Execution execution = mock(Execution.class);
        when(evaluator.getQuery()).thenReturn(query);
        when(query.getStatement()).thenReturn(statement);
        when(statement.getCurrentExecution()).thenReturn(execution);

        exp = mock(Calc.class);
        when(exp.getType()).thenReturn(NumericType.INSTANCE);
    }

    private static TupleList tuples(int memberCount) {
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < memberCount; i++) {
            members.add(mock(Member.class));
        }
        return new UnaryTupleList(members);
    }

    private void stubCellValues(Object first, Object... rest) {
        when(exp.evaluate(evaluator)).thenReturn(first, rest);
    }

    // --- sumDouble / sum ---------------------------------------------------

    @Test
    @DisplayName("sumDouble of an empty set returns the primitive sentinel 0.000000012345")
    void sumDoubleOfEmptySetReturnsSentinel() {
        double result = FunUtil.sumDouble(evaluator, tuples(0), exp);

        // Sum({}) = NULL, encoded as the sentinel value. Note that this is
        // indistinguishable from a genuine sum of 0.000000012345.
        assertThat(result).isEqualTo(FunUtil.DOUBLE_NULL.doubleValue());
    }

    @Test
    @DisplayName("sumDouble skips Java-null and Util.nullValue cells")
    void sumDoubleIgnoresNullCells(){
        stubCellValues(5.0, null, Util.nullValue, 3.0);

        double result = FunUtil.sumDouble(evaluator, tuples(4), exp);

        assertThat(result).isEqualTo(8.0);
    }

    @Test
    @DisplayName("sumDouble returns NaN when a cell holds valueNotReadyException (error)")
    void sumDoubleWithErrorCellReturnsNaN() {
        stubCellValues(5.0, Util.valueNotReadyException, 3.0);

        double result = FunUtil.sumDouble(evaluator, tuples(3), exp);

        assertThat(result).isNaN();
    }

    @Test
    @DisplayName("sum of an empty set returns the Util.nullValue singleton")
    void sumOfEmptySetReturnsNullValueSingleton() {
        Object result = FunUtil.sum(evaluator, tuples(0), exp);

        assertThat(result).isSameAs(Util.nullValue);
    }

    @Test
    @DisplayName("sum of real values returns their boxed sum")
    void sumOfRealValues() {
        stubCellValues(2.0, 3.0);

        Object result = FunUtil.sum(evaluator, tuples(2), exp);

        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("COLLISION: a genuine sum of exactly 0.000000012345 is reported as NULL")
    void sumCollidingWithSentinelValueIsReportedAsNull() {
        // The single cell value is a runtime-boxed 0.000000012345 — a real
        // number, deliberately NOT the sentinel singleton. evaluateSet's
        // identity check lets it through as a value, but FunUtil.sum compares
        // the resulting primitive sum against DOUBLE_NULL BY VALUE and
        // converts it to NULL. This is the value-collision bug; the assertion
        // flips once the sentinel encoding is gone.
        Double genuineTinyValue = Double.valueOf(SENTINEL_VALUE);
        assertThat(genuineTinyValue).isNotSameAs(Util.nullValue);
        stubCellValues(genuineTinyValue);

        Object result = FunUtil.sum(evaluator, tuples(1), exp);

        assertThat(result).isSameAs(Util.nullValue);
    }

    // --- percentile / quartile ----------------------------------------------

    @Test
    @DisplayName("percentile of an empty set returns the primitive sentinel")
    void percentileOfEmptySetReturnsSentinel() {
        double result = FunUtil.percentile(evaluator, tuples(0), exp, 0.5);

        assertThat(result).isEqualTo(FunUtil.DOUBLE_NULL.doubleValue());
    }

    @Test
    @DisplayName("percentile (median) of real values, NULL cells skipped")
    void percentileMedianIgnoresNullCells() {
        stubCellValues(3.0, null, 1.0, 2.0);

        double result = FunUtil.percentile(evaluator, tuples(4), exp, 0.5);

        assertThat(result).isEqualTo(2.0);
    }

    @Test
    @DisplayName("quartile of an empty set returns the primitive sentinel")
    void quartileOfEmptySetReturnsSentinel() {
        double result = FunUtil.quartile(evaluator, tuples(0), exp, 2);

        assertThat(result).isEqualTo(FunUtil.DOUBLE_NULL.doubleValue());
    }

    // --- min / max / avg -----------------------------------------------------

    @Test
    @DisplayName("min of an empty set returns the Util.nullValue singleton")
    void minOfEmptySetReturnsNullValueSingleton() {
        Object result = FunUtil.min(evaluator, tuples(0), exp);

        assertThat(result).isSameAs(Util.nullValue);
    }

    @Test
    @DisplayName("max of an empty set returns the Util.nullValue singleton")
    void maxOfEmptySetReturnsNullValueSingleton() {
        Object result = FunUtil.max(evaluator, tuples(0), exp);

        assertThat(result).isSameAs(Util.nullValue);
    }

    @Test
    @DisplayName("min/max of real values with NULL cells skipped")
    void minMaxIgnoreNullCells() {
        stubCellValues(3.0, null, 1.0, 2.0);
        assertThat(FunUtil.min(evaluator, tuples(4), exp)).isEqualTo(1.0);

        stubCellValues(3.0, null, 1.0, 2.0);
        assertThat(FunUtil.max(evaluator, tuples(4), exp)).isEqualTo(3.0);
    }

    @Test
    @DisplayName("avg of an empty set returns the Util.nullValue singleton")
    void avgOfEmptySetReturnsNullValueSingleton() {
        Object result = FunUtil.avg(evaluator, tuples(0), exp);

        assertThat(result).isSameAs(Util.nullValue);
    }

    @Test
    @DisplayName("avg ignores NULL cells (they do not count into the divisor)")
    void avgIgnoresNullCells() {
        stubCellValues(2.0, null, 4.0);

        Object result = FunUtil.avg(evaluator, tuples(3), exp);

        assertThat(result).isEqualTo(3.0);
    }
}
