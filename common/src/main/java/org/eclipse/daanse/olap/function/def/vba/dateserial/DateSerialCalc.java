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
package org.eclipse.daanse.olap.function.def.vba.dateserial;

import java.time.LocalDateTime;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDateTimeCalc;

public class DateSerialCalc extends AbstractProfilingNestedDateTimeCalc {

    protected DateSerialCalc(Type type, IntegerCalc yearCalc, IntegerCalc monthCalc, IntegerCalc dayCalc) {
        super(type,yearCalc, monthCalc, dayCalc);
    }

    @Override
    public LocalDateTime evaluateInternal(Evaluator evaluator) {
        Integer year = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
        Integer month = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        Integer day = getChildCalc(2, IntegerCalc.class).evaluate(evaluator);
        return LocalDateTime.of(year, month, day, 0, 0);
    }

}
