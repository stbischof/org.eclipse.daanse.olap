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

import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.NullSemantics;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class DoubleToBooleanCalc extends AbstractProfilingNestedBooleanCalc {

	public DoubleToBooleanCalc(Type type, DoubleCalc doubleCalc) {
		super(type, doubleCalc);
	}

	@Override
	public Boolean evaluateInternal(Evaluator evaluator) {
		Double v0 = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);

		// Null check must come first: MDX NULL is Java null and
		// Double.isNaN would throw on unboxing.
		if (NullSemantics.isNull(v0) || Double.isNaN(v0)) {
			return FunUtil.BOOLEAN_NULL;
		}

		return v0 != 0;
	}
}