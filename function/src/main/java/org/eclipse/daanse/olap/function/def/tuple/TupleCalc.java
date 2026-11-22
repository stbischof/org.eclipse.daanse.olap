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
package org.eclipse.daanse.olap.function.def.tuple;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.TupleMemberCalcs;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleCalc;

public class TupleCalc extends AbstractNestedTupleCalc implements TupleMemberCalcs {
    private final MemberCalc[] memberCalcs;

    public TupleCalc(ResolvedFunCall call, MemberCalc[] memberCalcs) {
        super(call.getType(), memberCalcs);
        this.memberCalcs = memberCalcs;
    }

    @Override
    public Member[] evaluateInternal(Evaluator evaluator) {
        final Member[] members = new Member[memberCalcs.length];
        for (int i = 0; i < members.length; i++) {
            final Member member =
                members[i] =
                memberCalcs[i].evaluate(evaluator);
            if (member == null || member.isNull()) {
                return null;
            }
        }
        return members;
    }

    public MemberCalc[] getMemberCalcs() {
        return memberCalcs;
    }
}
