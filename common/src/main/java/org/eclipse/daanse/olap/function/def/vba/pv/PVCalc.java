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
package org.eclipse.daanse.olap.function.def.vba.pv;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class PVCalc extends AbstractProfilingNestedDoubleCalc {

    protected PVCalc(Type type, final DoubleCalc rateCalc, final DoubleCalc nPerCalc, final DoubleCalc pmtCalc, final DoubleCalc fvCalc, final BooleanCalc dueCalc) {
        super(type, rateCalc, nPerCalc, pmtCalc, fvCalc, dueCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double rate = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        Double nPer = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        Double pmt = getChildCalc(2, DoubleCalc.class).evaluate(evaluator);
        Double fv = getChildCalc(3, DoubleCalc.class).evaluate(evaluator);
        Boolean due = getChildCalc(4, BooleanCalc.class).evaluate(evaluator);

        if (rate == null || nPer == null || pmt == null || fv == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }
        return pV(rate, nPer, pmt, fv, due);
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
