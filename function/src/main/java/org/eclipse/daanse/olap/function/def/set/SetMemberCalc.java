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
package org.eclipse.daanse.olap.function.def.set;

import java.util.Collections;

import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleIteratorCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;

public class SetMemberCalc extends AbstractNestedTupleIteratorCalc {

    public SetMemberCalc(Type type, MemberCalc memberCalc) {
        super(type, memberCalc);
    }

    // name "Sublist..."// name "Sublist..."
    @Override
    public TupleIterable evaluateInternal(Evaluator evaluator) {
        final Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        return member == null ? TupleCollections.createList(1)
                : new UnaryTupleList(Collections.singletonList(member));
    }

}
