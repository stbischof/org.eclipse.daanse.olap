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
package org.eclipse.daanse.olap.function.def.vba.fix;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedIntegerCalc;
import org.eclipse.daanse.olap.exceptions.InvalidArgumentException;

public class FixCalc extends AbstractNestedIntegerCalc {

    protected FixCalc(Type type, Calc<?> doubleCalc) {
        super(type, doubleCalc);
    }

    @Override
    public Integer evaluateInternal(Evaluator evaluator) {
        Object number = getChildCalc(0, Calc.class).evaluate(evaluator);
        if (number instanceof Number num) {
            int v = num.intValue();
            double dv = num.doubleValue();
            if (v < 0 && v < dv) {
                v++;
            }
            return v;
        } else {
            throw new InvalidArgumentException(
                new StringBuilder("Invalid parameter. ")
                    .append("number parameter ").append(number)
                    .append(" of Int function must be of type number").toString());
        }
    }

}
