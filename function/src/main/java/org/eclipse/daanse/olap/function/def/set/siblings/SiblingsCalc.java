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
package org.eclipse.daanse.olap.function.def.set.siblings;

import java.util.List;

import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;

public class SiblingsCalc extends AbstractNestedTupleListCalc {

    protected SiblingsCalc(Type type, final MemberCalc memberCalc) {
        super(type, memberCalc);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        final Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        return new UnaryTupleList(memberSiblings(member, evaluator));
    }

    private List<Member> memberSiblings(Member member, Evaluator evaluator) {
        if (member.isNull()) {
            // the null member has no siblings -- not even itself
            return List.of();
        }
        Member parent = member.getParentMember();
        final CatalogReader schemaReader = evaluator.getCatalogReader();
        if (parent == null) {
            return schemaReader.getHierarchyRootMembers(member.getHierarchy());
        } else {
            return schemaReader.getMemberChildren(parent);
        }
    }

}
