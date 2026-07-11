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
package org.eclipse.daanse.olap.function.def.udf.lastnonempty;

import java.util.List;

import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.common.Util;

public class LastNonEmptyCalc extends AbstractProfilingNestedMemberCalc {

    protected LastNonEmptyCalc(Type type, TupleListCalc tupleListCalc, Calc<?> memberCalc) {
        super(type, tupleListCalc, memberCalc);
    }

    @Override
    public Member evaluateInternal(Evaluator evaluator) {
        final TupleListCalc tupleListCalc = getChildCalc(0, TupleListCalc.class);
        final Calc<?> memberCalc = getChildCalc(1, Calc.class);
        TupleList tupleList = tupleListCalc.evaluate(evaluator);
        final List<Member> memberList = tupleList.slice(0);
        int nullCount = 0;
        int missCount = 0;
        for (int i = memberList.size() - 1; i >= 0; --i) {
            Member member = memberList.get(i);
            // Create an evaluator with the member as its context.
            Evaluator subEvaluator = evaluator.push();
            subEvaluator.setContext(member);
            int missCountBefore = subEvaluator.getMissCount();
            final Object o = memberCalc.evaluate(subEvaluator);
            int missCountAfter = subEvaluator.getMissCount();
            if (Util.isNull(o)) {
                ++nullCount;
                continue;
            }
            if (missCountAfter > missCountBefore) {
                // There was a cache miss while evaluating the expression, so
                // the result is bogus. It would be a mistake to give up after
                // one cache miss, because then it would take us N
                // evaluate/fetch passes to move back through N members, which
                // is way too many.
                //
                // Carry on until we have seen as many misses as we have seen
                // null cells. The effect of this policy is that each pass
                // examines twice as many cells as the previous pass. Thus
                // we can move back through N members in log2(N) passes.
                ++missCount;
                if (missCount < 2 * nullCount + 1) {
                    continue;
                }
            }
            if (o == NotLoaded.INSTANCE) {
                // Value is not in the cache yet, so we don't know whether
                // it will be empty. Carry on...
                continue;
            }
            //if (o instanceof RuntimeException runtimeException) {
            //    return runtimeException;
            //}
            return member;
        }
        // Not found. Return the hierarchy's 'null member'.
        // It is possible that a MemberType has a Dimension but
        // no hierarchy, so we have to just get the type's Dimension's
        // default hierarchy and return it's null member.
        final Hierarchy hierarchy = tupleListCalc.getType().getHierarchy();
        return (hierarchy == null)
            ? tupleListCalc.getType().getDimension()
                .getHierarchies().getFirst().getNullMember()
            : hierarchy.getNullMember();

    }

}
