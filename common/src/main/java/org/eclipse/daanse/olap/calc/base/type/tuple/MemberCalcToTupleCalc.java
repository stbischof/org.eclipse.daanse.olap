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

package org.eclipse.daanse.olap.calc.base.type.tuple;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleCalc;

public class MemberCalcToTupleCalc extends AbstractNestedTupleCalc {

	public MemberCalcToTupleCalc(Type type, MemberCalc memberCalc) {
		super(type, memberCalc);
	}

	@Override
	public Member[] evaluateInternal(Evaluator evaluator) {
		final Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
		if (member == null) {
			return null;
		} else {
			return new Member[] { member };
		}
	}
}