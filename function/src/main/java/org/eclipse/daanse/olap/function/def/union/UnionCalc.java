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
package org.eclipse.daanse.olap.function.def.union;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.fun.FunUtil;

public class UnionCalc extends AbstractNestedTupleListCalc {

    private final boolean all;

    public UnionCalc(Type type, TupleListCalc listCalc0, TupleListCalc listCalc1, final boolean all) {
        super(type, listCalc0, listCalc1);
        this.all = all;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleListCalc listCalc0 = getChildCalc(0, TupleListCalc.class);
        TupleListCalc listCalc1 = getChildCalc(1, TupleListCalc.class);
        TupleList list0 = listCalc0.evaluate(evaluator);
        TupleList list1 = listCalc1.evaluate(evaluator);
        return union(list0, list1, all);
    }

    public TupleList union(TupleList list0, TupleList list1, final boolean all) {
        assert list0 != null;
        assert list1 != null;
        if (all) {
            if (list0.isEmpty()) {
                return list1;
            }
            if (list1.isEmpty()) {
                return list0;
            }
            TupleList result = TupleCollections.createList(list0.getArity());
            result.addAll(list0);
            result.addAll(list1);
            return result;
        } else {
            Set<List<Member>> added = new HashSet<>();
            TupleList result = TupleCollections.createList(list0.getArity());
            FunUtil.addUnique(result, list0, added);
            FunUtil.addUnique(result, list1, added);
            return result;
        }
    }

}
