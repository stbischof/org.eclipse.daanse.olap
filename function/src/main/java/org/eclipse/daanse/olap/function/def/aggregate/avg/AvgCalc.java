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
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.olap.function.def.aggregate.avg;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.core.AbstractAggregateFunDef;

class AvgCalc extends AbstractNestedDoubleCalc {
	private final String timingName;

	public AvgCalc(Type type, TupleListCalc tupleListCalc, Calc<?> calc, String timingName) {
		super(type, tupleListCalc, calc);
		this.timingName = timingName;
	}

	@Override
	public Double evaluateInternal(Evaluator evaluator) {
		evaluator.getTiming().markStart(timingName);
		final int savepoint = evaluator.savepoint();
		try {
			TupleListCalc tupleListCalc = getChildCalc(0, TupleListCalc.class);
			Calc<?> calc = getChildCalc(1);
			TupleList memberList = AbstractAggregateFunDef.evaluateCurrentList(tupleListCalc, evaluator);
			evaluator.setNonEmpty(false);
			return (Double) FunUtil.avg(evaluator, memberList, calc);
		} finally {
			evaluator.restore(savepoint);
			evaluator.getTiming().markEnd(timingName);
		}
	}

	@Override
	public boolean dependsOn(Hierarchy hierarchy) {
		return HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
	}
}