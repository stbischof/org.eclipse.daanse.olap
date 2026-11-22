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
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class IifStringCalc extends AbstractNestedStringCalc {

    public IifStringCalc(Type type, BooleanCalc booleanCalc, StringCalc calc1, StringCalc calc2) {
        super(type, booleanCalc, calc1, calc2);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        final boolean b = getChildCalc(0, BooleanCalc.class).evaluate(evaluator);
        StringCalc calc = b ? getChildCalc(1, StringCalc.class) : getChildCalc(2, StringCalc.class);
        return calc.evaluate(evaluator);
    }

}
