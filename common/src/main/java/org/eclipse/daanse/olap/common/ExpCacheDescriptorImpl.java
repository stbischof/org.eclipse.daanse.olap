/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2005-2005 Julian Hyde
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
 */


package org.eclipse.daanse.olap.common;

import org.eclipse.daanse.olap.api.calc.compiler.CompileRequest;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.cache.ExpCacheDescriptor;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.calc.base.compiler.StandardExpressionCompiler;

/**
 * Holds information necessary to add an expression to the expression result
 * cache (see {@link Evaluator#getCachedResult(ExpCacheDescriptorImpl)}).
 *
 * @author jhyde
 * @since Aug 16, 2005
 */
public class ExpCacheDescriptorImpl implements ExpCacheDescriptor {
    private final Expression exp;
    private int[] dependentHierarchyOrdinals;
    private final Calc<?> calc;

    /**
     * Creates a descriptor with a given compiled expression.
     *
     * @param exp Expression
     * @param calc Compiled expression
     * @param evaluator Evaluator
     */
    public ExpCacheDescriptorImpl(Expression exp, Calc<?> calc, Evaluator evaluator) {
        this.calc = calc;
        this.exp = exp;
        computeDepends(calc, evaluator);
    }

    /**
     * Creates a descriptor.
     *
     * @param exp Expression
     * @param evaluator Evaluator
     */
    public ExpCacheDescriptorImpl(Expression exp, Evaluator evaluator) {
        this(exp, new StandardExpressionCompiler(evaluator, null));
    }

    /**
     * Creates a descriptor.
     *
     * @param exp Expression
     * @param compiler Compiler
     */
    public ExpCacheDescriptorImpl(Expression exp, ExpressionCompiler compiler) {
        this.exp = exp;

        // Compile expression.
        Calc<?> calcInner = compiler.compile(exp);
        if (calcInner == null) {
            // now allow conversions
            calcInner = compiler.compile(exp, CompileRequest.as(null, ResultStyle.ANY_ONLY));
        }
        this.calc = calcInner;

        // Compute list of dependent dimensions.
        computeDepends(calcInner, compiler.getEvaluator());
    }

    private void computeDepends(Calc<?> calc, Evaluator evaluator) {
        final List<Integer> ordinalList = new ArrayList<>();
        final Member[] members = evaluator.getMembers();
        for (int i = 0; i < members.length; i++) {
            Hierarchy hierarchy = members[i].getHierarchy();
            if (calc.dependsOn(hierarchy)) {
                ordinalList.add(i);
            }
        }
        dependentHierarchyOrdinals = new int[ordinalList.size()];
        for (int i = 0; i < dependentHierarchyOrdinals.length; i++) {
            dependentHierarchyOrdinals[i] = ordinalList.get(i);
        }
    }
    @Override
    public Expression getExp() {
        return exp;
    }

	@Override
	public Calc<?> getCalc() {
        return calc;
    }

    @Override
    public Object evaluate(Evaluator evaluator) {
        return calc.evaluate(evaluator);
    }

    /**
     * Returns the ordinals of the hierarchies which this expression is
     * dependent upon. When the cache descriptor is used to generate a cache
     * key, the key will consist of a member from each of these hierarchies.
     */
    @Override
    public int[] getDependentHierarchyOrdinals() {
        return dependentHierarchyOrdinals;
    }

}
