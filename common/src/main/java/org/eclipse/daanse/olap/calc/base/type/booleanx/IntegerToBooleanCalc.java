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

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedBooleanCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class IntegerToBooleanCalc extends AbstractNestedBooleanCalc {

	public IntegerToBooleanCalc(Type type, IntegerCalc integerCalc) {
		super(type, integerCalc);
	}

	@Override
	public Boolean evaluateInternal(Evaluator evaluator) {
		Integer v0 = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
		if (v0 == null) {
			return FunUtil.BOOLEAN_NULL;
		}
		return v0 != 0;
	}
}
