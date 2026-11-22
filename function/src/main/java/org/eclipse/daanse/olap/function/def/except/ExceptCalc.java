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
package org.eclipse.daanse.olap.function.def.except;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.ArrayTupleList;

public class ExceptCalc extends AbstractNestedTupleListCalc {

    public ExceptCalc(Type type, TupleListCalc listCalc0, TupleListCalc listCalc1) {
        super(type, listCalc0, listCalc1);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleList list0 = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
        if (list0.isEmpty()) {
            return list0;
        }
        TupleList list1 = getChildCalc(1, TupleListCalc.class).evaluate(evaluator);
        if (list1.isEmpty()) {
            return list0;
        }
        final Set<List<Member>> set1 = new HashSet<>(list1);
        final TupleList result = new ArrayTupleList(list0.getArity(), list0.size());
        for (List<Member> tuple1 : list0) {
            if (!set1.contains(tuple1)) {
                result.add(tuple1);
            }
        }
        return result;
    }

}
