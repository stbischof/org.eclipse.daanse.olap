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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.function.def.dimensions.numeric;

import java.util.List;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedHierarchyCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class DimensionNumericCalc extends AbstractNestedHierarchyCalc {

	public DimensionNumericCalc(Type type, IntegerCalc integerCalc) {
		super(type, integerCalc);
	}

	@Override
	public Hierarchy evaluateInternal(Evaluator evaluator) {
		Integer n = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
		return nthHierarchy(evaluator, n);
	}

	private Hierarchy nthHierarchy(Evaluator evaluator, Integer n) {
		Cube cube = evaluator.getCube();
		List<Hierarchy> hierarchies = cube.getHierarchies();
		if (n >= hierarchies.size() || n < 0) {
			throw FunUtil.newEvalException(DimensionsNumericFunDef.functionalMetaData,
					new StringBuilder("Index '").append(n).append("' out of bounds").toString());
		}
		// n=0 is the Measurement Hierarchy
		return hierarchies.get(n);
	}
}