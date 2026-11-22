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
package org.eclipse.daanse.olap.function.def.vba.isnull;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedBooleanCalc;

public class IsNullCalc extends AbstractNestedBooleanCalc {

    protected IsNullCalc(Type type, final Calc<?> varNameCalc) {
        super(type, varNameCalc);
    }

    @Override
    public Boolean evaluateInternal(Evaluator evaluator) {
        Object expression = getChildCalc(0, Calc.class).evaluate(evaluator);
        return expression == null;
    }

}
