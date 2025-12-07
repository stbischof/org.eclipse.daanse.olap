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
package org.eclipse.daanse.olap.function.def.vba.cdate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDateTimeCalc;
import org.eclipse.daanse.olap.common.InvalidArgumentException;

public class CDateCalc extends AbstractProfilingNestedDateTimeCalc {

    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("H:mm:ss"),
            DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("h:mm:ss a"),
            DateTimeFormatter.ofPattern("hh:mm:ss a"));

    protected CDateCalc(Type type, Calc<?> doubleCalc) {
        super(type, doubleCalc);
    }

    @Override
    public LocalDateTime evaluateInternal(Evaluator evaluator) {
        Object expression = getChildCalc(0, Calc.class).evaluate(evaluator);
        String str = String.valueOf(expression);
        if (expression instanceof LocalDateTime ldt) {
            return ldt;
        } else if (expression instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        } else if (expression == null) {
            return null;
        } else {
            // Try parsing as time with various formats
            LocalTime time = parseTime(str);
            if (time != null) {
                return time.atDate(java.time.LocalDate.of(1970, 1, 1));
            }
            // Try parsing as ISO LocalDateTime
            try {
                return LocalDateTime.parse(str);
            } catch (DateTimeParseException ex1) {
                // Try parsing as ISO LocalDate
                try {
                    return java.time.LocalDate.parse(str).atStartOfDay();
                } catch (DateTimeParseException ex2) {
                    throw new InvalidArgumentException(
                        new StringBuilder("Invalid parameter. ")
                        .append("expression parameter of CDate function must be ")
                        .append("formatted correctly (")
                        .append(String.valueOf(expression)).append(")").toString());
                }
            }
        }
    }

    private LocalTime parseTime(String str) {
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(str, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        return null;
    }

}
