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
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.fun.FunUtil;

class XtdWithMemberCalc extends AbstractNestedTupleListCalc {

	private static final String TIMING_NAME = XtdWithMemberCalc.class.getSimpleName();
	private final Level level;

	public XtdWithMemberCalc(Type type, MemberCalc memberCalc, Level level) {
		super(type, new Calc[] { memberCalc });// TODO: make Calc...
		this.level = level;
	}

	@Override
	public TupleList evaluateInternal(Evaluator evaluator) {
		evaluator.getTiming().markStart(XtdWithMemberCalc.TIMING_NAME);
		try {
			MemberCalc memberCalc = getChildCalc(0, MemberCalc.class);
			return new UnaryTupleList(FunUtil.periodsToDate(evaluator, level, memberCalc.evaluate(evaluator)));
		} finally {
			evaluator.getTiming().markEnd(XtdWithMemberCalc.TIMING_NAME);
		}
	}
}