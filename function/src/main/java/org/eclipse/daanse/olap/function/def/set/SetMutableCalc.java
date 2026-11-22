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

import java.util.List;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleIteratorCalc;

public class SetMutableCalc extends AbstractNestedTupleIteratorCalc {

    private final TupleListCalc tupleListCalc;
    
    protected SetMutableCalc(Type type, Calc<?> calc, final TupleListCalc tupleListCalc) {
        super(type, calc);
        this.tupleListCalc = tupleListCalc;
    }

    // name "Sublist..."
    @Override
    public TupleIterable evaluateInternal(Evaluator evaluator) {
        TupleList list = tupleListCalc.evaluate(evaluator);
        TupleList result = list.copyList(list.size());
        // Add only tuples which are not null. Tuples with
        // any null members are considered null.
        list: for (List<Member> members : list) {
            for (Member member : members) {
                if (member == null || member.isNull()) {
                    continue list;
                }
            }
            result.add(members);
        }
        return result;
    }

}
