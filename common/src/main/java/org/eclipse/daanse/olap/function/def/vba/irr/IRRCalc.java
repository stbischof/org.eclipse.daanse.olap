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
package org.eclipse.daanse.olap.function.def.vba.irr;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class IRRCalc extends AbstractProfilingNestedDoubleCalc {
    protected IRRCalc(Type type, Calc<?> valueArray, DoubleCalc guess) {
        super(type, valueArray, guess);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double[] valueArray = (Double[]) getChildCalc(0).evaluate(evaluator);
        Double guess = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);

        if (valueArray == null || guess == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }
        

        return irr(valueArray, guess);
    }

    public static double irr(Double[] valueArray, double guess) {
        // calc pV of stream (sum of pV's for valueArray) ((1 + guess) ^ index)
        double minGuess = 0.0;
        double maxGuess = 1.0;

        // i'm not certain
        int r = 1;
        if (valueArray[0] > 0) {
            r = -1;
        }

        for (int i = 0; i < 30; i++) {
            // first calculate overall return based on guess
            double totalPv = 0;
            for (int j = 0; j < valueArray.length; j++) {
                totalPv += valueArray[j] / Math.pow(1.0 + guess, j);
            }
            if ((maxGuess - minGuess) < 0.0000001) {
                return guess;
            } else if (totalPv * r < 0) {
                maxGuess = guess;
            } else {
                minGuess = guess;
            }
            // avg max min to determine next step
            guess = (maxGuess + minGuess) / 2;
        }
        // unable to find a match
        return -1;
    }
}
