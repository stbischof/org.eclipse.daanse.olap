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
package org.eclipse.daanse.olap.function.def.udf.currentdatestring;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;
import org.eclipse.daanse.olap.util.Format;

public class CurrentDateStringCalc extends AbstractProfilingNestedStringCalc {

    protected CurrentDateStringCalc(Type type, final StringCalc stringCalc) {
        super(type, stringCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        StringCalc stringCalc = getChildCalc(0, StringCalc.class);

        final Locale locale = Locale.getDefault();
        final Format format = new Format(stringCalc.evaluate(evaluator), locale);
        LocalDateTime currDate = evaluator.getQueryStartTime();
        // Convert LocalDateTime to Date for Format compatibility
        Date legacyDate = Date.from(currDate.atZone(ZoneId.systemDefault()).toInstant());
        return format.format(legacyDate);

    }

}
