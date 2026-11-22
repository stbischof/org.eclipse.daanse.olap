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
package org.eclipse.daanse.olap.function.def.generate;

import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleCursor;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.util.CancellationChecker;

public class GenerateStringCalc extends AbstractNestedStringCalc {
    private final StringCalc sepCalc;

    public GenerateStringCalc(Type type, TupleIteratorCalc tupleIteratorCalc, StringCalc stringCalc,
            StringCalc sepCalc) {
        super(type, tupleIteratorCalc, stringCalc);
        this.sepCalc = sepCalc;
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        try {
            StringBuilder buf = new StringBuilder();
            int k = 0;
            final TupleIterable iter11 = (TupleIterable) getChildCalc(0, TupleIteratorCalc.class).evaluate(evaluator);
            final TupleCursor cursor = iter11.tupleCursor();
            int currentIteration = 0;
            Execution execution = evaluator.getQuery().getStatement().getCurrentExecution();
            while (cursor.forward()) {
                CancellationChecker.checkCancelOrTimeout(currentIteration++, execution);
                cursor.setContext(evaluator);
                if (k++ > 0) {
                    String sep = sepCalc.evaluate(evaluator);
                    buf.append(sep);
                }
                final String result2 = getChildCalc(1, StringCalc.class).evaluate(evaluator);
                buf.append(result2);
            }
            return buf.toString();
        } finally {
            evaluator.restore(savepoint);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }
}