/*
* Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.parallelperiod;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;
import org.eclipse.daanse.olap.exceptions.FunctionMbrAndLevelHierarchyMismatchException;
import org.eclipse.daanse.olap.fun.FunUtil;

public class ParallelPeriodCalc extends AbstractNestedMemberCalc{

    protected ParallelPeriodCalc(Type type, final MemberCalc memberCalc, final IntegerCalc lagValueCalc, final LevelCalc ancestorLevelCalc) {
        super(type, memberCalc, lagValueCalc, ancestorLevelCalc);
    }

    @Override
    public Member evaluateInternal(Evaluator evaluator) {
        Member member;
        MemberCalc memberCalc = getChildCalc(0, MemberCalc.class);
        Integer lagValue = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        LevelCalc ancestorLevelCalc = getChildCalc(2, LevelCalc.class);
        Level ancestorLevel;
        if (ancestorLevelCalc != null) {
            ancestorLevel = ancestorLevelCalc.evaluate(evaluator);
            if (memberCalc == null) {
                member =
                    evaluator.getContext(ancestorLevel.getHierarchy());
            } else {
                member = memberCalc.evaluate(evaluator);
            }
        } else {
            member = memberCalc.evaluate(evaluator);
            Member parent = member.getParentMember();
            if (parent == null) {
                // This is a root member,
                // so there is no parallelperiod.
                return member.getHierarchy().getNullMember();
            }
            ancestorLevel = parent.getLevel();
        }
        return parallelPeriod(
            member, ancestorLevel, evaluator, lagValue);
    }

    Member parallelPeriod(
            Member member,
            Level ancestorLevel,
            Evaluator evaluator,
            Integer lagValue)
        {
            // Now do some error checking.
            // The ancestorLevel and the member must be from the
            // same hierarchy.
            if (member.getHierarchy() != ancestorLevel.getHierarchy()) {
                new FunctionMbrAndLevelHierarchyMismatchException(
                    "ParallelPeriod",
                    ancestorLevel.getHierarchy().getUniqueName(),
                    member.getHierarchy().getUniqueName());
            }

            if (lagValue == Integer.MIN_VALUE) {
                // Bump up lagValue by one; otherwise -lagValue (used in
                // the getleadMember call below) is out of range because
                // Integer.MAX_VALUE == -(Integer.MIN_VALUE + 1)
                lagValue +=  1;
            }

            int distance =
                member.getLevel().getDepth()
                - ancestorLevel.getDepth();
            Member ancestor = FunUtil.ancestor(
                evaluator, member, distance, ancestorLevel);
            Member inLaw = evaluator.getCatalogReader()
                .getLeadMember(ancestor, -lagValue);
            return FunUtil.cousin(
                evaluator.getCatalogReader(), member, inLaw);
        }

}
