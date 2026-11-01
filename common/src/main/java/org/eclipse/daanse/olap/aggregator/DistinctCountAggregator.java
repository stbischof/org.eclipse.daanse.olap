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

import org.eclipse.daanse.olap.api.aggregator.Aggregator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;

/**
 * Counts the distinct values. Where a distinct count can be rolled up at all it is a sum; the
 * engine cannot compute it from cell values, only the provider can.
 */
public class DistinctCountAggregator extends AbstractAggregator {

    public static final DistinctCountAggregator INSTANCE = new DistinctCountAggregator();

    public DistinctCountAggregator() {
        super("distinct-count", true);
    }

    @Override
    public Aggregator getRollup() {
        return SumAggregator.INSTANCE;
    }

    @Override
    public Aggregator getNonDistinctAggregator() {
        return CountAggregator.INSTANCE;
    }

    @Override
    public Object aggregate(Evaluator evaluator, TupleList members, Calc<?> exp) {
        throw new UnsupportedOperationException();
    }
}
