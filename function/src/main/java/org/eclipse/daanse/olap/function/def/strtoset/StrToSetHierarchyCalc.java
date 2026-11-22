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
package org.eclipse.daanse.olap.function.def.strtoset;

import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.exceptions.EmptyExpressionWasSpecifiedException;
import org.eclipse.daanse.olap.fun.FunUtil;

public class StrToSetHierarchyCalc extends AbstractNestedTupleListCalc {

    private Hierarchy hierarchy;

    protected StrToSetHierarchyCalc(Type type, final StringCalc stringCalc, final Hierarchy hierarchy) {
        super(type, stringCalc);
        this.hierarchy = hierarchy;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        String string = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        if (string == null) {
            throw FunUtil.newEvalException(new EmptyExpressionWasSpecifiedException());
        }
        return new UnaryTupleList(FunUtil.parseMemberList(evaluator, string, hierarchy));
    }

}
