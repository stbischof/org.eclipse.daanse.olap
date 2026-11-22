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
package org.eclipse.daanse.olap.function.def.nonstandard;

import java.util.List;

import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;

public class CalculatedChildCalc extends AbstractNestedMemberCalc {

    protected CalculatedChildCalc(Type type, final MemberCalc memberCalc, final StringCalc stringCalc) {
        super(type, memberCalc, stringCalc);
    }

    @Override
    public Member evaluateInternal(Evaluator evaluator) {
        Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        String name = getChildCalc(1, StringCalc.class).evaluate(evaluator);
        return getCalculatedChild(member, name, evaluator);
    }

    private Member getCalculatedChild(Member parent, String childName, Evaluator evaluator) {
        final CatalogReader schemaReader = evaluator.getQuery().getCatalogReader(true);
        Level childLevel = parent.getLevel().getChildLevel();
        if (childLevel == null) {
            return parent.getHierarchy().getNullMember();
        }
        List<Member> calcMemberList = schemaReader.getCalculatedMembers(childLevel);

        for (Member child : calcMemberList) {
            // the parent check is required in case there are parallel children
            // with the same names
            if (child.getParentMember().equals(parent) && child.getName().equals(childName)) {
                return child;
            }
        }

        return parent.getHierarchy().getNullMember();
    }
}
