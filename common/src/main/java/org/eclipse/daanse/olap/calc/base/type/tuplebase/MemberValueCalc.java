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
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.ScalarType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;

/**
 * Expression which evaluates a few member expressions,
 * sets the dimensional context to the result of those expressions,
 * then yields the value of the current measure in the current
 * dimensional context.
 *
 * The evaluator's context is preserved.
 *
 * Note that a MemberValueCalc with 0 member expressions is equivalent to a
 * {@link org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc}; see also {@link org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleValueCalc}.
 *
 * @author jhyde
 * @since Sep 27, 2005
 */
public class MemberValueCalc extends AbstractNestedUnknownCalc {
    private final MemberCalc memberCalc;
    private final boolean nullCheck;

    /**
     * Creates a MemberArrayValueCalc.
     *
     * Clients outside this package should use the
     * {@link MemberValueCalc#create(mondrian.olap.Exp,
     * org.eclipse.daanse.olap.api.calc.MemberCalc[], boolean)}
     * factory method.
     *
     * @param type Type 
     * @param memberCalc Compiled expression
     * @param nullCheck Whether to check for null values due to non-joining
     *     dimensions in a virtual cube
     */
    public MemberValueCalc(Type type, MemberCalc memberCalc, boolean nullCheck) {
        super(type);
        this.nullCheck = nullCheck;
        if (!(type instanceof ScalarType)) {
            throw new IllegalArgumentException("Invalid type in MemberValueCalc");
        }
        this.memberCalc = memberCalc;
    }

    /**
     * Creates a {@link CurrentValueUnknownCalc}, {@link MemberValueCalc} or
     * {@link MemberArrayValueCalc}.
     *
     * @param type Type 
     * @param memberCalcs Array of members to evaluate
     * @param nullCheck Whether to check for null values due to non-joining
     *     dimensions in a virtual cube
     * @return Compiled expression to evaluate each member expression, set
     *   evaluator context to each resulting member, then evaluate the current
     *   context
     */
    public static AbstractNestedUnknownCalc create(
    		 Type type,
            MemberCalc[] memberCalcs,
            boolean nullCheck)
    {
        return switch (memberCalcs.length) {
        case 0 -> new CurrentValueUnknownCalc(type);
        case 1 -> new MemberValueCalc(type, memberCalcs[0], nullCheck);
        default -> new MemberArrayValueCalc(type, memberCalcs, nullCheck);
        };
    }

    @Override
    public Object evaluateInternal(Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        try {
            final Member member = memberCalc.evaluate(evaluator);
            if (member == null
                    || member.isNull())
            {
                return null;
            }
            evaluator.setContext(member);
            if (nullCheck
                    && evaluator.needToReturnNullForUnrelatedDimension(
                            new Member[] {member}))
            {
                return null;
            }
            return evaluator.evaluateCurrent();
        } finally {
            evaluator.restore(savepoint);
        }
    }

    @Override
    public Calc[] getChildCalcs() {
        return new MemberCalc[] {memberCalc};
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        if (super.dependsOn(hierarchy)) {
            return true;
        }
        // If the expression definitely includes the dimension (in this
        // case, that means it is a member of that dimension) then we
        // do not depend on the dimension. For example, the scalar value of
        //   [Store].[USA]
        // does not depend on [Store].
        //
        // If the dimensionality of the expression is unknown, then the
        // expression MIGHT include the dimension, so to be safe we have to
        // say that it depends on the given dimension. For example,
        //   Dimensions(3).CurrentMember.Parent
        // may depend on [Store].
        return !memberCalc.getType().usesHierarchy(hierarchy, true);
    }
}
