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
package org.eclipse.daanse.olap.function.def.vba.datediff;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedLongCalc;
import org.eclipse.daanse.olap.function.def.vba.dateadd.Interval;

public class DateDiffCalc extends AbstractProfilingNestedLongCalc {
    protected DateDiffCalc(Type type, StringCalc stringCalc, DateTimeCalc dateTimeCalc1, DateTimeCalc dateTimeCalc2, IntegerCalc firstDayOfWeek, IntegerCalc firstWeekOfYear ) {
        super(type, stringCalc, dateTimeCalc1, dateTimeCalc2, firstDayOfWeek, firstWeekOfYear);
    }

    @Override
    public Long evaluateInternal(Evaluator evaluator) {
        String intervalName = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        LocalDateTime dateTime1 = getChildCalc(1, DateTimeCalc.class).evaluate(evaluator);
        LocalDateTime dateTime2 = getChildCalc(2, DateTimeCalc.class).evaluate(evaluator);
        int firstDayOfWeek = getChildCalc(3, IntegerCalc.class).evaluate(evaluator);
        int fwofy = getChildCalc(4, IntegerCalc.class).evaluate(evaluator);
        FirstWeekOfYear firstWeekOfYear = FirstWeekOfYear.values()[fwofy];
        return dateDiff(
                intervalName, dateTime1, dateTime2,
                firstDayOfWeek, firstWeekOfYear);
    }

    private static long dateDiff(
            String intervalName, LocalDateTime dateTime1, LocalDateTime dateTime2,
            int firstDayOfWeek, FirstWeekOfYear firstWeekOfYear)
        {
            Interval interval = Interval.valueOf(intervalName);
            if (interval == Interval.d) {
                // MONDRIAN-2319
                interval = Interval.y;
            }
            // Convert LocalDateTime to Calendar for Interval processing
            Calendar calendar1 = Calendar.getInstance();
            firstWeekOfYear.apply(calendar1);
            calendar1.setTimeInMillis(dateTime1.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            Calendar calendar2 = Calendar.getInstance();
            firstWeekOfYear.apply(calendar2);
            calendar2.setTimeInMillis(dateTime2.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            return interval.diff(calendar1, calendar2, firstDayOfWeek);
        }

}
