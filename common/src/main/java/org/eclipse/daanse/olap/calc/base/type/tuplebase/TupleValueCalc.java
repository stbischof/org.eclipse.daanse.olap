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

package org.eclipse.daanse.olap.calc.base.type.tuplebase;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.TupleMemberCalcs;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;

/**
 * Expression which evaluates a tuple expression,
 * sets the dimensional context to the result of that expression,
 * then yields the value of the current measure in the current
 * dimensional context.
 *
 * The evaluator's context is preserved.
 *
 * @see org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc
 * @see org.eclipse.daanse.olap.calc.base.type.tuplebase.MemberValueCalc
 *
 * @author jhyde
 * @since Sep 27, 2005
 */
public class TupleValueCalc extends AbstractNestedUnknownCalc {
    private final TupleCalc tupleCalc;
    private final boolean nullCheck;

    /**
     * Creates a TupleValueCalc.
     *
     * @param type Type
     * @param tupleCalc Compiled expression to evaluate the tuple
     * @param nullCheck Whether to check for null values due to non-joining
     *     dimensions in a virtual cube
     */
    public TupleValueCalc( Type type, TupleCalc tupleCalc, boolean nullCheck) {
        super(type);
        this.tupleCalc = tupleCalc;
        this.nullCheck = nullCheck;
    }

    @Override
    public Object evaluateInternal(Evaluator evaluator) {
        final Member[] members = tupleCalc.evaluate(evaluator);
        if ((members == null) || (nullCheck
                && evaluator.needToReturnNullForUnrelatedDimension(members)))
        {
            return null;
        }

        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setContext(members);
            return evaluator.evaluateCurrent();
        } finally {
            evaluator.restore(savepoint);
        }
    }

    @Override
    public Calc[] getChildCalcs() {
        return new Calc[] {tupleCalc};
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        if (super.dependsOn(hierarchy)) {
            return true;
        }
        for (final Type type : ((TupleType) tupleCalc.getType()).elementTypes) {
            // If the expression definitely includes the dimension (in this
            // case, that means it is a member of that dimension) then we
            // do not depend on the dimension. For example, the scalar value of
            //   ([Store].[USA], [Gender].[F])
            // does not depend on [Store].
            //
            // If the dimensionality of the expression is unknown, then the
            // expression MIGHT include the dimension, so to be safe we have to
            // say that it depends on the given dimension. For example,
            //   (Dimensions(3).CurrentMember.Parent, [Gender].[F])
            // may depend on [Store].
            if (type.usesHierarchy(hierarchy, true)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Optimizes the scalar evaluation of a tuple. It evaluates the members
     * of the tuple, sets the context to these members, and evaluates the
     * scalar result in one step, without generating a tuple.
     *
     * This is useful when evaluating calculated members:
     *
     * WITH MEMBER [Measures].[Sales last quarter]
     *   AS ' ([Measures].[Unit Sales], [Time].PreviousMember) '
     *
     *
     *
     * @return optimized expression
     */
    public Calc optimize() {
        if (tupleCalc instanceof TupleMemberCalcs calc) {
            return MemberValueCalc.create(
                    getType(),
                    calc.getMemberCalcs(),
                    nullCheck);
        }
        return this;
    }
}
