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

package org.eclipse.daanse.olap.calc.base.type.hierarchy;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.DimensionCalc;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedHierarchyCalc;
import org.eclipse.daanse.olap.calc.base.util.DimensionUtil;

public class DimensionDefaultHierarchyCalc extends AbstractNestedHierarchyCalc {

	public DimensionDefaultHierarchyCalc(Type type, DimensionCalc dimensionCalc) {
		super(type, dimensionCalc);
	}

	@Override
	public Hierarchy evaluateInternal(Evaluator evaluator) {
		final DimensionCalc dimensionCalc = getChildCalc(0, DimensionCalc.class);
		final Dimension dimension = dimensionCalc.evaluate(evaluator);
		final Hierarchy hierarchy = DimensionUtil.getDimensionDefaultHierarchyOrThrow(dimension);
		return hierarchy;

	}
}