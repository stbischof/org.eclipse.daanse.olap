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
package org.eclipse.daanse.olap.function.def.format;

import java.util.Locale;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;
import org.eclipse.daanse.olap.util.format.Format;

public class FormatCalc extends AbstractNestedStringCalc{

    private final Locale locale;
    
    public FormatCalc(Type type, Calc<?> calc, StringCalc stringCalc, final Locale locale) {
        super(type, calc, stringCalc);
        this.locale = locale;
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Calc<?> calc = getChildCalc(0, Calc.class);
        StringCalc stringCalc = getChildCalc(1, StringCalc.class);
        final Object o = calc.evaluate(evaluator);
        final String formatString =
                stringCalc.evaluate(evaluator);
        final Format format =
                new Format(formatString, locale);
        return format.format(o);
    }

}
