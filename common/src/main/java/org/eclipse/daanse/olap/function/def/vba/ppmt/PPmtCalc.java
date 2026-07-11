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
package org.eclipse.daanse.olap.function.def.vba.ppmt;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class PPmtCalc extends AbstractProfilingNestedDoubleCalc {
    protected PPmtCalc(Type type, DoubleCalc rateCalc, DoubleCalc perCalc, DoubleCalc nPerCalc, DoubleCalc pvCalc, DoubleCalc fvCalc,
            BooleanCalc dueCalc) {
        super(type, rateCalc, perCalc, nPerCalc, pvCalc, fvCalc,
                dueCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double rate = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        Double per = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        Double nPer = getChildCalc(2, DoubleCalc.class).evaluate(evaluator);
        Double pv = getChildCalc(3, DoubleCalc.class).evaluate(evaluator);
        Double fv = getChildCalc(4, DoubleCalc.class).evaluate(evaluator);
        Boolean due = getChildCalc(5, BooleanCalc.class).evaluate(evaluator);

        if (rate == null || per == null || nPer == null || pv == null || fv == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }

        return pmt(rate, nPer, pv, fv, due)
                - iPmt(rate, per, nPer, pv, fv, due);
    }

    public static double iPmt(
            double rate,
            double per,
            double nPer,
            double pv,
            double fv,
            boolean due)
        {
            double pmtVal = pmt(rate, nPer, pv, fv, due);
            double pValm1 = pv - pV(rate, per - 1, pmtVal, fv, due);
            return - pValm1 * rate;
        }
    
    public static double pmt(
            double rate,
            double nPer,
            double pv,
            double fv,
            boolean due)
        {
            if (rate == 0) {
                return -(fv + pv) / nPer;
            } else {
                double r1 = rate + 1;
                return
                    (fv + pv * Math.pow(r1, nPer))
                    * rate
                    / ((due ? r1 : 1) * (1 - Math.pow(r1, nPer)));
            }
        }
    
    public static double pV(
            double rate,
            double nper,
            double pmt,
            double fv,
            boolean due)
        {
            if (rate == 0) {
                return -((nper * pmt) + fv);
            } else {
                double r1 = rate + 1;
                return
                    (((1 - Math.pow(r1, nper)) / rate) * (due ? r1 : 1) * pmt - fv)
                        / Math.pow(r1, nper);
            }
        }
}
