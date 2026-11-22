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
package org.eclipse.daanse.olap.calc.base.constant;

import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.calc.base.AbstractConstantCalc;

public class ConstantDoubleCalc extends AbstractConstantCalc<Double> implements DoubleCalc {

	public ConstantDoubleCalc(NumericType type, Double value) {
		super(value, type);
	}

}
