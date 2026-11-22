/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */
package org.eclipse.daanse.olap.calc.base.type.tuplebase;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleCursor;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;

/**
 * Adapter that converts a
 * {@link org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc} to a
 * {@link org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc}.
 *
 * @author jhyde
 * @since Oct 23, 2008
 */
public class IterableListCalc extends AbstractNestedTupleListCalc {
    private final TupleIteratorCalc<?> tupleIteratorCalc;

    /**
     * Creates an IterableListCalc.
     *
     * @param tupleIteratorCalc Calculation that returns an iterable.
     */
    public IterableListCalc(TupleIteratorCalc<?> tupleIteratorCalc) {
        super(tupleIteratorCalc.getType(), new Calc[] { tupleIteratorCalc });
        this.tupleIteratorCalc = tupleIteratorCalc;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        // A TupleIterCalc is allowed to return a list. If so, save the copy.
        final TupleIterable iterable = tupleIteratorCalc.evaluate(evaluator);
        if (iterable instanceof TupleList tupleList) {
            return tupleList;
        }

        final TupleList list = TupleCollections.createList(iterable.getArity());
        final TupleCursor tupleCursor = iterable.tupleCursor();
        while (tupleCursor.forward()) {
            // REVIEW: Worth creating TupleList.addAll(TupleCursor)?
            list.addCurrent(tupleCursor);
        }
        return list;
    }
}
