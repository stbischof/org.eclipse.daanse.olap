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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;

/** Does not aggregate: the value of the aggregated cell always comes from the provider. */
public class NoneAggregator extends AbstractAggregator {

    public static final NoneAggregator INSTANCE = new NoneAggregator();

    public NoneAggregator() {
        super("None", false);
    }

    @Override
    public Object aggregate(Evaluator evaluator, TupleList members, Calc<?> calc) {
        final Evaluator eval = evaluator.pushAggregation(members);
        eval.setNonEmpty(false);
        return eval.evaluateCurrent();
    }
}
