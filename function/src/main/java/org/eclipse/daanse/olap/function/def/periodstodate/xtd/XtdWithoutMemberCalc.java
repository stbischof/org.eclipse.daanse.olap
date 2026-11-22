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
package org.eclipse.daanse.olap.function.def.periodstodate.xtd;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.fun.FunUtil;

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
public class XtdWithoutMemberCalc extends AbstractNestedTupleListCalc {
	private static final String TIMING_NAME = XtdWithoutMemberCalc.class.getSimpleName();

	private final Level level;

	public XtdWithoutMemberCalc(Type type, Level level) {
		super(type, new Calc[0]);
		this.level = level;
	}

	@Override
	public TupleList evaluateInternal(Evaluator evaluator) {
		evaluator.getTiming().markStart(XtdWithoutMemberCalc.TIMING_NAME);
		try {
			return new UnaryTupleList(FunUtil.periodsToDate(evaluator, level, null));
		} finally {
			evaluator.getTiming().markEnd(XtdWithoutMemberCalc.TIMING_NAME);
		}
	}

	@Override
	public boolean dependsOn(Hierarchy hierarchy) {
		return hierarchy.getDimension().getDimensionType() == org.eclipse.daanse.olap.api.element.DimensionType.TIME_DIMENSION;
	}
}
