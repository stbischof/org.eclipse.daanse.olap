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
package org.eclipse.daanse.olap.function.def.vba.strcomp;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedIntegerCalc;

public class StrCompCalc extends AbstractNestedIntegerCalc {

    protected StrCompCalc(Type type, final StringCalc string1Calc, final StringCalc string2Calc, final IntegerCalc compareCalc) {
        super(type, string1Calc, string2Calc,compareCalc);
    }

    @Override
    public Integer evaluateInternal(Evaluator evaluator) {
        String string1 = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        String string2 = getChildCalc(1, StringCalc.class).evaluate(evaluator);
        Integer compare = getChildCalc(2, IntegerCalc.class).evaluate(evaluator);
        return strComp(string1, string2, compare);
    }

    public static int strComp(String string1, String string2, int compare /* default BinaryCompare */) {
        // Note: compare is currently ignored
        // Wrapper already checked whether args are null
        assert string1 != null;
        assert string2 != null;
        return string1.compareTo(string2);
    }

}
