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
package org.eclipse.daanse.olap.function.def.udf.in;

import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedBooleanCalc;

public class InCalc extends AbstractNestedBooleanCalc {

    protected InCalc(Type type, MemberCalc memberCalc, TupleListCalc tupleListCalc) {
        super(type, memberCalc, tupleListCalc);
    }

    @Override
    public Boolean evaluateInternal(Evaluator evaluator) {
        final MemberCalc memberCalc = getChildCalc(0, MemberCalc.class);
        final TupleListCalc tupleListCalc = getChildCalc(1, TupleListCalc.class);
        TupleList tupleList = tupleListCalc.evaluate(evaluator);
        Member member = memberCalc.evaluate(evaluator);
        //List arg1 = (List) arguments[1].evaluate(evaluator);

        for (Member m : tupleList.slice(0)) {
            if (member.getUniqueName().equals(
                    m.getUniqueName()))
            {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;

    }

}
