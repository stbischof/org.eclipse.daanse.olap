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
package org.eclipse.daanse.olap.function.def.vba.monthname;

import java.text.DateFormatSymbols;
import java.util.Locale;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class MonthNameCalc extends AbstractNestedStringCalc {

    private static final DateFormatSymbols DATE_FORMAT_SYMBOLS =
            new DateFormatSymbols(Locale.getDefault());

    protected MonthNameCalc(Type type, final IntegerCalc monthCalc, final BooleanCalc abbreviateCalc) {
        super(type, monthCalc, abbreviateCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Integer month = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
        Boolean abbreviate = getChildCalc(1, BooleanCalc.class).evaluate(evaluator);
        return monthName(month, abbreviate);
    }

    public static String monthName(int month, boolean abbreviate) {
        // VB months are 1-based, Java months are 0-based
        --month;
        return (abbreviate ? getDateFormatSymbols().getShortMonths()
                : getDateFormatSymbols().getMonths())[month];
    }
    
    /**
     * Returns an instance of {@link DateFormatSymbols} for the current locale.
     *
     *
     * Todo: inherit locale from connection.
     *
     * @return a DateFormatSymbols object
     */
    private static DateFormatSymbols getDateFormatSymbols() {
        // We would use DataFormatSymbols.getInstance(), but it is only
        // available from JDK 1.6 onwards.
        return DATE_FORMAT_SYMBOLS;
    }

}
