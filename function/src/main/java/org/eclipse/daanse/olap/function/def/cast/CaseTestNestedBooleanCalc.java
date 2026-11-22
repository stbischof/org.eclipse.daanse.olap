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
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedBooleanCalc;

public class CaseTestNestedBooleanCalc extends AbstractNestedBooleanCalc {

    private final BooleanCalc[] conditionCalcs;
    private final Calc<?>[] exprCalcs;
    private final Calc<?> defaultCalc;

    protected CaseTestNestedBooleanCalc(Type type, final BooleanCalc[] conditionCalcs, final Calc<?>[] exprCalcs,
            Calc<?> defaultCalc, Calc<?>[] calcs) {
        super(type, calcs);
        this.conditionCalcs = conditionCalcs;
        this.exprCalcs = exprCalcs;
        this.defaultCalc = defaultCalc;
    }

    @Override
    public Boolean evaluateInternal(Evaluator evaluator) {
        for (int i = 0; i < conditionCalcs.length; i++) {
            if (conditionCalcs[i].evaluate(evaluator)) {
                return (Boolean) exprCalcs[i].evaluate(evaluator);
            }
        }
        return (Boolean) defaultCalc.evaluate(evaluator);
    }

}
