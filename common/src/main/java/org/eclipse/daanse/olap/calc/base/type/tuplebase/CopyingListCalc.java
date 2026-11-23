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
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleListCalc;

/**
 * A list a consumer may change: the copy of a list whose producer only promised LIST. A producer that
 * promised MUTABLE_LIST is not copied again.
 */
public class CopyingListCalc extends AbstractNestedTupleListCalc {

    private final TupleListCalc listCalc;

    public CopyingListCalc(TupleListCalc listCalc) {
        super(listCalc.getType(), new Calc[] { listCalc });
        this.listCalc = listCalc;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        return listCalc.evaluate(evaluator).copyList(-1);
    }

    @Override
    public ResultStyle getResultStyle() {
        return ResultStyle.MUTABLE_LIST;
    }
}
