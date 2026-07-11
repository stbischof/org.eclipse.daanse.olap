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
package org.eclipse.daanse.olap.function.def.vba.cdbl;

import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class CDblCalc extends AbstractProfilingNestedDoubleCalc {

    protected CDblCalc(Type type, Calc<?> doubleCalc) {
        super(type, doubleCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        Object expression = getChildCalc(0, Calc.class).evaluate(evaluator);
        if (expression == null) {
            // MDX NULL stays NULL: the legacy sentinel passed through the
            // Number branch unchanged, i.e. remained NULL downstream.
            return null;
        }
        if (expression == NotLoaded.INSTANCE) {
            // discarded dirty-pass marker; legacy Double(0) yielded 0.0
            return 0.0;
        }
        if (expression instanceof Number number) {
            return number.doubleValue();
        } else {
            final String s = String.valueOf(expression);
            return Double.valueOf(s);
        }
    }

}
