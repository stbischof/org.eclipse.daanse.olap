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


import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.NullSemantics;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class UnknownToDoubleCalc extends AbstractProfilingNestedDoubleCalc {

	public UnknownToDoubleCalc(Type type, Calc<?> calc) {
		super(type, calc);
	}

	@Override
	public Double evaluateInternal(Evaluator evaluator) {

		Object o = getFirstChildCalc().evaluate(evaluator);
		if (o == null) {
			return FunUtil.DOUBLE_NULL;
			// null;
			// TODO: !!! JUST REFACTORING 0 must be null
		} else if (NullSemantics.isNullValue(o)) {
			return FunUtil.DOUBLE_NULL;
		} else if (o instanceof Double d) {
			return d;
		} else if (o instanceof Number n) {
			return n.doubleValue();
		}
		throw evaluator.newEvalException(null, "wrtong typed, was: " + o);
	}
}