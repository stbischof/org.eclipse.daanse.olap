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
package org.eclipse.daanse.olap.function.def.vba.formatdatetime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;

public class FormatDateTimeCalc extends AbstractProfilingNestedStringCalc {

    private static final DateTimeFormatter LONG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter SHORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter GENERAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected FormatDateTimeCalc(Type type, final DateTimeCalc dateCalc, final IntegerCalc namedFormatCalc) {
        super(type, dateCalc, namedFormatCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        LocalDateTime dateTime = getChildCalc(0, DateTimeCalc.class).evaluate(evaluator);
        Integer namedFormat = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        return formatDateTime(dateTime, namedFormat);
    }

    public static String formatDateTime(
            LocalDateTime dateTime,
            int namedFormat /* default 0, GeneralDate */)
        {
            // todo: how do we support VB Constants? Strings or Ints?
            return switch (namedFormat) {
                // vbLongDate, 1
                // Display a date using the long date format specified in your
                // computer's regional settings.
                case 1 -> dateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));

                // vbShortDate, 2
                // Display a date using the short date format specified in your
                // computer's regional settings.
                case 2 -> dateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));

                // vbLongTime, 3
                // Display a time using the time format specified in your computer's
                // regional settings.
                // Note: Using explicit pattern since FormatStyle.LONG requires timezone
                case 3 -> dateTime.format(LONG_TIME_FORMATTER);

                // vbShortTime, 4
                // Display a time using the 24-hour format (hh:mm).
                case 4 -> dateTime.format(SHORT_TIME_FORMATTER);

                // vbGeneralDate, 0
                // Display a date and/or time. If there is a date part,
                // display it as a short date. If there is a time part,
                // display it as a long time. If present, both parts are
                // displayed.
                default -> dateTime.format(GENERAL_DATE_FORMATTER);
            };
        }

}
