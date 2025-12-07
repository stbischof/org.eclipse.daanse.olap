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
package org.eclipse.daanse.olap.function.def.vba.date;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDateTimeCalc;

public class DateCalc extends AbstractProfilingNestedDateTimeCalc {

    protected DateCalc(Type type) {
        super(type);
    }

    @Override
    public LocalDateTime evaluateInternal(Evaluator evaluator) {
        // Returns current date at midnight
        return LocalDate.now().atStartOfDay();
    }

}
