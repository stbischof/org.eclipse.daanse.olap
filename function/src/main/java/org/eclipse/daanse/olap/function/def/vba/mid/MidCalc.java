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
package org.eclipse.daanse.olap.function.def.vba.mid;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;
import org.eclipse.daanse.olap.exceptions.InvalidArgumentException;

public class MidCalc extends AbstractNestedStringCalc {
    private final IntegerCalc lengthCalc;
    protected MidCalc(final Type type, final StringCalc valueCalc, final IntegerCalc beginIndexCalc, final IntegerCalc lengthCalc) {
        super(type, valueCalc, beginIndexCalc, lengthCalc);
        this.lengthCalc = lengthCalc;
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        String value = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        Integer beginIndex = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        Integer length;
        if (lengthCalc != null) {
            length = getChildCalc(2, IntegerCalc.class).evaluate(evaluator);
        } else {
            length = value.length();
        }
        
        return mid(value, beginIndex, length);

    }

    public static String mid(String value, int beginIndex, int length) {
        // Arguments are 1-based. Spec says that the function gives an error if
        // Start <= 0 or Length < 0.
        if (beginIndex <= 0) {
            throw new InvalidArgumentException(
                "Invalid parameter. "
                + "Start parameter of Mid function must be positive");
        }
        if (length < 0) {
            throw new InvalidArgumentException(
                "Invalid parameter. "
                + "Length parameter of Mid function must be non-negative");
        }

        if (beginIndex > value.length()) {
            return "";
        }

        // Shift from 1-based to 0-based.
        --beginIndex;
        int endIndex = beginIndex + length;
        return endIndex >= value.length() ? value.substring(beginIndex) : value
                .substring(beginIndex, endIndex);
    }

}
