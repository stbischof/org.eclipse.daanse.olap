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
package org.eclipse.daanse.olap.function.def.strtotuple;

import java.util.List;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleCalc;
import org.eclipse.daanse.olap.exceptions.EmptyExpressionWasSpecifiedException;
import org.eclipse.daanse.olap.fun.FunUtil;

public class StrToTupleCalc extends AbstractNestedTupleCalc{

    private final List<Hierarchy> hierarchies;

    protected StrToTupleCalc(Type type, StringCalc stringCalc, final List<Hierarchy> hierarchies) {
        super(type, stringCalc);
        this.hierarchies = hierarchies;
    }

    @Override
    public Member[] evaluateInternal(Evaluator evaluator) {
        String string = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        if (string == null) {
            throw FunUtil.newEvalException(
                new EmptyExpressionWasSpecifiedException());
        }
        return FunUtil.parseTuple(evaluator, string, hierarchies);
    }

}
