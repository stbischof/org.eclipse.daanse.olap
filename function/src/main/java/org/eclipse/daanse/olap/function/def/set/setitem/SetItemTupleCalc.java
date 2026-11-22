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
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleCalc;

public class SetItemTupleCalc extends AbstractNestedTupleCalc{

    private final TupleListCalc tupleListCalc;
    private final IntegerCalc indexCalc;
    private final Member[] nullTuple;
    
    public SetItemTupleCalc(Type type, Calc<?>[] calcs, TupleListCalc tupleListCalc, IntegerCalc indexCalc,
            Member[] nullTuple) {
        super(type, calcs);
        this.tupleListCalc = tupleListCalc;
        this.indexCalc = indexCalc;
        this.nullTuple = nullTuple;
    }

    @Override
    public Member[] evaluateInternal(Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        final TupleList list;
        try {
            evaluator.setNonEmpty(false);
            list =
                tupleListCalc.evaluate(evaluator);
        } finally {
            evaluator.restore(savepoint);
        }
        assert list != null;
        try {
            final Integer index =
                indexCalc.evaluate(evaluator);
            int listSize = list.size();
            if (index >= listSize || index < 0) {
                return nullTuple;
            } else {
                final List<Member> members =
                    list.get(index);
                return members.toArray(
                    new Member[members.size()]);
            }
        } finally {
            evaluator.restore(savepoint);
        }
    }

}

