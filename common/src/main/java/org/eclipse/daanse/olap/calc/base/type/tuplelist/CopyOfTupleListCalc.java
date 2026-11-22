/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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

package org.eclipse.daanse.olap.calc.base.type.tuplelist;

import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleListCalc;

public class CopyOfTupleListCalc extends AbstractNestedTupleListCalc {

	public CopyOfTupleListCalc(TupleListCalc tupleListCalc) {
		super(tupleListCalc.getType(), tupleListCalc);
	}

	@Override
	public TupleList evaluateInternal(Evaluator evaluator) {
		final TupleList list = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
		return list.copyList(-1);
	}
}