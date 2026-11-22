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
package org.eclipse.daanse.olap.function.def.vba.formatcurrency;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class FormatCurrencyCalc extends AbstractNestedStringCalc {
    protected FormatCurrencyCalc(Type type, Calc<?> expressionCalc, IntegerCalc numDigitsAfterDecimalCalc,
            IntegerCalc includeLeadingDigitCalc, IntegerCalc useParensForNegativeNumbersCalc,
            IntegerCalc groupDigitsCalc) {
        super(type, expressionCalc, numDigitsAfterDecimalCalc, includeLeadingDigitCalc, useParensForNegativeNumbersCalc,
                groupDigitsCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Object expression = getChildCalc(0, Calc.class).evaluate(evaluator);
        Integer numDigitsAfterDecimal = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        Integer includeLeadingDigit = getChildCalc(2, IntegerCalc.class).evaluate(evaluator);
        Integer useParensForNegativeNumbers = getChildCalc(3, IntegerCalc.class).evaluate(evaluator);
        Integer groupDigits = getChildCalc(4, IntegerCalc.class).evaluate(evaluator);

        return formatCurrency(expression, numDigitsAfterDecimal, includeLeadingDigit, useParensForNegativeNumbers,
                groupDigits);
    }

    public static String formatCurrency(Object expression, int numDigitsAfterDecimal, int includeLeadingDigit,
            int useParensForNegativeNumbers, int groupDigits) {
        DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance();
        if (numDigitsAfterDecimal != -1) {
            format.setMaximumFractionDigits(numDigitsAfterDecimal);
            format.setMinimumFractionDigits(numDigitsAfterDecimal);
        }
        if (includeLeadingDigit != -2) {
            if (includeLeadingDigit != 0) {
                format.setMinimumIntegerDigits(1);
            } else {
                format.setMinimumIntegerDigits(0);
            }
        }
        if (useParensForNegativeNumbers != -2) {
            // todo: implement.
            // This will require tweaking of the currency expression
        }

        if (groupDigits != -2 && groupDigits != 0) {
            format.setGroupingUsed(false);
        } else {
            format.setGroupingUsed(true);
        }
        return format.format(expression);
    }

}
