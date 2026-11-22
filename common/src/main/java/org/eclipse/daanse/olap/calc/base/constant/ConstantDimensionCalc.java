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

import org.eclipse.daanse.olap.api.calc.DimensionCalc;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.type.DimensionType;
import org.eclipse.daanse.olap.calc.base.AbstractConstantCalc;

public class ConstantDimensionCalc extends AbstractConstantCalc<Dimension> implements DimensionCalc {

	public ConstantDimensionCalc(DimensionType type, Dimension value) {
		super(value, type);
	}

	public static ConstantDimensionCalc of(Dimension dimension) {
		return new ConstantDimensionCalc(DimensionType.forDimension(dimension), dimension);
	}

}
