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
package org.eclipse.daanse.olap.function.def.set.extract;

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

public class ExtractCalc extends AbstractNestedTupleListCalc{

    private int outArity;
    private int[] extractedOrdinals;

    protected ExtractCalc(Type type, final int outArity, final int[] extractedOrdinals, final TupleListCalc tupleListCalc) {
        super(type, tupleListCalc);
        this.outArity = outArity;
        this.extractedOrdinals = extractedOrdinals;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        TupleList result = TupleCollections.createList(outArity);
        TupleList list = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
        Set<List<Member>> emittedTuples = new HashSet<>();
        for (List<Member> members : list.project(extractedOrdinals)) {
            if (emittedTuples.add(members)) {
                result.add(members);
            }
        }
        return result;
    }

}
