/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2003-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara
 * All Rights Reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.api.aggregator;

import java.util.List;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;

/**
 * Describes an aggregation operator, such as "sum" or "count".
 *
 * @see FunctionDefinition
 * @see Evaluator
 *
 * @author jhyde$
 * @since Jul 9, 2003$
 */
public interface Aggregator {

    /**
     * Returns the name of the Aggregator.
     * 
     * @return name of the Aggregator
     */
    String getName();

    /**
     * Returns the aggregator used to combine sub-totals into a grand-total.
     *
     * @return aggregator used to combine sub-totals into a grand-total
     */
    Aggregator getRollup();

    /**
     * Applies this aggregator to an expression over a set of members and returns
     * the result.
     *
     * @param evaluator Evaluation context
     * @param members   List of members, not null
     * @param calc      Expression to evaluate
     *
     * @return result of applying this aggregator to a set of members/tuples
     */
    Object aggregate(Evaluator evaluator, TupleList members, Calc<?> calc);

    /**
     * Tells Mondrian if this aggregator can perform fast aggregation using only the
     * raw data of a given object type. This will determine if Mondrian will attempt
     * to perform in-memory rollups on raw segment data by invoking #aggregate.
     *
     * This is only invoked for rollup operations.
     *
     * @param datatype The datatype of the object we would like to rollup.
     * @return Whether this aggregator supports fast aggregation
     */
    boolean supportsFastAggregates(DataTypeJdbc datatype);

    /**
     * Applies this aggregator over a raw list of objects for a rollup operation.
     * This is useful when the values are already resolved and we are dealing with a
     * raw SegmentBody object.
     *
     * Only gets called if #supportsFastAggregates(DataTypeJdbc) is true.
     *
     * This is only invoked for rollup operations.
     *
     * @param rawData An array of values in its raw form, to be aggregated.
     * @return A rolled up value of the raw data. if the object type is not
     *         supported.
     */
    Object aggregate(List<Object> rawData, DataTypeJdbc datatype);

    boolean isDistinct();

    Aggregator getNonDistinctAggregator();
}
