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
package org.eclipse.daanse.olap.function.def.member.lastsibling;

import java.util.List;

import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;

public class LastSiblingCalc extends AbstractNestedMemberCalc {

    protected LastSiblingCalc(Type type, final MemberCalc memberCalc) {
        super(type, memberCalc);
    }

    @Override
    public Member evaluateInternal(Evaluator evaluator) {
        Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        return lastSibling(member, evaluator);
    }

    private Member lastSibling(Member member, Evaluator evaluator) {
        Member parent = member.getParentMember();
        List<Member> children;
        final CatalogReader schemaReader = evaluator.getCatalogReader();
        if (parent == null) {
            if (member.isNull()) {
                return member;
            }
            children = schemaReader.getHierarchyRootMembers(member.getHierarchy());
        } else {
            children = schemaReader.getMemberChildren(parent);
        }
        return children.get(children.size() - 1);
    }

}
