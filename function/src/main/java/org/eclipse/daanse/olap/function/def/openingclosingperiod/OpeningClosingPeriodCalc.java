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
package org.eclipse.daanse.olap.function.def.openingclosingperiod;

import java.util.List;

import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;
import org.eclipse.daanse.olap.common.Util;

public class OpeningClosingPeriodCalc extends AbstractNestedMemberCalc{

    private final boolean opening;
    
    protected OpeningClosingPeriodCalc(Type type, final LevelCalc levelCalc, final MemberCalc memberCalc, final boolean opening) {
        super(type, levelCalc, memberCalc);
        this.opening = opening;
    }

    @Override
    public Member evaluateInternal(Evaluator evaluator) {
        Member member = getChildCalc(1, MemberCalc.class).evaluate(evaluator);
        LevelCalc levelCalc = getChildCalc(0, LevelCalc.class);
        // If the level argument is present, use it. Otherwise use the
        // level immediately after that of the member argument.
        Level level;
        if (levelCalc == null) {
            int targetDepth = member.getLevel().getDepth() + 1;
            List<? extends Level> levels = member.getHierarchy().getLevels();

            if (levels.size() <= targetDepth) {
                return member.getHierarchy().getNullMember();
            }
            level = levels.get(targetDepth);
        } else {
            level = levelCalc.evaluate(evaluator);
        }

        // Shortcut if the level is above the member.
        if (level.getDepth() < member.getLevel().getDepth()) {
            return member.getHierarchy().getNullMember();
        }

        // Shortcut if the level is the same as the member
        if (level == member.getLevel()) {
            return member;
        }

        return getDescendant(
            evaluator.getCatalogReader(), member,
            level, opening, evaluator);
    }

    /**
     * Returns the first or last descendant of the member at the target level.
     * This method is the implementation of both OpeningPeriod and
     * ClosingPeriod.
     *
     * @param schemaReader The schema reader to use to evaluate the function.
     * @param member The member from which the descendant is to be found.
     * @param targetLevel The level to stop at.
     * @param returnFirstDescendant Flag indicating whether to return the first
     * or last descendant of the member.
     * @return A member.
     *  member.getLevel().getDepth() < level.getDepth();
     */
    private Member getDescendant(
        CatalogReader schemaReader,
        Member member,
        Level targetLevel,
        boolean returnFirstDescendant,
        Evaluator evaluator)
    {
        List<Member> children;

        final int targetLevelDepth = targetLevel.getDepth();
        Util.assertPrecondition(member.getLevel().getDepth() < targetLevelDepth,
                "member.getLevel().getDepth() < targetLevel.getDepth()");

        for (;;) {
            children = schemaReader.getMemberChildren(member);

            if (children.size() == 0) {
                return targetLevel.getHierarchy().getNullMember();
            }

            final int index =
                returnFirstDescendant ? 0 : (children.size() - 1);
            member = children.get(index);
            if (member.getLevel().getDepth() == targetLevelDepth) {
                if (member.isHidden()) {
                    return member.getHierarchy().getNullMember();
                } else {
                    return member;
                }
            }
        }
    }

}
