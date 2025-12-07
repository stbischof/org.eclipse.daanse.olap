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
package org.eclipse.daanse.olap.function.def.vba.weekday;

import java.time.LocalDateTime;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;

public class WeekdayCalc extends AbstractProfilingNestedIntegerCalc {
    protected WeekdayCalc(Type type, DateTimeCalc dateTimeCalc,
            IntegerCalc firstDayOfWeek) {
        super(type, dateTimeCalc, firstDayOfWeek);
    }

    @Override
    public Integer evaluateInternal(Evaluator evaluator) {
        LocalDateTime dateTime = getChildCalc(0, DateTimeCalc.class).evaluate(evaluator);
        int firstDayOfWeek = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        // DayOfWeek: MONDAY=1, SUNDAY=7 in java.time
        // VBA: SUNDAY=1, SATURDAY=7 by default
        int weekday = dateTime.getDayOfWeek().getValue() % 7 + 1; // Convert to VBA style (Sunday=1)
        // adjust for start of week
        weekday -= (firstDayOfWeek - 1);
        // bring into range 1..7
        weekday = (weekday + 6) % 7 + 1;
        return weekday;
    }


}
