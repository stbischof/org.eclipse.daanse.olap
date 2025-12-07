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
package org.eclipse.daanse.olap.function.def.vba.dateadd;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDateTimeCalc;

public class DateAddCalc extends AbstractProfilingNestedDateTimeCalc {

    public static final long MILLIS_IN_A_DAY = 24L * 60 * 60 * 1000;

    protected DateAddCalc(Type type, StringCalc stringCalc, DoubleCalc doubleCalc, DateTimeCalc dateTimeCalc ) {
        super(type, stringCalc, doubleCalc, dateTimeCalc);
    }

    @Override
    public LocalDateTime evaluateInternal(Evaluator evaluator) {
        String intervalName = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        Double number = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        LocalDateTime dateTime = getChildCalc(2, DateTimeCalc.class).evaluate(evaluator);

        Interval interval = Interval.valueOf(intervalName);
        final double floor = Math.floor(number);

        // Convert LocalDateTime to Calendar for Interval processing
        ZoneId zone = ZoneId.systemDefault();
        long millis = dateTime.atZone(zone).toInstant().toEpochMilli();

        // We use the local calendar here. This method will therefore return
        // different results in different locales: it depends whether the
        // initial date and the final date are in DST.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        if (floor != number) {
            final double ceil = Math.ceil(number);
            interval.add(calendar, (int) ceil);
            final long ceilMillis = calendar.getTimeInMillis();

            calendar.setTimeInMillis(millis);
            interval.add(calendar, (int) floor);
            final long floorMillis = calendar.getTimeInMillis();

            final long amount =
                (long)
                    (((double) (ceilMillis - floorMillis)) * (number - floor));
            calendar.add(
                Calendar.DAY_OF_YEAR,
                (int) (amount / MILLIS_IN_A_DAY));
            calendar.add(
                Calendar.MILLISECOND, (int)
                (amount % MILLIS_IN_A_DAY));
        } else {
            interval.add(calendar, (int) floor);
        }
        // Convert Calendar back to LocalDateTime
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(calendar.getTimeInMillis()), zone);
    }

}
