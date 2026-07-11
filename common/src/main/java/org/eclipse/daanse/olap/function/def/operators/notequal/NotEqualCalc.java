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

package org.eclipse.daanse.olap.function.def.operators.notequal;

import java.util.Objects;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.NullSemantics;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class NotEqualCalc extends AbstractProfilingNestedBooleanCalc {

    protected NotEqualCalc(Type type, final DoubleCalc calc0, final DoubleCalc calc1) {
        super(type, calc0, calc1);
    }

    @Override
    public Boolean evaluateInternal(Evaluator evaluator) {
        final Double v0 = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        final Double v1 = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        // Null check first: MDX NULL is Java null and Double.isNaN
        // would throw on unboxing. NULL comparisons yield BOOLEAN_NULL (false).
        if (NullSemantics.isNull(v0) || NullSemantics.isNull(v1) || Double.isNaN(v0) || Double.isNaN(v1)) {
            return FunUtil.BOOLEAN_NULL;
        }
        return !Objects.equals(v0, v1);
    }
}
