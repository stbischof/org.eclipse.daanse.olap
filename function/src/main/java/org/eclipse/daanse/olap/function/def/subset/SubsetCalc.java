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
package org.eclipse.daanse.olap.function.def.subset;

import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;

public class SubsetCalc extends AbstractNestedTupleListCalc{

    public SubsetCalc(Type type, TupleListCalc tupleListCalc, IntegerCalc startCalc, IntegerCalc countCalc) {
        super(type, tupleListCalc, startCalc, countCalc);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleListCalc tupleListCalc = getChildCalc(0, TupleListCalc.class);
        IntegerCalc startCalc = getChildCalc(1, IntegerCalc.class);
        IntegerCalc countCalc = getChildCalc(2, IntegerCalc.class);
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            final TupleList list = tupleListCalc.evaluate(evaluator);
            final Integer start = startCalc.evaluate(evaluator);
            if (start == null) {
                return TupleCollections.emptyList(list.getArity());
            }
            int end;
            if (countCalc != null) {
                final Integer count = countCalc.evaluate(evaluator);
                if (count == null) {
                    return TupleCollections.emptyList(list.getArity());
                }
                end = start + count;
            } else {
                end = list.size();
            }
            if (end > list.size()) {
                end = list.size();
            }
            if (start >= end || start < 0) {
                return TupleCollections.emptyList(list.getArity());
            }
            if (start == 0 && end == list.size()) {
                return list;
            }
            assert 0 <= start;
            assert start < end;
            assert end <= list.size();
            return list.subList(start, end);
        } finally {
            evaluator.restore(savepoint);
        }
    }

}
