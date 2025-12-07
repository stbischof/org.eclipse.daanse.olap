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
package org.eclipse.daanse.olap.function.def.vba.timeserial;

import java.time.LocalDateTime;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDateTimeCalc;

public class TimeSerialCalc extends AbstractProfilingNestedDateTimeCalc {

    protected TimeSerialCalc(Type type, IntegerCalc hourCalc, IntegerCalc minuteCalc, IntegerCalc secondCalc) {
        super(type, hourCalc, minuteCalc, secondCalc);
    }

    @Override
    public LocalDateTime evaluateInternal(Evaluator evaluator) {
        Integer hour = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
        Integer minute = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        Integer second = getChildCalc(2, IntegerCalc.class).evaluate(evaluator);
        // Use epoch date (1970-01-01) as base for time-only values
        return LocalDateTime.of(1970, 1, 1, hour, minute, second);
    }

}
