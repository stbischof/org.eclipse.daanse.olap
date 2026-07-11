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

import java.util.List;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.fun.FunUtil.SetWrapper;

public class LinRegCalc extends AbstractProfilingNestedDoubleCalc {

    private final TupleListCalc tupleListCalc;
    private final DoubleCalc yCalc;
    private final DoubleCalc xCalc;
    private final int regType;

    public LinRegCalc(
        ResolvedFunCall call,
        TupleListCalc tupleListCalc,
        DoubleCalc yCalc,
        DoubleCalc xCalc,
        int regType)
    {
        super(call.getType(), new Calc[]{tupleListCalc, yCalc, xCalc});
        this.tupleListCalc = tupleListCalc;
        this.yCalc = yCalc;
        this.xCalc = xCalc;
        this.regType = regType;
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Value value = process(evaluator, tupleListCalc, yCalc, xCalc);
        if (value == null) {
            return null;
        }
        switch (regType) {
        case LinRegFunDef.INTERCEPT:
            return value.getIntercept();
        case LinRegFunDef.SLOPE:
            return value.getSlope();
        case LinRegFunDef.VARIANCE:
            return value.getVariance();
        case LinRegFunDef.R2:
            return value.getRSquared();
        default:
        case LinRegFunDef.POINT:
            throw Util.newInternal("unexpected value " + regType);
        }
    }
    
    public static Value process(
            Evaluator evaluator,
            TupleListCalc tupleListCalc,
            DoubleCalc yCalc,
            DoubleCalc xCalc)
        {
            final int savepoint = evaluator.savepoint();
            TupleList members;
            try {
                evaluator.setNonEmpty(false);
                members = tupleListCalc.evaluate(evaluator);
            } finally {
                evaluator.restore(savepoint);
            }
            SetWrapper[] sws;
            try {
                sws =
                    FunUtil.evaluateSet(
                        evaluator, members, new DoubleCalc[] {yCalc, xCalc});
            } finally {
                evaluator.restore(savepoint);
            }
            SetWrapper swY = sws[0];
            SetWrapper swX = sws[1];

            if (swY.errorCount > 0) {
                debug("LinReg.process", "ERROR error(s) count ="  + swY.errorCount);
                // TODO: throw exception
                return null;
            } else if (swY.v.isEmpty()) {
                return null;
            }

            return linearReg(swX.v, swY.v);
        }
    
    private static void debug(String type, String msg) {
        // comment out for no output
        // RME
    }
    
    private static Value linearReg(List xlist, List ylist) {
        // y and x have same number of points
        int size = ylist.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXX = 0.0;
        double sumXY = 0.0;

        debug("LinReg.linearReg", "ylist.size()=" + ylist.size());
        debug("LinReg.linearReg", "xlist.size()=" + xlist.size());
        int n = 0;
        for (int i = 0; i < size; i++) {
            Object yo = ylist.get(i);
            Object xo = xlist.get(i);
            if ((yo == null) || (xo == null)) {
                continue;
            }
            n++;
            double y = ((Double) yo).doubleValue();
            double x = ((Double) xo).doubleValue();

            debug("LinReg.linearReg", new StringBuilder(" ").append(i).append(" (")
                .append(x).append(",").append(y).append(")").toString());
            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
        }

        double xMean = n == 0 ? 0 : sumX / n;
        double yMean = n == 0 ? 0 : sumY / n;

        debug("LinReg.linearReg", "yMean=" + yMean);
        debug(
            "LinReg.linearReg",
            "(n*sumXX - sumX*sumX)=" + (n * sumXX - sumX * sumX));
        // The regression line is the line that minimizes the variance of the
        // errors. The mean error is zero; so, this means that it minimizes the
        // sum of the squares errors.
        double slope = (n * sumXX - sumX * sumX) == 0 ? 0 : (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = yMean - slope * xMean;

        Value value = new Value(intercept, slope, xlist, ylist);
        debug("LinReg.linearReg", "value=" + value);

        return value;
    }

}
