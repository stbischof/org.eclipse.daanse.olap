/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.vba.string;

import java.util.Arrays;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class StringCalc extends AbstractNestedStringCalc {

    protected StringCalc(Type type, final IntegerCalc numberCalc, final org.eclipse.daanse.olap.api.calc.StringCalc characterCalc) {
        super(type, numberCalc, characterCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Integer number = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
        String character = getChildCalc(1, org.eclipse.daanse.olap.api.calc.StringCalc.class).evaluate(evaluator);
        return string(number, character.charAt(0));
    }

    public static String string(int number, char character) {
        if (character == 0) {
            return "";
        }
        final char[] chars = new char[number];
        Arrays.fill(chars, (char) (character % 256));
        return new String(chars);
    }

}
