/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.result;

import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.HierarchyExpression;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.query.component.UnresolvedFunCall;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.query.component.MdxVisitorImpl;

/**
 * Whether the rows behind a cell can still be named, given the expression that produced it.
 * <p>
 * A cell whose value comes from a formula usually has no rows of its own, but some formulas only
 * rename or combine things that do: a literal, a member, parentheses, the four arithmetic signs and
 * {@code CoalesceEmpty} all keep the rows nameable, everything else does not. Examples:
 * <ul>
 * <li>{@code 1} — nameable</li>
 * <li>{@code [Measures].[Amount]} — nameable</li>
 * <li>{@code [Measures].[Amount] + 1} — nameable</li>
 * <li>{@code ([Measures].[Amount], [Time].PrevMember)} — not nameable, it moves the context</li>
 * </ul>
 * <p>
 * The walk over the expression is the same for every provider; what a single member <em>means</em> is
 * not, so a provider answers that by overriding {@link #handleMember(Member)}. Giving up is reported
 * by throwing {@link #tooComplex()}, which the caller recognises with {@link #isTooComplex}.
 */
public class DrillThroughExpressionCheck extends MdxVisitorImpl {

    /**
     * The signal that the walk gave up. One instance, thrown by identity and never handed to a
     * caller, so no stack trace is paid for a decision.
     */
    private static final RuntimeException TOO_COMPLEX =
            new UnsupportedOperationException("the expression behind this cell names no rows");

    /** The signal to throw when the expression turns out to name no rows. */
    protected static RuntimeException tooComplex() {
        return TOO_COMPLEX;
    }

    /** Whether {@code e} is this check giving up rather than a failure of its own. */
    public static boolean isTooComplex(RuntimeException e) {
        return e == TOO_COMPLEX;
    }

    /**
     * What a single member means. The base follows the formula of a calculated member and lets a
     * plain member pass; a provider that knows more about its members says so here.
     */
    protected void handleMember(Member member) {
        if (member.isCalculated()) {
            Expression expression = member.getExpression();
            if (expression == null) {
                throw tooComplex();
            }
            expression.accept(this);
        }
    }

    /** Walks the members of a cell; false when one of them names no rows. */
    public final boolean accepts(Member[] members) {
        try {
            for (Member member : members) {
                handleMember(member);
            }
            return true;
        } catch (RuntimeException e) {
            if (isTooComplex(e)) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public Object visitMemberExpression(MemberExpression memberExpr) {
        handleMember(memberExpr.getMember());
        return null;
    }

    @Override
    public Object visitResolvedFunCall(ResolvedFunCall call) {
        final FunctionDefinition def = call.getFunDef();
        final Expression[] args = call.getArgs();
        final String name = def.getFunctionMetaData().operationAtom().name();
        if (name.equals("+")
            || name.equals("-")
            || name.equals("/")
            || name.equals("*")
            || name.equals("CoalesceEmpty")
            // Allow parentheses but don't allow tuple
            || name.equals("()") && args.length == 1)
        {
            return null;
        }
        throw tooComplex();
    }

    @Override
    public Object visitLiteral(Literal literal) {
        return null;
    }

    @Override
    public Object visitParameterExpression(ParameterExpression parameterExpr) {
        // Not valid in general; might contain complex expression
        throw tooComplex();
    }

    @Override
    public Object visitDimensionExpression(DimensionExpression dimensionExpr) {
        // Not valid in general; might be part of complex expression
        throw tooComplex();
    }

    @Override
    public Object visitHierarchyExpression(HierarchyExpression hierarchyExpr) {
        // Not valid in general; might be part of complex expression
        throw tooComplex();
    }

    @Override
    public Object visitLevelExpression(LevelExpression levelExpr) {
        // Not valid in general; might be part of complex expression
        throw tooComplex();
    }

    @Override
    public Object visitNamedSetExpression(NamedSetExpression namedSetExpr) {
        throw Util.newInternal("not valid here: " + namedSetExpr);
    }

    @Override
    public Object visitQuery(Query query) {
        throw Util.newInternal("not valid here: " + query);
    }

    @Override
    public Object visitQueryAxis(QueryAxis queryAxis) {
        throw Util.newInternal("not valid here: " + queryAxis);
    }

    @Override
    public Object visitFormula(Formula formula) {
        throw Util.newInternal("not valid here: " + formula);
    }

    @Override
    public Object visitUnresolvedFunCall(UnresolvedFunCall call) {
        throw Util.newInternal("expected resolved expression");
    }

    @Override
    public Object visitId(Id id) {
        throw Util.newInternal("expected resolved expression");
    }
}
