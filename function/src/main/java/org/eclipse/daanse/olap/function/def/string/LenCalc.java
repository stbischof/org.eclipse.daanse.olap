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
package org.eclipse.daanse.olap.function.def.string;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedIntegerCalc;

public class LenCalc extends AbstractNestedIntegerCalc {

    protected LenCalc(Type type, final StringCalc stringCalc) {
        super(type, stringCalc);
    }

    @Override
    public Integer evaluateInternal(Evaluator evaluator) {
        String value = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        if (value == null) {
            return 0;
        }
        return value.length();
    }

}
