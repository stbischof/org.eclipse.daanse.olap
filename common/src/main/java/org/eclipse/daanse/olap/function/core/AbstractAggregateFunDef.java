/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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
 */
package org.eclipse.daanse.olap.function.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.StoredMeasure;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.DelegatingTupleList;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.exceptions.ResourceLimitExceededException;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl;

/**
 * Abstract base class for all aggregate functions (Aggregate,
 * Sum, Avg, et cetera).
 *
 * @author jhyde
 * @since 2005/8/14
 */
public abstract class AbstractAggregateFunDef extends AbstractFunctionDefinition {

    private final static String iterationLimitExceeded = "Number of iterations exceeded limit of {0,number}";

    public AbstractAggregateFunDef(FunctionMetaData functionMetaData ) {
        super(functionMetaData);
    }

    @Override
	protected Expression validateArgument(
        Validator validator, Expression[] args, int i, DataType category)
    {
        // If expression cache is enabled, wrap first expression (the set)
        // in a function which will use the expression cache.
        // Validation runs while parsing, where no execution is bound, so the setting
        // comes from the query's connection.
        if (i == 0 && validator.getQuery().getConnection().getContext().getConfig().enableExpCache()) {
            Expression arg = args[0];
            if (FunUtil.worthCaching(arg)) {
                final Expression cacheCall =
                    new UnresolvedFunCallImpl(
								new FunctionOperationAtom("Cache"), new Expression[] { arg });
                return validator.validate(cacheCall, false);
            }
        }
        return super.validateArgument(validator, args, i, category);
    }

    /**
     * Evaluates the list of members or tuples used in computing the aggregate.
     * If the measure for aggregation has to ignore unrelated dimensions
     * this method will push unrelated dimension members to top level member.
     * This behaviour is driven by the ignoreUnrelatedDimensions property
     * on a base cube usage specified in the virtual cube.Keeps track of the
     * number of iterations that will be required to iterate over the members
     * or tuples needed to compute the aggregate within the current context.
     * In doing so, also determines if the cross product of all iterations
     * across all parent evaluation contexts will exceed the limit set in the
     * properties file.
     *
     * @param tupleListCalc  calculator used to evaluate the member list
     * @param evaluator current evaluation context
     * @return list of evaluated members or tuples
     */
    public static TupleList evaluateCurrentList(
        TupleListCalc tupleListCalc,
        Evaluator evaluator)
    {
        final int savepoint = evaluator.savepoint();
        TupleList tuples;
        try {
            evaluator.setNonEmpty(false);
            tuples = tupleListCalc.evaluate(evaluator);
        } finally {
            evaluator.restore(savepoint);
        }
        int currLen = tuples.size();
        TupleList dims;
        try {
            dims = AbstractAggregateFunDef.processUnrelatedDimensions(tuples, evaluator);
        } finally {
            evaluator.restore(savepoint);
        }
        AbstractAggregateFunDef.crossProd(evaluator, currLen);
        return dims;
    }

    protected TupleIterable evaluateCurrentIterable(
        TupleIteratorCalc<?> tupleIteratorCalc,
        Evaluator evaluator)
    {
        final int savepoint = evaluator.savepoint();
        int currLen = 0;
        TupleIterable iterable;
        try {
            evaluator.setNonEmpty(false);
            iterable = tupleIteratorCalc.evaluate(evaluator);
        } finally {
            evaluator.restore(savepoint);
        }
        AbstractAggregateFunDef.crossProd(evaluator, currLen);
        return iterable;
    }

    public static void crossProd(Evaluator evaluator, int currLen) {
        long iterationLimit =
            evaluator.getQuery().getConnection().getContext().getConfig().iterationLimit();
        final int productLen = currLen * evaluator.getIterationLength();
        if (iterationLimit > 0 && productLen > iterationLimit) {
                throw new ResourceLimitExceededException(MessageFormat.format(
                    iterationLimitExceeded, iterationLimit));
        }
        evaluator.setIterationLength(currLen);
    }

    /**
     * Pushes unrelated dimensions to the top level member from the given list
     * of tuples if the ignoreUnrelatedDimensions property is set on the base
     * cube usage in the virtual cube.
     *
     * If IgnoreMeasureForNonJoiningDimension is set to true and
     * ignoreUnrelatedDimensions on CubeUsage is set to false then if a non
     * joining dimension exists in the aggregation list then return an empty
     * list else return the original list.
     *
     * @param tuplesForAggregation is a list of members or tuples used in
     * computing the aggregate
     * @param evaluator Evaluator
     * @return list of members or tuples
     */
    public static TupleList processUnrelatedDimensions(
        TupleList tuplesForAggregation,
        Evaluator evaluator)
    {
        if (tuplesForAggregation.isEmpty()) {
            return tuplesForAggregation;
        }

        Member measure = AbstractAggregateFunDef.measureForUnrelatedDimensionCheck(
            evaluator, tuplesForAggregation);

        if (measure.isCalculated()) {
            return tuplesForAggregation;
        }

        Cube virtualCube = evaluator.getCube();
        if (virtualCube.drawsFromOtherCubes()) {
            // this should be a safe cast since we've eliminated calcs above
            StoredMeasure stored = (StoredMeasure) measure;
            Cube baseCube = stored.getCube();
            if (baseCube == null) {
                return tuplesForAggregation;
            }
            if (virtualCube.unrelatedDimensionsStayFor(stored))
            {
                return AbstractAggregateFunDef.ignoreUnrelatedDimensions(
                    tuplesForAggregation, baseCube);
            } else if (evaluator.getQuery().getConnection().getContext()
                    .getConfig().ignoreMeasureForNonJoiningDimension())
            {
                return AbstractAggregateFunDef.ignoreMeasureForNonJoiningDimension(
                    tuplesForAggregation, baseCube);
            }
        }
        return tuplesForAggregation;
    }

    /**
     * Returns the measure to use when determining which dimensions
     * are unrelated.  Most of the time this is the measure in context,
     * except 2 cases:
     * 1)  When a measure is included in a compound slicer
     * 2)  When one or more measures are included in the first parameter
     *     of Aggregate.
     *      e.g. Aggregate( Crossjoin( {Time.[1997]}, {measures.[Unit Sales]})
     * In both cases the measure(s) will be present in tuplesForAggregation.
     */
    private static Member measureForUnrelatedDimensionCheck(
        Evaluator evaluator, TupleList tuplesForAggregation)
    {
        Member measure = evaluator.getMembers()[0];
        if (tuplesForAggregation != null
            && !tuplesForAggregation.isEmpty())
        {
            // this looks for the measure in the first tuple, with the
            // assumption that there is a single measure in all tuples.
            // This assumption is incorrect in the unusual case where
            // a set of measures is used as the first param in an aggregate
            // function.
            for (Member tupMember : tuplesForAggregation.get(0)) {
                if (tupMember.isMeasure()) {
                    measure = tupMember;
                }
            }
        }
        return measure;
    }

    /**
     * If a non joining dimension exists in the aggregation list then return
     * an empty list else return the original list.
     *
     * @param tuplesForAggregation is a list of members or tuples used in
     * computing the aggregate
     * @param baseCube the cube to scan for nonjoining dimensions.
     * @return list of members or tuples
     */
    private static TupleList ignoreMeasureForNonJoiningDimension(
        TupleList tuplesForAggregation,
        Cube baseCube)
    {
        Set<Dimension> nonJoiningDimensions =
            AbstractAggregateFunDef.nonJoiningDimensions(baseCube, tuplesForAggregation);
        if (!nonJoiningDimensions.isEmpty()) {
            return TupleCollections.emptyList(tuplesForAggregation.getArity());
        }
        return tuplesForAggregation;
    }

    /**
     * Pushes unrelated dimensions to the top level member from the given list
     * of tuples if the ignoreUnrelatedDimensions property is set on the base
     * cube usage in the virtual cube.
     *
     * @param tuplesForAggregation is a list of members or tuples used in
     * computing the aggregate
     * @return list of members or tuples
     */
    private static TupleList ignoreUnrelatedDimensions(
        TupleList tuplesForAggregation,
        Cube baseCube)
    {
        Set<Dimension> nonJoiningDimensions =
            AbstractAggregateFunDef.nonJoiningDimensions(baseCube, tuplesForAggregation);
        final Set<List<Member>> processedTuples =
            new LinkedHashSet<>(tuplesForAggregation.size());
        for (List<Member> tuple : tuplesForAggregation) {
            List<Member> tupleCopy = tuple;
            for (int j = 0; j < tuple.size(); j++) {
                final Member member = tuple.get(j);
                if (nonJoiningDimensions.contains(member.getDimension())) {
                    if (tupleCopy == tuple) {
                        // Avoid making a copy until we have to change a tuple.
                        tupleCopy = new ArrayList<>(tuple);
                    }
                    final Hierarchy hierarchy =
                        member.getDimension().getHierarchy();
                    if (hierarchy.hasAll()) {
                        tupleCopy.set(j, hierarchy.getAllMember());
                    } else {
                        tupleCopy.set(j, hierarchy.getDefaultMember());
                    }
                }
            }
            processedTuples.add(tupleCopy);
        }
        return new DelegatingTupleList(
            tuplesForAggregation.getArity(),
            new ArrayList<>(
                processedTuples));
    }

    private static Set<Dimension> nonJoiningDimensions(
        Cube baseCube,
        TupleList tuplesForAggregation)
    {
        List<Member> tuple = tuplesForAggregation.get(0);
        return baseCube.nonJoiningDimensions(
            tuple.toArray(new Member[tuple.size()]));
    }
}
