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
package org.eclipse.daanse.olap.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.junit.jupiter.api.Test;

class StandardAggregatorsTest {

    private static final List<Object> INTS = Arrays.asList(3, null, 5, -1);
    private static final List<Object> DOUBLES = Arrays.asList(1.5, null, 2.5);
    private static final List<Object> NULLS = Arrays.asList(null, null);

    @Test
    void sumMinMaxRollUpRawNumbers() {
        assertThat(SumAggregator.INSTANCE.aggregate(INTS, DataTypeJdbc.INTEGER)).isEqualTo(7);
        assertThat(MinAggregator.INSTANCE.aggregate(INTS, DataTypeJdbc.INTEGER)).isEqualTo(-1);
        assertThat(MaxAggregator.INSTANCE.aggregate(INTS, DataTypeJdbc.INTEGER)).isEqualTo(5);
        assertThat(SumAggregator.INSTANCE.aggregate(DOUBLES, DataTypeJdbc.NUMERIC)).isEqualTo(4.0);
        assertThat(MinAggregator.INSTANCE.aggregate(DOUBLES, DataTypeJdbc.NUMERIC)).isEqualTo(1.5);
        assertThat(MaxAggregator.INSTANCE.aggregate(DOUBLES, DataTypeJdbc.NUMERIC)).isEqualTo(2.5);
    }

    @Test
    void onlyNullsRollUpToNull() {
        assertThat(SumAggregator.INSTANCE.aggregate(NULLS, DataTypeJdbc.NUMERIC)).isNull();
        assertThat(MinAggregator.INSTANCE.aggregate(NULLS, DataTypeJdbc.INTEGER)).isNull();
        assertThat(MaxAggregator.INSTANCE.aggregate(NULLS, DataTypeJdbc.NUMERIC)).isNull();
    }

    @Test
    void rawRollupIsLimitedToNumbers() {
        assertThat(SumAggregator.INSTANCE.supportsFastAggregates(DataTypeJdbc.INTEGER)).isTrue();
        assertThat(SumAggregator.INSTANCE.supportsFastAggregates(DataTypeJdbc.VARCHAR)).isFalse();
        assertThatThrownBy(() -> SumAggregator.INSTANCE.aggregate(INTS, DataTypeJdbc.VARCHAR))
                .isInstanceOf(OlapRuntimeException.class);
    }

    @Test
    void averageNeverRollsUpFromRawValues() {
        assertThat(AvgAggregator.INSTANCE.supportsFastAggregates(DataTypeJdbc.NUMERIC)).isFalse();
        assertThat(AvgAggregator.INSTANCE.getRollup()).isSameAs(AvgAggregator.INSTANCE);
        assertThatThrownBy(() -> AvgAggregator.INSTANCE.aggregate(DOUBLES, DataTypeJdbc.NUMERIC))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void countsRollUpAsSum() {
        assertThat(CountAggregator.INSTANCE.getRollup()).isSameAs(SumAggregator.INSTANCE);
        assertThat(DistinctCountAggregator.INSTANCE.getRollup()).isSameAs(SumAggregator.INSTANCE);
        assertThat(DistinctCountAggregator.INSTANCE.isDistinct()).isTrue();
        assertThat(DistinctCountAggregator.INSTANCE.getNonDistinctAggregator()).isSameAs(CountAggregator.INSTANCE);
        assertThat(DistinctCountAggregator.INSTANCE.supportsFastAggregates(DataTypeJdbc.INTEGER)).isFalse();
    }
}
