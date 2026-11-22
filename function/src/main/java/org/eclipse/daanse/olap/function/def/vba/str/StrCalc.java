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
package org.eclipse.daanse.olap.function.def.vba.str;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;
import org.eclipse.daanse.olap.exceptions.InvalidArgumentException;

public class StrCalc extends AbstractNestedStringCalc {

    protected StrCalc(Type type, Calc<?> doubleCalc) {
        super(type, doubleCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Object number = getChildCalc(0, Calc.class).evaluate(evaluator);
        // When numbers are converted to strings, a leading space is always
        // reserved for the sign of number. If number is positive, the returned
        // string contains a leading space and the plus sign is implied.
        //
        // Use the Format function to convert numeric values you want formatted
        // as dates, times, or currency or in other user-defined formats.
        // Unlike Str, the Format function doesn't include a leading space for
        // the sign of number.
        //
        // Note The Str function recognizes only the period (.) as a valid
        // decimal separator. When different decimal separators may be used
        // (for example, in international applications), use CStr to convert a
        // number to a string.
        if (number instanceof Number num) {
            if (num.doubleValue() >= 0) {
                return " " + number.toString();
            } else {
                return number.toString();
            }
        } else {
            throw new InvalidArgumentException(
                new StringBuilder("Invalid parameter. ")
                    .append("number parameter ").append(number)
                    .append(" of Str function must be ")
                    .append("of type number").toString());
        }
    }

}
