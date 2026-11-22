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

package org.eclipse.daanse.olap.function.def.operators.equal;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedBooleanCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class EqualStringCalc extends AbstractNestedBooleanCalc {

    protected EqualStringCalc(Type type, final StringCalc calc0, final StringCalc calc1) {
        super(type, calc0, calc1);
    }

    @Override
    public Boolean evaluateInternal(Evaluator evaluator) {
        final String b0 = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        final String b1 = getChildCalc(1, StringCalc.class).evaluate(evaluator);
        if (b0 == null || b1 == null) {
            return FunUtil.BOOLEAN_NULL;
        }
        return b0.equals(b1);
    }
}
