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
package org.eclipse.daanse.olap.function.def.set.setitem;

import java.util.List;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;

public class SetItemStringCalc extends AbstractNestedMemberCalc {

    private final TupleListCalc tupleListCalc;
    private final StringCalc[] stringCalcs;
    private final Member nullMember;

    protected SetItemStringCalc(Type type, Calc<?>[] calcs, TupleListCalc tupleListCalc, final StringCalc[] stringCalcs,
            final Member nullMember) {
        super(type, calcs);
        this.tupleListCalc = tupleListCalc;
        this.stringCalcs = stringCalcs;
        this.nullMember = nullMember;
    }

    @Override
    public Member evaluateInternal(Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        final List<Member> list;
        try {
            evaluator.setNonEmpty(false);
            list = tupleListCalc.evaluate(evaluator).slice(0);
            assert list != null;
        } finally {
            evaluator.restore(savepoint);
        }
        try {
            final String result = stringCalcs[0].evaluate(evaluator);
            for (Member member : list) {
                if (SetItemFunDef.matchMember(member, result)) {
                    return member;
                }
            }
            return nullMember;
        } finally {
            evaluator.restore(savepoint);
        }
    }
}
