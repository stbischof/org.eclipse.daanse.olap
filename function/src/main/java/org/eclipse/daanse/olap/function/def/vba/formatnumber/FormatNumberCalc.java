/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.vba.formatnumber;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class FormatNumberCalc extends AbstractNestedStringCalc {
    protected FormatNumberCalc(Type type, Calc<?> expressionCalc, IntegerCalc numDigitsAfterDecimalCalc,
            IntegerCalc includeLeadingDigitCalc, IntegerCalc useParensForNegativeNumbersCalc,
            IntegerCalc groupDigitsCalc) {
        super(type, expressionCalc, numDigitsAfterDecimalCalc, includeLeadingDigitCalc, useParensForNegativeNumbersCalc, groupDigitsCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Object expression = getChildCalc(0, Calc.class).evaluate(evaluator);
        Integer numDigitsAfterDecimal = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        Integer includeLeadingDigit = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        Integer useParensForNegativeNumbers = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        Integer groupDigits = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        return formatNumber(
                expression,
                numDigitsAfterDecimal,
                includeLeadingDigit /* default usedefault */,
                useParensForNegativeNumbers /* default UseDefault */,
                groupDigits);
    }

    public static String formatNumber(
            Object expression,
            int numDigitsAfterDecimal /* default -1 */,
            int includeLeadingDigit /* default usedefault */,
            int useParensForNegativeNumbers /* default UseDefault */,
            int groupDigits /* default UseDefault */)
        {
            NumberFormat format = NumberFormat.getNumberInstance();
            if (numDigitsAfterDecimal != -1) {
                format.setMaximumFractionDigits(numDigitsAfterDecimal);
                format.setMinimumFractionDigits(numDigitsAfterDecimal);
            }

            if (includeLeadingDigit != -1) {
                if (includeLeadingDigit != 0) {
                    // true
                    format.setMinimumIntegerDigits(1);
                } else {
                    format.setMinimumIntegerDigits(0);
                }
            }

            if (useParensForNegativeNumbers != -1) {
                if (useParensForNegativeNumbers != 0) {
                    DecimalFormat dformat = (DecimalFormat)format;
                    dformat.setNegativePrefix("(");
                    dformat.setNegativeSuffix(")");
                } else {
                    DecimalFormat dformat = (DecimalFormat)format;
                    dformat.setNegativePrefix(
                        "" + dformat.getDecimalFormatSymbols().getMinusSign());
                    dformat.setNegativeSuffix("");
                }
            }

            if (groupDigits != -1) {
                format.setGroupingUsed(groupDigits != 0);
            }

            return format.format(expression);
        }

}
