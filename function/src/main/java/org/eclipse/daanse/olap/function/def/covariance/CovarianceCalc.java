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
package org.eclipse.daanse.olap.function.def.covariance;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.fun.FunUtil;

public class CovarianceCalc extends AbstractNestedDoubleCalc{

    private final boolean biased;
    
    protected CovarianceCalc(Type type, final TupleListCalc tupleListCalc, final Calc<?> calc1, final Calc<?> calc2, final boolean biased) {
        super(type, tupleListCalc, calc1, calc2);
        this.biased = biased;
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        TupleList memberList = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            return (Double) FunUtil.covariance(
                    evaluator,
                    memberList,
                    getChildCalc(1, Calc.class),
                    getChildCalc(2, Calc.class),
                    biased);
        } finally {
            evaluator.restore(savepoint);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }

}
