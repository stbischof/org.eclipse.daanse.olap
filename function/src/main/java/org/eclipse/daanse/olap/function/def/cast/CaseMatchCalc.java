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
package org.eclipse.daanse.olap.function.def.cast;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;

public class CaseMatchCalc extends AbstractNestedUnknownCalc {

    private final Calc<?> valueCalc;
    private final Calc<?>[] exprCalcs;
    private final Calc<?>[] matchCalcs;
    private final Calc<?> defaultCalc;
    private final Calc<?>[] calcs;
    
    protected CaseMatchCalc(Type type, final Calc<?> valueCalc, Calc<?>[] exprCalcs, final Calc<?>[] matchCalcs, final Calc<?> defaultCalc, final Calc<?>[] calcs) {
        super(type);
        this.valueCalc = valueCalc;
        this.exprCalcs = exprCalcs;
        this.matchCalcs = matchCalcs;
        this.defaultCalc = defaultCalc;
        this.calcs = calcs;
    }

    @Override
    public Object evaluateInternal(Evaluator evaluator) {
        Object value = valueCalc.evaluate(evaluator);
        for (int i = 0; i < matchCalcs.length; i++) {
            Object match = matchCalcs[i].evaluate(evaluator);
            if (match.equals(value)) {
                return exprCalcs[i].evaluate(evaluator);
            }
        }
        return defaultCalc.evaluate(evaluator);
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return calcs;
    }

}
