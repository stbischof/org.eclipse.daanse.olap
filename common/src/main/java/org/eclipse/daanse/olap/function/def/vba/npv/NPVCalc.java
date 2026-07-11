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
package org.eclipse.daanse.olap.function.def.vba.npv;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class NPVCalc extends AbstractProfilingNestedDoubleCalc {

    protected NPVCalc(Type type, final DoubleCalc rCalc, final Calc<?> cfsCalc) {
        super(type, rCalc, cfsCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double r = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        Double[] cfs = (Double[]) getChildCalc(1).evaluate(evaluator);

        if (r == null || cfs == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }
        return nPV(r, cfs);
    }

    public static double nPV(Double r, Double[] cfs) {
        double npv = 0;
        double r1 = r + 1;
        double trate = r1;
        for (int i = 0, iSize = cfs.length; i < iSize; i++) {
            npv += cfs[i] / trate;
            trate *= r1;
        }
        return npv;
    }

}
