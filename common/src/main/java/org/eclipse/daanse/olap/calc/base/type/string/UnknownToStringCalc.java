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

package org.eclipse.daanse.olap.calc.base.type.string;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class UnknownToStringCalc extends AbstractNestedStringCalc {

	public UnknownToStringCalc(Type type, Calc<?> calc) {
		super(type,  calc );
	}

	@Override
	public String evaluateInternal(Evaluator evaluator) {

		Object o = getFirstChildCalc().evaluate(evaluator);

		if (o != null) {
			return o.toString();
		}
		return null;
	}
}