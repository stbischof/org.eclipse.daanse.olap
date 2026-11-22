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
package org.eclipse.daanse.olap.function.def.udf.matches;

import java.util.regex.Pattern;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedBooleanCalc;

public class MatchesCalc extends AbstractNestedBooleanCalc {

    protected MatchesCalc(Type type, StringCalc stringCalc, StringCalc regexCalc) {
        super(type, stringCalc, regexCalc);
    }

    @Override
    public Boolean evaluateInternal(Evaluator evaluator) {
        final StringCalc stringCalc = getChildCalc(0, StringCalc.class);
        final StringCalc regexCalc = getChildCalc(1, StringCalc.class);
        String string = stringCalc.evaluate(evaluator);
        String regex = regexCalc.evaluate(evaluator);
        return Boolean.valueOf(Pattern.matches(regex, string));

    }

}
