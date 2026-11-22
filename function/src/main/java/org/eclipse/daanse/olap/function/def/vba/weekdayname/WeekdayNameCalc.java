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
package org.eclipse.daanse.olap.function.def.vba.weekdayname;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class WeekdayNameCalc extends AbstractNestedStringCalc {

    private static final DateFormatSymbols DATE_FORMAT_SYMBOLS =
            new DateFormatSymbols(Locale.getDefault());

    protected WeekdayNameCalc(Type type, final IntegerCalc weekdayCalc, final BooleanCalc abbreviateCalc, final IntegerCalc firstDayOfWeekCalc) {
        super(type, weekdayCalc, abbreviateCalc, firstDayOfWeekCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Integer weekday = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
        Boolean abbreviate = getChildCalc(1, BooleanCalc.class).evaluate(evaluator);
        Integer firstDayOfWeek = getChildCalc(2, IntegerCalc.class).evaluate(evaluator);
        return weekdayName(weekday, abbreviate, firstDayOfWeek);
    }

    public static String weekdayName(
            int weekday,
            boolean abbreviate,
            int firstDayOfWeek)
        {
            // Java and VB agree: SUNDAY = 1, ... SATURDAY = 7
            final Calendar calendar = Calendar.getInstance();
            if (firstDayOfWeek == 0) {
                firstDayOfWeek = calendar.getFirstDayOfWeek();
            }
            // compensate for start of week
            weekday += (firstDayOfWeek - 1);
            // bring into range 1..7
            weekday = (weekday - 1) % 7 + 1;
            if (weekday <= 0) {
                // negative numbers give negative modulo
                weekday += 7;
            }
            return
                (abbreviate
                 ? getDateFormatSymbols().getShortWeekdays()
                 : getDateFormatSymbols().getWeekdays())
                [weekday];
        }
    
    private static DateFormatSymbols getDateFormatSymbols() {
        // We would use DataFormatSymbols.getInstance(), but it is only
        // available from JDK 1.6 onwards.
        return DATE_FORMAT_SYMBOLS;
    }


}
