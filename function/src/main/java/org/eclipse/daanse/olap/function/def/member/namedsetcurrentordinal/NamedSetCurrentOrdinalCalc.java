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
package org.eclipse.daanse.olap.function.def.member.namedsetcurrentordinal;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedIntegerCalc;

public class NamedSetCurrentOrdinalCalc extends AbstractNestedIntegerCalc {

    private NamedSetExpression namedSetExpr;

    protected NamedSetCurrentOrdinalCalc(Type type, final NamedSetExpression namedSetExpr) {
        super(type, new Calc[0]);
        this.namedSetExpr = namedSetExpr;
    }

    @Override
    public Integer evaluateInternal(Evaluator evaluator) {
        return namedSetExpr.getEval(evaluator).currentOrdinal();
    }
}
