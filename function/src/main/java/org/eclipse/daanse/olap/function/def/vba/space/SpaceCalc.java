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
package org.eclipse.daanse.olap.function.def.vba.space;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class SpaceCalc extends AbstractNestedStringCalc {

    private static final String WHITESPACE = " ";

    protected SpaceCalc(Type type, final IntegerCalc numberCalc) {
        super(type, numberCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Integer number = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
        return WHITESPACE.repeat(number);
    }

}
