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
package org.eclipse.daanse.olap.function.def.periodstodate;

import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.fun.FunUtil;

public class PeriodsToDateCalc extends AbstractNestedTupleListCalc {

    private static final String TIMING_NAME = PeriodsToDateFunDef.class.getSimpleName();

    private final Hierarchy timeHierarchy;

    public PeriodsToDateCalc(Type type, LevelCalc levelCalc, MemberCalc memberCalc,
            final Hierarchy timeHierarchy) {
        super(type, levelCalc, memberCalc);
        this.timeHierarchy = timeHierarchy;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        LevelCalc levelCalc = getChildCalc(0, LevelCalc.class);
        MemberCalc memberCalc = getChildCalc(1, MemberCalc.class);
        evaluator.getTiming().markStart(TIMING_NAME);
        try {
            final Member member;
            final Level level;
            if (levelCalc == null) {
                member = evaluator.getContext(timeHierarchy);
                level = member.getLevel().getParentLevel();
            } else {
                level = levelCalc.evaluate(evaluator);
                if (memberCalc == null) {
                    member = evaluator.getContext(level.getHierarchy());
                } else {
                    member = memberCalc.evaluate(evaluator);
                }
            }
            return new UnaryTupleList(FunUtil.periodsToDate(evaluator, level, member));
        } finally {
            evaluator.getTiming().markEnd(TIMING_NAME);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        LevelCalc levelCalc = getChildCalc(0, LevelCalc.class);
        MemberCalc memberCalc = getChildCalc(1, MemberCalc.class);
        if (super.dependsOn(hierarchy)) {
            return true;
        }
        if (memberCalc != null) {
            return false;
        } else if (levelCalc != null) {
            return levelCalc.getType().usesHierarchy(hierarchy, true);
        } else {
            return hierarchy == timeHierarchy;
        }
    }

}
