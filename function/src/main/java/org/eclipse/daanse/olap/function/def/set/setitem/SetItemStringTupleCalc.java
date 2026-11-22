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
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleCalc;

public class SetItemStringTupleCalc extends AbstractNestedTupleCalc {

    private final TupleListCalc tupleListCalc;
    private final StringCalc[] stringCalcs;
    
    protected SetItemStringTupleCalc(Type type, Calc<?>[] calcs, final TupleListCalc tupleListCalc, final StringCalc[] stringCalcs) {
        super(type, calcs);
        this.tupleListCalc = tupleListCalc;
        this.stringCalcs = stringCalcs;
    }
    
    @Override
    public Member[] evaluateInternal(Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        final TupleList list;
        try {
            evaluator.setNonEmpty(false);
            list = tupleListCalc.evaluate(evaluator);
            assert list != null;
        } finally {
            evaluator.restore(savepoint);
        }
        try {
            String[] results = new String[stringCalcs.length];
            for (int i = 0; i < stringCalcs.length; i++) {
                results[i] =
                    stringCalcs[i].evaluate(evaluator);
            }
            listLoop:
            for (List<Member> members : list) {
                for (int j = 0; j < results.length; j++) {
                    String result = results[j];
                    final Member member = members.get(j);
                    if (!SetItemFunDef.matchMember(member, result)) {
                        continue listLoop;
                    }
                }
                // All members match. Return the current one.
                return members.toArray(
                    new Member[members.size()]);
            }
        } finally {
            evaluator.restore(savepoint);
        }
        // We use 'null' to represent the null tuple. Don't
        // know why.
        return null;
    }


}
