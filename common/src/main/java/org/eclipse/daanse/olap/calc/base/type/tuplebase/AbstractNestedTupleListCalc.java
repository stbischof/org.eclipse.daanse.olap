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
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.AbstractNestedCalc;

/**
 * Abstract implementation of the ListCalc interface.
 *
 * The derived class must
 * implement the evaluateList(org.eclipse.daanse.olap.api.Evaluator) method, and the
 * evaluate(org.eclipse.daanse.olap.api.Evaluator) method will call it.
 *
 * @author jhyde
 * @since Sep 27, 2005
 */
public abstract class AbstractNestedTupleListCalc extends AbstractNestedCalc<TupleList> implements TupleListCalc {
    private final boolean mutable;

    /**
     * Creates an abstract implementation of a compiled expression which returns a
     * mutable list of tuples.
     *
     * @param type  Type
     * @param calcs List of child compiled expressions (for dependency analysis)
     */
    protected AbstractNestedTupleListCalc(Type type, Calc... calcs) {
        this(type, calcs, true);
    }

    /**
     * Creates an abstract implementation of a compiled expression which returns a
     * list.
     *
     * @param type    Ttype
     * @param calcs   List of child compiled expressions (for dependency analysis)
     * @param mutable Whether the list is mutable
     */
    protected AbstractNestedTupleListCalc(Type type, Calc[] calcs, boolean mutable) {
        super(type, calcs);
        this.mutable = mutable;
        assert type instanceof SetType : "expecting a set: " + getType();
    }

    @Override
    public SetType getType() {
        return (SetType) super.getType();
    }

    @Override
    public ResultStyle getResultStyle() {
        return mutable ? ResultStyle.MUTABLE_LIST : ResultStyle.LIST;
    }

    @Override
    public String toString() {
        return "AbstractListCalc object";
    }
}
