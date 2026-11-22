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
package org.eclipse.daanse.olap.function.def.set;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;

/**
 * Compiled expression that returns an empty list of members or tuples.
 */
public class EmptyListCalc  extends AbstractNestedTupleListCalc {
    private final TupleList list;

    /**
     * Creates an EmptyListCalc.
     *
     * @param call Expression which was compiled
     */
    EmptyListCalc(ResolvedFunCall call) {
        super(call.getType(), new Calc[0]);

        list = TupleCollections.emptyList(call.getType().getArity());
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        return list;
    }
}
