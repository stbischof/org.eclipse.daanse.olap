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
package org.eclipse.daanse.olap.function.def.settostr;

import java.util.List;

import org.eclipse.daanse.olap.api.calc.tuple.TupleCursor;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class SetToStrCalc extends AbstractNestedStringCalc {

    protected SetToStrCalc(Type type, final TupleListCalc tupleListCalc) {
        super(type, tupleListCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        final TupleList list = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
        if (list.getArity() == 1) {
            return memberSetToStr(list.slice(0));
        } else {
            return tupleSetToStr(list);
        }
    }

    private String memberSetToStr(List<Member> list) {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        int k = 0;
        for (Member member : list) {
            if (k++ > 0) {
                buf.append(", ");
            }
            buf.append(member.getUniqueName());
        }
        buf.append("}");
        return buf.toString();
    }

    private String tupleSetToStr(TupleList list) {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        int k = 0;
        Member[] members = new Member[list.getArity()];
        final TupleCursor cursor = list.tupleCursor();
        while (cursor.forward()) {
            if (k++ > 0) {
                buf.append(", ");
            }
            cursor.currentToArray(members, 0);
            FunUtil.appendTuple(buf, members);
        }
        buf.append("}");
        return buf.toString();
    }

}
