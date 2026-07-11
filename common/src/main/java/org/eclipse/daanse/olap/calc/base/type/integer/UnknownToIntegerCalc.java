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

package org.eclipse.daanse.olap.calc.base.type.integer;

import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;

public class UnknownToIntegerCalc extends AbstractProfilingNestedIntegerCalc {

	public UnknownToIntegerCalc(Type type, Calc<?> calc) {
		super(type, calc);
	}

	@Override
	public Integer evaluateInternal(Evaluator evaluator) {
		Object o = getFirstChildCalc().evaluate(evaluator);
		if (o == null) {
			return null;
		} else if (o == NotLoaded.INSTANCE) {
			// dirty pass, results discarded; dummy matches the old Double(0) marker
			return 0;
		} else if (o instanceof Number n) {
			return n.intValue();
		}

		throw evaluator.newEvalException(null, "expected NUMBER was " + o);
	}
}