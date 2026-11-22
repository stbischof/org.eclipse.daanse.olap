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
package org.eclipse.daanse.olap.function.def.ancestor;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class AncestorLevelCalc extends AbstractNestedMemberCalc {

	public AncestorLevelCalc(Type type, MemberCalc memberCalc, LevelCalc levelCalc) {
		super(type, memberCalc, levelCalc);

	}

	@Override
	public Member evaluateInternal(Evaluator evaluator) {
		final MemberCalc memberCalc = getChildCalc(0, MemberCalc.class);
		final LevelCalc levelCalc = getChildCalc(1, LevelCalc.class);
		
		Level level = levelCalc.evaluate(evaluator);
		Member member = memberCalc.evaluate(evaluator);
		
		int distance = member.getLevel().getDepth() - level.getDepth();
		return FunUtil.ancestor(evaluator, member, distance, level);
	}
}