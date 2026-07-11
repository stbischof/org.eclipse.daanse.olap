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
package org.eclipse.daanse.olap.function.def.excel.asin;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.NullSemantics;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class AsinCalc extends AbstractProfilingNestedDoubleCalc {

    protected AsinCalc(Type type, DoubleCalc doubleCalc) {
        super(type, doubleCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double x = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        if (NullSemantics.isNull(x)) {
            return null;
        }
        return Math.asin(x);
    }

}
