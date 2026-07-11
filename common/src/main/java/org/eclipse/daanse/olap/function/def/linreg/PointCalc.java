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
package org.eclipse.daanse.olap.function.def.linreg;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class PointCalc extends AbstractProfilingNestedDoubleCalc {
    private final DoubleCalc xPointCalc;
    private final TupleListCalc tupleListCalc;
    private final DoubleCalc yCalc;
    private final DoubleCalc xCalc;

    public PointCalc(
        ResolvedFunCall call,
        DoubleCalc xPointCalc,
        TupleListCalc tupleListCalc,
        DoubleCalc yCalc,
        DoubleCalc xCalc)
    {
        super(call.getType(), new Calc[]{xPointCalc, tupleListCalc, yCalc, xCalc});
        this.xPointCalc = xPointCalc;
        this.tupleListCalc = tupleListCalc;
        this.yCalc = yCalc;
        this.xCalc = xCalc;
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Double xPoint = xPointCalc.evaluate(evaluator);
        Value value = LinRegCalc.process(evaluator, tupleListCalc, yCalc, xCalc);
        if (value == null || xPoint == null) {
            return null;
        }
        // use first arg to generate y position
        return xPoint * value.getSlope() + value.getIntercept();
    }
}
