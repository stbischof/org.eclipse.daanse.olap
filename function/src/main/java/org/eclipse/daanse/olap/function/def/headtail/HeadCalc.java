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
package org.eclipse.daanse.olap.function.def.headtail;

import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;

public class HeadCalc extends AbstractNestedTupleListCalc {

    public HeadCalc(Type type, TupleListCalc tupleListCalc, IntegerCalc integerCalc) {
        super(type, tupleListCalc, integerCalc);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            TupleList list = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
            Integer count = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
            return head(count, list);
        } finally {
            evaluator.restore(savepoint);
        }
    }

    private TupleList head(final Integer count, final TupleList members) {
        assert members != null;
        if (count == null || count <= 0) {
            return TupleCollections.emptyList(members.getArity());
        }
        return members.subList(0, Math.min(count, members.size()));
    }

}
