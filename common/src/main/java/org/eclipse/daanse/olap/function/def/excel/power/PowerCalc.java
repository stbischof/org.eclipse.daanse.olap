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
package org.eclipse.daanse.olap.function.def.excel.power;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class PowerCalc extends AbstractProfilingNestedDoubleCalc {

    protected PowerCalc(Type type, DoubleCalc calc0, DoubleCalc calc1) {
        super(type, calc0, calc1);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double x = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        Double y = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);

        if (x == null || y == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }
        return Math.pow(x, y);
    }

}
