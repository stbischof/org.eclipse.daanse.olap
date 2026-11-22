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
package org.eclipse.daanse.olap.function.def.iif;

import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;

public class IifNumericCalc extends AbstractNestedUnknownCalc {
    private final BooleanCalc booleanCalc;
    private final Calc<?> calc1;
    private final Calc<?> calc2;

    protected IifNumericCalc(Type type, final BooleanCalc booleanCalc, final Calc<?> calc1, final Calc<?> calc2) {
        super(type);
        this.booleanCalc = booleanCalc;
        this.calc1 = calc1;
        this.calc2 = calc2;
    }

    @Override
    public Object evaluateInternal(Evaluator evaluator) {
        final boolean b = booleanCalc.evaluate(evaluator);
        Calc<?> calc = b ? calc1 : calc2;
        return calc.evaluate(evaluator);
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return new Calc[] { booleanCalc, calc1, calc2 };
    }

}
