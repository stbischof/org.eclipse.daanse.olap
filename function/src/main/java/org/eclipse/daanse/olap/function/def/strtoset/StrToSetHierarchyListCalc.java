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

import java.util.List;

import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.exceptions.EmptyExpressionWasSpecifiedException;
import org.eclipse.daanse.olap.fun.FunUtil;

public class StrToSetHierarchyListCalc extends AbstractNestedTupleListCalc {

    private List<Hierarchy> hierarchyList;

    protected StrToSetHierarchyListCalc(Type type, final StringCalc stringCalc, final List<Hierarchy> hierarchyList) {
        super(type, stringCalc);
        this.hierarchyList = hierarchyList;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        String string = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        if (string == null) {
            throw FunUtil.newEvalException(new EmptyExpressionWasSpecifiedException());
        }
        return FunUtil.parseTupleList(evaluator, string, hierarchyList);
    }

}
