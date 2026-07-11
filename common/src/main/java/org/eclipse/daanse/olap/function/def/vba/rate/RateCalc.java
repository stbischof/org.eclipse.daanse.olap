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
package org.eclipse.daanse.olap.function.def.vba.rate;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;
import org.eclipse.daanse.olap.exceptions.InvalidArgumentException;
import org.eclipse.daanse.olap.function.def.vba.fv.FVCalc;

public class RateCalc extends AbstractProfilingNestedDoubleCalc {
    protected RateCalc(Type type, DoubleCalc nPerCalc, DoubleCalc pmtCalc, DoubleCalc pvCalc, DoubleCalc fvCalc,
            BooleanCalc typeCalc, DoubleCalc guessCalc) {
        super(type, nPerCalc, pmtCalc, pvCalc, fvCalc, typeCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double nPer = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        Double pmt = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        Double pv = getChildCalc(2, DoubleCalc.class).evaluate(evaluator);
        Double fv = getChildCalc(3, DoubleCalc.class).evaluate(evaluator);
        Boolean type = getChildCalc(4, BooleanCalc.class).evaluate(evaluator);
        Double guess = getChildCalc(5, DoubleCalc.class).evaluate(evaluator);

        if (nPer == null || pmt == null || pv == null || fv == null || guess == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }

        return rate(nPer, pmt, pv, fv, type, guess);
    }

    public static double rate(
            double nPer, // specifies the number of payment periods
            double pmt, // payment per period of annuity
            double pv, // the present value of the annuity (0 if a loan)
            double fv, // the future value of the annuity ($ if savings)
            boolean due,
            double guess)
        {
            if (nPer <= 0) {
                throw new InvalidArgumentException(
                    "number of payment periods must be larger than 0");
            }
            double minGuess = 0.0;
            double maxGuess = 1.0;

            // converge on the correct answer should use Newton's Method
            // for now use a binary search
            int r = 1;
            if (pv < fv) {
                r = -1;
            }

            // the vb method uses 20 iterations, but they also probably use newton's
            // method,
            // so i've bumped it up to 30 iterations.
            for (int n = 0; n < 30; n++) {
                double gFV = FVCalc.fV(guess, nPer, pmt, pv, due);
                double diff = gFV - fv;
                if ((maxGuess - minGuess) < 0.0000001) {
                    return guess;
                } else {
                    if (diff * r < 0) {
                        maxGuess = guess;
                    } else {
                        minGuess = guess;
                    }
                    guess = (maxGuess + minGuess) / 2;
                }
            }
            // fail, not sure how VB fails
            return -1;
        }

}
