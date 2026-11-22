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
package org.eclipse.daanse.olap.function.def.member;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.fun.FunUtil;

public class AncestorsCalcForLevelType extends AbstractNestedTupleListCalc{

    protected AncestorsCalcForLevelType(Type type, final MemberCalc memberCalc, final LevelCalc levelCalc ) {
        super(type, memberCalc, levelCalc);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        Level level = getChildCalc(1, LevelCalc.class).evaluate(evaluator);
        Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        int distance = member.getDepth() - level.getDepth();
        List<Member> ancestors = new ArrayList<>();
        for (int curDist = 1; curDist <= distance; curDist++) {
            ancestors.add(FunUtil.ancestor(evaluator, member, curDist, null));
        }
        return TupleCollections.asTupleList(ancestors);
    }

}
