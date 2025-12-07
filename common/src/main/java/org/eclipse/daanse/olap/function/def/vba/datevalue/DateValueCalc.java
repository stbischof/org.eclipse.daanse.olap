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
package org.eclipse.daanse.olap.function.def.vba.datevalue;

import java.time.LocalDateTime;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDateTimeCalc;

public class DateValueCalc extends AbstractProfilingNestedDateTimeCalc {

    protected DateValueCalc(Type type, final DateTimeCalc dateCalc) {
        super(type, dateCalc);
    }

    @Override
    public LocalDateTime evaluateInternal(Evaluator evaluator) {
        LocalDateTime dateTime = getChildCalc(0, DateTimeCalc.class).evaluate(evaluator);
        // Return date portion only (at midnight)
        return dateTime.toLocalDate().atStartOfDay();
    }

}
