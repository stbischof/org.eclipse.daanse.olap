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
package org.eclipse.daanse.olap.function.def.format;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;
import org.eclipse.daanse.olap.util.format.Format;

public class FormatLiteralCalc extends AbstractNestedStringCalc{

    private final Format format;
    
    protected FormatLiteralCalc(Type type, Calc<?> calc, final Format format) {
        super(type, calc);
        this.format = format;
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        final Object o = getChildCalc(0, Calc.class).evaluate(evaluator);
        return format.format(o);
    }

}
