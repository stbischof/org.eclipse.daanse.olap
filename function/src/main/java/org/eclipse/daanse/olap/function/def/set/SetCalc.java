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

import java.util.Arrays;

import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleIteratorCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.ListTupleList;

public class SetCalc extends AbstractNestedTupleIteratorCalc{

    protected SetCalc(Type type, final TupleCalc tupleCalc) {
        super(type, tupleCalc);
    }

    @Override
    public TupleIterable evaluateInternal(Evaluator evaluator) {
        TupleCalc tupleCalc = getChildCalc(0, TupleCalc.class);
        final Member[] members = tupleCalc.evaluate(evaluator);
        return new ListTupleList(tupleCalc.getType().getArity(), Arrays.asList(members));
    }

}
