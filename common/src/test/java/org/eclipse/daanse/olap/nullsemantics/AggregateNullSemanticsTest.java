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
import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NULL semantics of the static aggregate helpers in {@code FunUtil}:
 *
 * <ul>
 * <li>Empty sets: sum/sumDouble/min/max/avg/percentile/quartile return Java
 * {@code null} (MDX NULL).</li>
 * <li>NULL cells (Java {@code null}) are skipped by evaluateSet, so
 * aggregates ignore them — including the divisor of avg.</li>
 * <li>Error cells ({@link NotLoaded}) turn the aggregate into NaN.</li>
 * </ul>
 */
class AggregateNullSemanticsTest {

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
    @DisplayName("sumDouble of an empty set returns Java null")
    void sumDoubleOfEmptySetReturnsJavaNull() {
        Double result = FunUtil.sumDouble(evaluator, tuples(0), exp);

        // Sum({}) = NULL — Java null, distinguishable from any
        // genuine sum.
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("sumDouble skips NULL cells")
    void sumDoubleIgnoresNullCells(){
        stubCellValues(5.0, null, 3.0);

        Double result = FunUtil.sumDouble(evaluator, tuples(3), exp);

        assertThat(result).isEqualTo(8.0);
    }

    @Test
    @DisplayName("sumDouble returns NaN when a cell holds NotLoaded (error)")
    void sumDoubleWithErrorCellReturnsNaN() {
        stubCellValues(5.0, NotLoaded.INSTANCE, 3.0);

        Double result = FunUtil.sumDouble(evaluator, tuples(3), exp);

        assertThat(result).isNaN();
    }

    @Test
    @DisplayName("sum of an empty set returns Java null ")
    void sumOfEmptySetReturnsNull() {
        Object result = FunUtil.sum(evaluator, tuples(0), exp);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("sum of real values returns their boxed sum")
    void sumOfRealValues() {
        stubCellValues(2.0, 3.0);

        Object result = FunUtil.sum(evaluator, tuples(2), exp);

        assertThat(result).isEqualTo(5.0);
    }

    // --- percentile / quartile ----------------------------------------------

    @Test
    @DisplayName("percentile of an empty set returns Java null")
    void percentileOfEmptySetReturnsJavaNull() {
        Double result = FunUtil.percentile(evaluator, tuples(0), exp, 0.5);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("percentile (median) of real values, NULL cells skipped")
    void percentileMedianIgnoresNullCells() {
        stubCellValues(3.0, null, 1.0, 2.0);

        Double result = FunUtil.percentile(evaluator, tuples(4), exp, 0.5);

        assertThat(result).isEqualTo(2.0);
    }

    @Test
    @DisplayName("quartile of an empty set returns Java null")
    void quartileOfEmptySetReturnsJavaNull() {
        Double result = FunUtil.quartile(evaluator, tuples(0), exp, 2);

        assertThat(result).isNull();
    }

    // --- min / max / avg -----------------------------------------------------

    @Test
    @DisplayName("min of an empty set returns Java null")
    void minOfEmptySetReturnsNull() {
        Object result = FunUtil.min(evaluator, tuples(0), exp);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("max of an empty set returns Java null")
    void maxOfEmptySetReturnsNull() {
        Object result = FunUtil.max(evaluator, tuples(0), exp);

        assertThat(result).isNull();
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
    @DisplayName("avg of an empty set returns Java null")
    void avgOfEmptySetReturnsNull() {
        Object result = FunUtil.avg(evaluator, tuples(0), exp);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("avg ignores NULL cells (they do not count into the divisor)")
    void avgIgnoresNullCells() {
        stubCellValues(2.0, null, 4.0);

        Object result = FunUtil.avg(evaluator, tuples(3), exp);

        assertThat(result).isEqualTo(3.0);
    }
}
