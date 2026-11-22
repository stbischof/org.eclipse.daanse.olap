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
package org.eclipse.daanse.olap.function.def.vba.hex;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;
import org.eclipse.daanse.olap.exceptions.InvalidArgumentException;

public class HexCalc extends AbstractNestedStringCalc {

    protected HexCalc(Type type, Calc<?> doubleCalc) {
        super(type, doubleCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Object number = getChildCalc(0, Calc.class).evaluate(evaluator);
        if (number instanceof Number num) {
            return Integer.toHexString(num.intValue())
                    .toUpperCase();
        } else {
            throw new InvalidArgumentException(
                new StringBuilder("Invalid parameter. ")
                    .append("number parameter ").append(number)
                    .append(" of Hex function must be of type number").toString());
        }
    }

}
