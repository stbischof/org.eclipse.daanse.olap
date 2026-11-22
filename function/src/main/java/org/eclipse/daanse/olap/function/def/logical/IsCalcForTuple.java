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
package org.eclipse.daanse.olap.function.def.logical;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedBooleanCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class IsCalcForTuple extends AbstractNestedBooleanCalc{

    protected IsCalcForTuple(Type type, final TupleCalc tupleCalc0, final TupleCalc tupleCalc1) {
        super(type, tupleCalc0, tupleCalc1);
    }
    
    @Override
    public Boolean evaluateInternal(Evaluator evaluator) {
        Member[] o0 = getChildCalc(0, TupleCalc.class).evaluate(evaluator);
        Member[] o1 = getChildCalc(1, TupleCalc.class).evaluate(evaluator);
        return FunUtil.equalTuple(o0, o1);
    }
}
