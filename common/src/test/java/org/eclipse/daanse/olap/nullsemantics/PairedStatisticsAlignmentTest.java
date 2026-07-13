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
import static org.assertj.core.api.Assertions.within;
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
import org.eclipse.daanse.olap.fun.FunUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pair alignment of the paired statistics: Covariance and Correlation
 * evaluate both expressions per tuple and drop a tuple ONLY when one of the
 * two values is NULL (pairwise deletion) — dropping NULLs per set
 * independently would silently pair x and y values of different tuples
 * whenever the NULL patterns differed.
 */
class PairedStatisticsAlignmentTest {

    private Evaluator evaluator;
    private Calc<Object> exp1;
    private Calc<Object> exp2;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        evaluator = mock(Evaluator.class);
        Query query = mock(Query.class);
        Statement statement = mock(Statement.class);
        Execution execution = mock(Execution.class);
        when(evaluator.getQuery()).thenReturn(query);
        when(query.getStatement()).thenReturn(statement);
        when(statement.getCurrentExecution()).thenReturn(execution);

        exp1 = mock(Calc.class);
        exp2 = mock(Calc.class);
        when(exp1.getType()).thenReturn(NumericType.INSTANCE);
        when(exp2.getType()).thenReturn(NumericType.INSTANCE);
    }

    private static TupleList tuples(int memberCount) {
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < memberCount; i++) {
            members.add(mock(Member.class));
        }
        return new UnaryTupleList(members);
    }

    @Test
    @DisplayName("Tuples with a NULL on either side are dropped pairwise, keeping x/y aligned")
    void covariancePairwiseDeletionKeepsAlignment() {
        // tuples:      t1      t2      t3      t4
        // exp1:        1.0     null    3.0     5.0
        // exp2:        2.0     4.0     null    10.0
        // aligned pairs: (1,2) and (5,10) -> unbiased covariance:
        // means (3, 6); ((1-3)(2-6) + (5-3)(10-6)) / (2-1) = (8 + 8) / 1 = 16
        when(exp1.evaluate(evaluator)).thenReturn(1.0, null, 3.0, 5.0);
        when(exp2.evaluate(evaluator)).thenReturn(2.0, 4.0, null, 10.0);

        Object covar = FunUtil.covariance(evaluator, tuples(4), exp1, exp2, false);

        assertThat(covar).isInstanceOf(Double.class);
        assertThat((Double) covar).isEqualTo(16.0);
        // Dropping NULLs per set instead would pair (1,2),(3,4),(5,10) and
        // produce a different (wrong) value.
    }

    @Test
    @DisplayName("Correlation uses the same aligned pairs for covariance and both variances")
    void correlationUsesAlignedPairs() {
        // aligned pairs (1,2) and (5,10) lie exactly on y = 2x -> r == 1
        when(exp1.evaluate(evaluator)).thenReturn(1.0, null, 5.0);
        when(exp2.evaluate(evaluator)).thenReturn(2.0, 7.0, 10.0);

        double r = FunUtil.correlation(evaluator, tuples(3), exp1, exp2);

        assertThat(r).isCloseTo(1.0, within(1e-12));
    }

    @Test
    @DisplayName("No aligned pairs at all: Covariance is NULL, Correlation NaN")
    void allPairsIncomplete() {
        when(exp1.evaluate(evaluator)).thenReturn(1.0, null);
        when(exp2.evaluate(evaluator)).thenReturn(null, 4.0);

        Object covar = FunUtil.covariance(evaluator, tuples(2), exp1, exp2, false);
        assertThat(covar).isNull();

        when(exp1.evaluate(evaluator)).thenReturn(1.0, null);
        when(exp2.evaluate(evaluator)).thenReturn(null, 4.0);
        assertThat(FunUtil.correlation(evaluator, tuples(2), exp1, exp2)).isNaN();
    }

    @Test
    @DisplayName("Biased covariance divides by n")
    void biasedCovariance() {
        when(exp1.evaluate(evaluator)).thenReturn(1.0, 5.0);
        when(exp2.evaluate(evaluator)).thenReturn(2.0, 10.0);

        Object covar = FunUtil.covariance(evaluator, tuples(2), exp1, exp2, true);

        assertThat((Double) covar).isEqualTo(8.0);
    }
}
