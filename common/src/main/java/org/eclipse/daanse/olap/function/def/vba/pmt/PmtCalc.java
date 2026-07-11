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
package org.eclipse.daanse.olap.function.def.vba.pmt;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class PmtCalc extends AbstractProfilingNestedDoubleCalc {

    protected PmtCalc(Type type, final DoubleCalc rateCalc, final DoubleCalc nPerCalc, final DoubleCalc pvCalc, final DoubleCalc fvCalc, final BooleanCalc dueCalc) {
        super(type, rateCalc, nPerCalc, pvCalc, fvCalc, dueCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double rate = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        Double nPer = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        Double pv = getChildCalc(2, DoubleCalc.class).evaluate(evaluator);
        Double fv = getChildCalc(3, DoubleCalc.class).evaluate(evaluator);
        Boolean due = getChildCalc(4, BooleanCalc.class).evaluate(evaluator);

        if (rate == null || nPer == null || pv == null || fv == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }
        
        return pmt(rate, nPer, pv, fv, due);
    }

    public static double pmt(Double rate, Double nPer, Double pv, Double fv, Boolean due) {
        
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

}
