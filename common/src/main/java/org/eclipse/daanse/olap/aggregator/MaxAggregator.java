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

import java.util.List;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.fun.FunUtil;

/** The largest value; raw integers and numerics roll up directly. */
public class MaxAggregator extends AbstractAggregator {

    public static final MaxAggregator INSTANCE = new MaxAggregator();

    public MaxAggregator() {
        super("max", false);
    }

    @Override
    public Object aggregate(Evaluator evaluator, TupleList members, Calc<?> exp) {
        return FunUtil.max(evaluator, members, exp);
    }

    @Override
    public boolean supportsFastAggregates(DataTypeJdbc dataType) {
        return isNumber(dataType);
    }

    @Override
    public Object aggregate(List<Object> rawData, DataTypeJdbc datatype) {
        assert !rawData.isEmpty();
        switch (datatype) {
        case INTEGER:
            int resultInt = Integer.MIN_VALUE;
            for (Object data : rawData) {
                if (data != null) {
                    resultInt = Math.max(resultInt, (Integer) data);
                }
            }
            return resultInt == Integer.MIN_VALUE ? null : resultInt;
        case NUMERIC:
            double resultDouble = Double.NEGATIVE_INFINITY;
            for (Object data : rawData) {
                if (data != null) {
                    resultDouble = Math.max(resultDouble, ((Number) data).doubleValue());
                }
            }
            return resultDouble == Double.NEGATIVE_INFINITY ? null : resultDouble;
        default:
            throw unsupportedDatatype(datatype);
        }
    }
}
