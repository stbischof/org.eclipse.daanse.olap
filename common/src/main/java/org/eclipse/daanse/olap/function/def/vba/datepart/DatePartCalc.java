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
package org.eclipse.daanse.olap.function.def.vba.datepart;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;
import org.eclipse.daanse.olap.function.def.vba.dateadd.Interval;
import org.eclipse.daanse.olap.function.def.vba.datediff.FirstWeekOfYear;

public class DatePartCalc extends AbstractProfilingNestedIntegerCalc {
    protected DatePartCalc(Type type, StringCalc stringCalc, DateTimeCalc dateTimeCalc,
            IntegerCalc firstDayOfWeek, IntegerCalc firstWeekOfYear) {
        super(type, stringCalc, dateTimeCalc, firstDayOfWeek, firstWeekOfYear);
    }

    @Override
    public Integer evaluateInternal(Evaluator evaluator) {
        String intervalName = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        LocalDateTime dateTime = getChildCalc(1, DateTimeCalc.class).evaluate(evaluator);
        int firstDayOfWeek = getChildCalc(2, IntegerCalc.class).evaluate(evaluator);
        int fwofy = getChildCalc(3, IntegerCalc.class).evaluate(evaluator);
        FirstWeekOfYear firstWeekOfYear = FirstWeekOfYear.values()[fwofy];
        return datePart(intervalName, dateTime, firstDayOfWeek, firstWeekOfYear);
    }

    private static int datePart(String intervalName, LocalDateTime dateTime, int firstDayOfWeek, FirstWeekOfYear firstWeekOfYear) {
        Interval interval = Interval.valueOf(intervalName);
        // Convert LocalDateTime to Calendar for Interval processing
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        if (Interval.w.equals(interval) || Interval.ww.equals(interval)) {
            // firstWeekOfYear and firstDayOfWeek only matter for 'w' and 'ww'
            firstWeekOfYear.apply(calendar);
            calendar.setFirstDayOfWeek(firstDayOfWeek);
        }
        return interval.datePart(calendar);
    }

}
