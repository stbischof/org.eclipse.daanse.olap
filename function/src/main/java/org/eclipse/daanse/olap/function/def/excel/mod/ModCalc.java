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
package org.eclipse.daanse.olap.function.def.excel.mod;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedDoubleCalc;
import org.eclipse.daanse.olap.exceptions.InvalidArgumentException;

public class ModCalc extends AbstractNestedDoubleCalc {

    protected ModCalc(Type type, Calc<?> calc0, Calc<?> calc1) {
        super(type, calc0, calc1);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Object first = getChildCalc(0, Calc.class).evaluate(evaluator);
        Object second = getChildCalc(1, Calc.class).evaluate(evaluator);
        double iFirst;
        if (!(first instanceof Number numberFirst)) {
            throw new InvalidArgumentException(
                new StringBuilder("Invalid parameter. ")
                .append("first parameter ").append(first)
                .append(" of Mod function must be of type number").toString());
        } else {
            iFirst = numberFirst.doubleValue();
        }
        double iSecond;
        if (!(second instanceof Number numberSecond)) {
            throw new InvalidArgumentException(
                new StringBuilder("Invalid parameter. ")
                .append("second parameter ").append(second)
                .append(" of Mod function must be of type number").toString());
        } else {
            iSecond = numberSecond.doubleValue();
        }
        // Use formula "mod(n, d) = n - d * int(n / d)".
        if (iSecond == 0) {
            throw new ArithmeticException("/ by zero");
        }
        return iFirst - iSecond * intNative(iFirst / iSecond);
    }

    /**
     * Equivalent of the toInt function on the native 'double' type.
     * Not an MDX function.
     *
     * @param dv Double value
     * @return Value rounded towards negative infinity
     */
    public static int intNative(double dv) {
        int v = (int) dv;
        if (v < 0 && v > dv) {
            v--;
        }
        return v;
    }

}
