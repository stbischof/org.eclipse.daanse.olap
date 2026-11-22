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
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedBooleanCalc;

public class IifBooleanCalc extends AbstractNestedBooleanCalc{

    public IifBooleanCalc(Type type, BooleanCalc booleanCalc, BooleanCalc booleanCalc1, BooleanCalc booleanCalc2) {
        super(type, booleanCalc, booleanCalc1, booleanCalc2);
    }

    @Override
    public Boolean evaluateInternal(Evaluator evaluator) {
        final boolean condition =
            getChildCalc(0, BooleanCalc.class).evaluate(evaluator);
        if (condition) {
            return getChildCalc(1, BooleanCalc.class).evaluate(evaluator);
        } else {
            return getChildCalc(2, BooleanCalc.class).evaluate(evaluator);
        }
    }

}
