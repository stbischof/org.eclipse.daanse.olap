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
package org.eclipse.daanse.olap.function.def.udf.val;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedDoubleCalc;

public class ValCalc extends AbstractNestedDoubleCalc {

    protected ValCalc(Type type, final DoubleCalc doubleCalc) {
        super(type, doubleCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Calc<?> doubleCalc = getChildCalc(0, Calc.class);
        Object arg = doubleCalc.evaluate(evaluator);
        if (arg instanceof Number) {
            return new Double(((Number) arg).doubleValue());
        } else {
            return new Double(0.0);
        }
    }

}
