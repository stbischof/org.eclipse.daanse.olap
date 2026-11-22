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
package org.eclipse.daanse.olap.function.def.tupleitem;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;

public class TupleItemMemberTypeCalc extends AbstractNestedMemberCalc{

    public TupleItemMemberTypeCalc(Type type, MemberCalc memberCalc, IntegerCalc indexCalc) {
        super(type, memberCalc, indexCalc);
    }

    @Override
    public Member evaluateInternal(Evaluator evaluator) {
        final Member member =
                getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        final Integer index =
                getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        if (index == null || index != 0) {
            return null;
        }
        return member;
    }

}
