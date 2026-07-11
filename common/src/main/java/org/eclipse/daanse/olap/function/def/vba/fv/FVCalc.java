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
package org.eclipse.daanse.olap.function.def.vba.fv;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class FVCalc extends AbstractProfilingNestedDoubleCalc {
    protected FVCalc(Type type, DoubleCalc rateCalc, DoubleCalc nPerCalc, DoubleCalc pmtCalc, DoubleCalc pvCalc,
            BooleanCalc typeCalc) {
        super(type, rateCalc, nPerCalc, pmtCalc, pvCalc, typeCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double rate = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        Double nPer = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        Double pmt = getChildCalc(2, DoubleCalc.class).evaluate(evaluator);
        Double pv = getChildCalc(3, DoubleCalc.class).evaluate(evaluator);
        Boolean type = getChildCalc(4, BooleanCalc.class).evaluate(evaluator);

        if (rate == null || nPer == null || pmt == null || pv == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }

        return fV(rate, nPer, pmt, pv, type);
    }

    public static double fV(double rate, double nPer, double pmt, double pv, boolean type) {
        if (rate == 0) {
            return -(pv + (nPer * pmt));
        } else {
            double r1 = rate + 1;
            return ((1 - Math.pow(r1, nPer)) * (type ? r1 : 1) * pmt) / rate - pv * Math.pow(r1, nPer);
        }
    }
}
