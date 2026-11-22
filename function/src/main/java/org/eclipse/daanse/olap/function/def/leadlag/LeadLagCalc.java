/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.leadlag;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;

public class LeadLagCalc extends AbstractNestedMemberCalc {

    private boolean lag;

    protected LeadLagCalc(Type type, final MemberCalc memberCalc, final IntegerCalc integerCalc, final boolean lag) {
        super(type, memberCalc, integerCalc);
        this.lag = lag;
    }

    @Override
    public Member evaluateInternal(Evaluator evaluator) {
        Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        Integer nBoxed = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        int n = nBoxed == null ? 0 : nBoxed;
        if (lag) {
            if (n == Integer.MIN_VALUE) {
                // Bump up lagValue by one, otherwise -n (used
                // in the getLeadMember call below) is out of
                // range because Integer.MAX_VALUE ==
                // -(Integer.MIN_VALUE + 1).
                n += 1;
            }

            n = -n;
        }
        return evaluator.getCatalogReader().getLeadMember(member, n);
    }
}
