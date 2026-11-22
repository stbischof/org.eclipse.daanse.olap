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
package org.eclipse.daanse.olap.function.def.descendants;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.sort.Sorter;

public class DescendantsLeavesByDepthCalc extends AbstractNestedTupleListCalc {

    public DescendantsLeavesByDepthCalc(Type type, MemberCalc memberCalc, IntegerCalc depthCalc) {
        super(type, memberCalc, depthCalc);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        IntegerCalc depthCalc = getChildCalc(1, IntegerCalc.class);
        final Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        List<Member> result = new ArrayList<>();
        Integer depth = -1;
        if (depthCalc != null) {
            depth = depthCalc.evaluate(evaluator);
            if (depth < 0) {
                depth = -1; // no limit
            }
        }
        final CatalogReader schemaReader = evaluator.getCatalogReader();
        descendantsLeavesByDepth(member, result, schemaReader, depth);
        Sorter.hierarchizeMemberList(result, false);
        return new UnaryTupleList(result);
    }

    /**
     * Populates 'result' with the descendants at the leaf level at depth
     * 'depthLimit' or less. If 'depthLimit' is -1, does not apply a depth
     * constraint.
     */
    private void descendantsLeavesByDepth(final Member member, final List<Member> result,
            final CatalogReader schemaReader, final int depthLimit) {
        if (!schemaReader.isDrillable(member)) {
            if (depthLimit >= 0) {
                result.add(member);
            }
            return;
        }
        List<Member> children = new ArrayList<>();
        children.add(member);
        for (int depth = 0; depthLimit == -1 || depth <= depthLimit; ++depth) {
            children = schemaReader.getMemberChildren(children);
            if (children.isEmpty()) {
                throw Util.newInternal("drillable member must have children");
            }
            List<Member> nextChildren = new ArrayList<>();
            for (Member child : children) {
                // TODO: Implement this more efficiently. The current
                // implementation of isDrillable for a parent-child hierarchy
                // simply retrieves the children sees whether there are any,
                // so we end up fetching each member's children twice.
                if (schemaReader.isDrillable(child)) {
                    nextChildren.add(child);
                } else {
                    result.add(child);
                }
            }
            if (nextChildren.isEmpty()) {
                return;
            }
            children = nextChildren;
        }
    }

}
