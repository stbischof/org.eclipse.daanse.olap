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
package org.eclipse.daanse.olap.function.def.vba.cbool;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedBooleanCalc;
import org.eclipse.daanse.olap.function.def.vba.cint.CIntCalc;

public class CBoolCalc extends AbstractNestedBooleanCalc {

    protected CBoolCalc(Type type, Calc<?> doubleCalc) {
        super(type, doubleCalc);
    }

    @Override
    public Boolean evaluateInternal(Evaluator evaluator) {
        Object expression = getChildCalc(0, Calc.class).evaluate(evaluator);
        if (expression instanceof Boolean bool) {
            return bool;
        } else {
            int i = CIntCalc.cInt(expression);
            return i != 0;
        }
    }

}
