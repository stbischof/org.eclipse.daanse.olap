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
package org.eclipse.daanse.olap.calc.base.type.booleanx;

import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class UnknownToBooleanCalc extends AbstractProfilingNestedBooleanCalc {

	public UnknownToBooleanCalc(Type type, Calc<?> calc) {
		super(type, calc);
	}

	@Override
	public Boolean evaluateInternal(Evaluator evaluator) {
		Object v0 = getFirstChildCalc().evaluate(evaluator);
		if (v0 == null) {
			return FunUtil.BOOLEAN_NULL;
		}
		if (v0 == NotLoaded.INSTANCE) {
			// dirty pass, results discarded; dummy matches the old Double(0) marker
			return false;
		}
		if (v0 instanceof Boolean b) {
			return b;
		}
		if (v0 instanceof Number n) {
			return n.intValue() != 0;
		}
		throw evaluator.newEvalException(null, "wrong type, was:" + v0);

	}
}