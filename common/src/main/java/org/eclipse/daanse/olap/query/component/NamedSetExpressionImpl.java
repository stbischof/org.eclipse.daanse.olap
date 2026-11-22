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
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.query.component;

import java.util.List;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleIteratorCalc;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.query.component.expression.AbstractExpression;

/**
 * Usage of a {@link org.eclipse.daanse.olap.api.element.NamedSet} in an MDX expression.
 *
 * @author jhyde
 * @since Sep 26, 2005
 */
public class NamedSetExpressionImpl extends AbstractExpression implements Expression, NamedSetExpression {
    private final NamedSet namedSet;

    /**
     * Creates a usage of a named set.
     *
     * @param namedSet namedSet
     *  NamedSet != null
     */
    public NamedSetExpressionImpl(NamedSet namedSet) {
        Util.assertPrecondition(namedSet != null, "namedSet != null");
        this.namedSet = namedSet;
    }

    /**
     * Returns the named set.
     *
     *  return != null
     */
    @Override
    public NamedSet getNamedSet() {
        return namedSet;
    }

    @Override
	public String toString() {
        return namedSet.getUniqueName();
    }

    @Override
	public NamedSetExpressionImpl cloneExp() {
        return new NamedSetExpressionImpl(namedSet);
    }

    @Override
	public DataType getCategory() {
        return DataType.SET;
    }

    @Override
	public Expression accept(Validator validator) {
        // A set is sometimes used in more than one cube. So, clone the
        // expression and re-validate every time it is used.
        //
        // But keep the expression wrapped in a NamedSet, so that the
        // expression is evaluated once per query. (We don't want the
        // expression to be evaluated context-sensitive.)
        NamedSet namedSet2 = namedSet.validate(validator);
        if (namedSet2 == namedSet) {
            return this;
        }
        return new NamedSetExpressionImpl(namedSet2);
    }

    @Override
	public Calc<?> accept(ExpressionCompiler compiler) {
        // This is a deliberate breach of the usual rules for interpreting
        // acceptable result styles. Usually the caller gets to call the shots:
        // the callee iterates over the acceptable styles and implements in the
        // first style it is able to. But in this case, we return iterable if
        // the caller can handle it, even if it isn't the caller's first choice.
        // This is because the .current and .currentOrdinal functions only
        // work correctly on iterators.
        final List<ResultStyle> styleList =
            compiler.getAcceptableResultStyles();
        if (!styleList.contains(ResultStyle.ITERABLE)
            && !styleList.contains(ResultStyle.ANY))
        {
            return null;
        }

        return new AbstractNestedTupleIteratorCalc(getType(),
            new Calc[]{/* todo: compile namedSet.getExp() */})
        {
            @Override
			public TupleIterable evaluateInternal(
                Evaluator evaluator)
            {
                final Evaluator.NamedSetEvaluator eval = getEval(evaluator);
                return eval.evaluateTupleIterable(evaluator);
            }

            @Override
			public boolean dependsOn(Hierarchy hierarchy) {
                // Given that a named set is never re-evaluated within the
                // scope of a query, effectively it's independent of all
                // dimensions.
                return false;
            }
        };
    }

    @Override
    public Evaluator.NamedSetEvaluator getEval(Evaluator evaluator) {
        return evaluator.getNamedSetEvaluator(namedSet, true);
    }

    @Override
	public Object accept(QueryComponentVisitor visitor) {
        Object o = visitor.visitNamedSetExpression(this);
        if (visitor.visitChildren()) {
            namedSet.getExp().accept(visitor);
        }
        return o;
    }

    @Override
	public Type getType() {
        return namedSet.getType();
    }

}
