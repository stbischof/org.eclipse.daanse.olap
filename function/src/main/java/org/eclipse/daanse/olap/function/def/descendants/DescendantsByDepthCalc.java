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
import org.eclipse.daanse.olap.fun.sort.Sorter;

public class DescendantsByDepthCalc extends AbstractNestedTupleListCalc {

    private Flag flag;

    public DescendantsByDepthCalc(Type type, MemberCalc memberCalc, IntegerCalc depthCalc, final Flag flag) {
        super(type, memberCalc, depthCalc);
        this.flag = flag;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        final MemberCalc memberCalc = getChildCalc(0, MemberCalc.class);
        final IntegerCalc depthCalc = getChildCalc(1, IntegerCalc.class);
        final Member member = memberCalc.evaluate(evaluator);
        List<Member> result = new ArrayList<>();
        final Integer depth = depthCalc.evaluate(evaluator);
        final CatalogReader schemaReader = evaluator.getCatalogReader();
        descendantsByDepth(member, result, schemaReader, depth, flag.before, flag.self, flag.after, evaluator);
        Sorter.hierarchizeMemberList(result, false);
        return new UnaryTupleList(result);
    }

    private void descendantsByDepth(Member member, List<Member> result, final CatalogReader schemaReader,
            final int depthLimitFinal, final boolean before, final boolean self, final boolean after,
            final Evaluator context) {
        List<Member> children = new ArrayList<>();
        children.add(member);
        for (int depth = 0;; ++depth) {
            if (depth == depthLimitFinal) {
                if (self) {
                    result.addAll(children);
                }
                if (!after) {
                    break; // no more results after this level
                }
            } else if (depth < depthLimitFinal) {
                if (before) {
                    result.addAll(children);
                }
            } else {
                if (after) {
                    result.addAll(children);
                } else {
                    break; // no more results after this level
                }
            }

            if (context.isNonEmpty()) {
                children = schemaReader.getMemberChildren(children, context);
            } else {
                children = schemaReader.getMemberChildren(children);
            }
            if (children.isEmpty()) {
                break;
            }
        }
    }

}
