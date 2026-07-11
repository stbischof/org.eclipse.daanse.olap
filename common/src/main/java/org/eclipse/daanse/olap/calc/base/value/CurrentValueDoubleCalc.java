/*
* Copyright (c) 2024 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*/
package org.eclipse.daanse.olap.calc.base.value;

import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingValueCalc;

public class CurrentValueDoubleCalc extends AbstractProfilingValueCalc<Double> implements DoubleCalc{


	public CurrentValueDoubleCalc(Type type) {
		super(type);
	}

	@Override
	protected Double convertCurrentValue(Object evaluatedCurrentValue) {
		if (evaluatedCurrentValue == null) {
			return null;
		} else if (evaluatedCurrentValue instanceof Double d) {
			return d;
		} else if (evaluatedCurrentValue instanceof Number n) {
			return n.doubleValue();
		}
		throw new RuntimeException("wring value");
	}

}
