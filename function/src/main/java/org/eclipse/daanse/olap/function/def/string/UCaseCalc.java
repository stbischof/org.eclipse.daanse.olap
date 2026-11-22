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
package org.eclipse.daanse.olap.function.def.string;

import java.util.Locale;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class UCaseCalc extends AbstractNestedStringCalc {

    private Locale locale;

    protected UCaseCalc(Type type, final StringCalc stringCalc, final Locale locale) {
        super(type, stringCalc);
        this.locale = locale;
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        String value = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        if (value == null) {
            return null;
        }
        return value.toUpperCase(locale);
    }

}
