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
package org.eclipse.daanse.olap.function.def.visualtotals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.VisualTotalMember;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.query.component.MemberExpressionImpl;
import org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl;

public class VisualTotalsCalc extends AbstractNestedTupleListCalc {
    private final TupleListCalc tupleListCalc;
    private final StringCalc stringCalc;

    public VisualTotalsCalc(
            ResolvedFunCall call, TupleListCalc tupleListCalc, StringCalc stringCalc)
    {
        super(call.getType(), new Calc[] {tupleListCalc, stringCalc});
        this.tupleListCalc = tupleListCalc;
        this.stringCalc = stringCalc;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        final List<Member> list =
            tupleListCalc.evaluate(evaluator).slice(0);
        final List<Member> resultList = new ArrayList<>(list);
        final int memberCount = list.size();
        for (int i = memberCount - 1; i >= 0; --i) {
            Member member = list.get(i);
            if (i + 1 < memberCount) {
                Member nextMember = resultList.get(i + 1);
                if (nextMember != member
                    && nextMember.isChildOrEqualTo(member))
                {
                    resultList.set(
                        i,
                        createMember(member, i, resultList, evaluator));
                }
            }
        }
        return new UnaryTupleList(resultList);
    }

    private VisualTotalMember createMember(
        Member member,
        int i,
        final List<Member> list,
        Evaluator evaluator)
    {
        final String name = member.getName();;
        final String caption;
        if (stringCalc != null) {
            final String namePattern = stringCalc.evaluate(evaluator);
            caption = substitute(namePattern, member.getCaption());
        } else {
            caption = member.getCaption();
        }
        final List<Member> childMemberList =
            followingDescendants(member, i + 1, list);
        final Expression exp = makeExpr(childMemberList);
        final Validator validator = evaluator.getQuery().createValidator();
        final Expression validatedExp = exp.accept(validator);
        return member.getHierarchy().createVisualTotalMember(member, name, caption, validatedExp);
    }

    private List<Member> followingDescendants(
        Member member, int i, final List<Member> list)
    {
        List<Member> childMemberList = new ArrayList<>();
        while (i < list.size()) {
            Member descendant = list.get(i);
            if (descendant.equals(member)) {
                // strict descendants only
                break;
            }
            if (!descendant.isChildOrEqualTo(member)) {
                break;
            }
            if (descendant instanceof VisualTotalMember visualTotalMember) {
                childMemberList.add(visualTotalMember);
                i = lastChildIndex(visualTotalMember.getMember(), i, list);
                continue;
            }
            childMemberList.add(descendant);
            ++i;
        }
        return childMemberList;
    }

    private int lastChildIndex(Member member, int start, List<?> list) {
        int i = start;
        while (true) {
            ++i;
            if (i >= list.size()) {
                break;
            }
            Member descendant = (Member) list.get(i);
            if (descendant.equals(member)) {
                // strict descendants only
                break;
            }
            if (!descendant.isChildOrEqualTo(member)) {
                break;
            }
        }
        return i;
    }

    private Expression makeExpr(final List<?> childMemberList) {
        Expression[] memberExprs = new Expression[childMemberList.size()];
        for (int i = 0; i < childMemberList.size(); i++) {
            final Member childMember = (Member) childMemberList.get(i);
            memberExprs[i] = new MemberExpressionImpl(childMember);
        }
        return new UnresolvedFunCallImpl(
                 new FunctionOperationAtom( "Aggregate"),
            new Expression[] {
                new UnresolvedFunCallImpl(
                    new BracesOperationAtom(),
                    memberExprs)
            });
    }
    
    /**
     * Substitutes a name into a pattern.
     *
     * Asterisks are replaced with the name,
     * double-asterisks are replaced with a single asterisk.
     * For example,
     *
     * substitute("** Subtotal - *",
     * "Dairy")
     *
     * returns
     *
     * "* Subtotal - Dairy"
     *
     * @param namePattern Pattern
     * @param name Name to substitute into pattern
     * @return Substituted pattern
     */
    public static String substitute(String namePattern, String name) {
        final StringBuilder buf = new StringBuilder(256);
        final int namePatternLen = namePattern.length();
        int startIndex = 0;

        while (true) {
            int endIndex = namePattern.indexOf('*', startIndex);

            if (endIndex == -1) {
                // No '*' left
                // append the rest of namePattern from startIndex onwards
                buf.append(namePattern.substring(startIndex));
                break;
            }

            // endIndex now points to the '*'; check for '**'
            ++endIndex;
            if (endIndex < namePatternLen
                && namePattern.charAt(endIndex) == '*')
            {
                // Found '**', replace with '*'
                 // Include first '*'.
                buf.append(namePattern.substring(startIndex, endIndex));
                // Skip over 2nd '*'
                ++endIndex;
            } else {
                // Found single '*' - substitute (omitting the '*')
                // Exclude '*'
                buf.append(namePattern.substring(startIndex, endIndex - 1));
                buf.append(name);
            }

            startIndex = endIndex;
        }

        return buf.toString();
    }

}