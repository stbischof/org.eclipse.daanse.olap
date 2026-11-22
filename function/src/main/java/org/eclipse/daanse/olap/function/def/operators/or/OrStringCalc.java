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

package org.eclipse.daanse.olap.function.def.operators.or;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class OrStringCalc extends AbstractNestedStringCalc {

    protected OrStringCalc(Type type, final StringCalc calc0, final StringCalc calc1) {
        super(type, calc0, calc1);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        final String s0 = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        final String s1 = getChildCalc(1, StringCalc.class).evaluate(evaluator);
        return s0 + s1;
    }
}
