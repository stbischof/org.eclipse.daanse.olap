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
package org.eclipse.daanse.olap.function.def.coalesceempty;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;

public class CoalesceEmptyCalc extends AbstractNestedUnknownCalc {

    private final Calc<?>[] calcs;

    protected CoalesceEmptyCalc(Type type, Calc<?>[] calcs) {
        super(type);
        this.calcs = calcs;
    }

    @Override
    public Object evaluateInternal(Evaluator evaluator) {
        for (Calc<?> calc : calcs) {
            final Object o = calc.evaluate(evaluator);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return calcs;
    }

}
