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

package org.eclipse.daanse.olap.function.def.operators.minus;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.NullSemantics;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class MinusPrefixCalc extends AbstractProfilingNestedDoubleCalc {

    protected MinusPrefixCalc(Type type, final DoubleCalc calc) {
        super(type, calc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        final Double v = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        if (NullSemantics.isNull(v)) {
            return null;
        } else {
            return - v;
        }
    }
}
