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
package org.eclipse.daanse.olap.function.def.vba.sgn;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;

public class SgnCalc extends AbstractProfilingNestedIntegerCalc {

    protected SgnCalc(Type type, final DoubleCalc numberCalc) {
        super(type, numberCalc);
    }

    @Override
    public Integer evaluateInternal(Evaluator evaluator) {
        Double number = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);

        if (number == null) {
            // A NULL operand yields NULL; guard before unboxing.
            return null;
        }
        // We could use Math.signum(double) from JDK 1.5 onwards.
        if (number < 0.0d) {
            return -1;
        } else {
            return number > 0.0d ? 1 : 0;
        }
    }

}
