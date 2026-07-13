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
package org.eclipse.daanse.olap.calc.base.type.doublex;


import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class UnknownToDoubleCalc extends AbstractProfilingNestedDoubleCalc {

	public UnknownToDoubleCalc(Type type, Calc<?> calc) {
		super(type, calc);
	}

	@Override
	public Double evaluateInternal(Evaluator evaluator) {

		Object o = getFirstChildCalc().evaluate(evaluator);
		if (o == null) {
			return null;
		} else if (o == NotLoaded.INSTANCE) {
			// Dirty evaluation pass: the cell is not loaded yet and this
			// pass's results are discarded. Substitute the same dummy the
			// old Double(0) marker used to inject, but keep the lie local.
			return 0.0;
		} else if (o instanceof Double d) {
			return d;
		} else if (o instanceof Number n) {
			return n.doubleValue();
		}
		throw evaluator.newEvalException(null, "wrtong typed, was: " + o);
	}
}