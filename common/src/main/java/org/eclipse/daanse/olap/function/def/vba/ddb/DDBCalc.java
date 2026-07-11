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
package org.eclipse.daanse.olap.function.def.vba.ddb;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class DDBCalc extends AbstractProfilingNestedDoubleCalc {
    protected DDBCalc(Type type, DoubleCalc costCalc, DoubleCalc salvageCalc, DoubleCalc lifeCalc, DoubleCalc periodCalc, DoubleCalc factorCalc ) {
        super(type, costCalc, salvageCalc, lifeCalc, periodCalc, factorCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double cost = getChildCalc(0, DoubleCalc.class).evaluate(evaluator); 
        Double salvage = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        Double life = getChildCalc(2, DoubleCalc.class).evaluate(evaluator);
        Double period = getChildCalc(3, DoubleCalc.class).evaluate(evaluator);
        Double factor = getChildCalc(4, DoubleCalc.class).evaluate(evaluator);

        if (cost == null || salvage == null || life == null || period == null || factor == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }
        return (((cost - salvage) * factor) / life) * period;
    }

}
