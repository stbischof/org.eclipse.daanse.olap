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
package org.eclipse.daanse.olap.function.def.vba.nper;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class NPerCalc extends AbstractProfilingNestedDoubleCalc {

    protected NPerCalc(Type type, final DoubleCalc rateCalc, final DoubleCalc pmtCalc, final DoubleCalc pvCalc, final DoubleCalc fvCalc, final BooleanCalc dueCalc) {
        super(type, rateCalc, pmtCalc, pvCalc, fvCalc, dueCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double rate = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        Double pmt = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        Double pv = getChildCalc(2, DoubleCalc.class).evaluate(evaluator);
        Double fv = getChildCalc(3, DoubleCalc.class).evaluate(evaluator);
        Boolean due = getChildCalc(3, BooleanCalc.class).evaluate(evaluator);

        if (rate == null || pmt == null || pv == null || fv == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }
        
        if (rate == 0) {
            return -(fv + pv) / pmt;
        } else {
            double r1 = rate + 1;
            double ryr = (due ? r1 : 1) * pmt / rate;
            double a1 =
                ((ryr - fv) < 0)
                ? Math.log(fv - ryr)
                : Math.log(ryr - fv);
            double a2 =
                ((ryr - fv) < 0)
                ? Math.log(-pv - ryr)
                : Math.log(pv + ryr);
            double a3 = Math.log(r1);
            return (a1 - a2) / a3;
        }
    }

}
