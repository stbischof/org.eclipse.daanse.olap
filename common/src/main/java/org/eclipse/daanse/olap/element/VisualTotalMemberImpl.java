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
package org.eclipse.daanse.olap.element;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.ParentChildMember;
import org.eclipse.daanse.olap.api.element.MetaData;
import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.element.VisualTotalMember;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.query.component.MemberExpressionImpl;
import org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The provider-neutral {@link VisualTotalMember}: a calculated member over
 * {@link MemberBase} that stands for the member it wraps.
 * <p>
 * It corresponds to a real member and most of its properties are the same.
 * The differences: its name comes from the VisualTotals pattern (e.g.
 * {@code "*Subtotal - Dairy"} instead of {@code "Dairy"}), and its value is
 * the aggregate of the members that follow it in the list.
 * <p>
 * A provider whose evaluator needs its own member type overrides
 * {@link HierarchyBase#createVisualTotalMember} and returns that type instead.
 */
public class VisualTotalMemberImpl extends CalculableMemberBase implements VisualTotalMember {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisualTotalMemberImpl.class);

    /** Solve order: high, so the total is expanded after other calculations. */
    public static final int SOLVE_ORDER = 99;

    private final Member member;
    private final String name;
    private Expression exp;

    public VisualTotalMemberImpl(Member member, String name, String caption, Expression exp) {
        super(member.getParentMember(), member.getLevel(),
                member.getMemberType() == MemberType.ALL ? MemberType.ALL : MemberType.FORMULA);
        this.member = member;
        this.name = name;
        this.caption = caption;
        this.exp = exp;
        this.uniqueName = member.getUniqueName();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getCaptionValue() {
        return caption;
    }

    @Override
    public String getCaption() {
        return caption;
    }

    @Override
    public boolean equals(Object o) {
        // A visual total member must compare equal to the member it wraps
        // (for purposes of the MDX Intersect function, for instance).
        return o instanceof VisualTotalMember that
                && this.member.equals(that.getMember())
                && this.exp.equals(that.getExpression())
                || o instanceof Member
                && this.member.equals(o);
    }

    @Override
    public int hashCode() {
        return member.hashCode();
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof VisualTotalMember that) {
            // VisualTotals members are a special case: compare the delegates.
            return this.getMember().compareTo(that.getMember());
        }
        return super.compareTo(o);
    }

    @Override
    protected boolean computeCalculated(final MemberType memberType) {
        return true;
    }

    @Override
    public int getSolveOrder() {
        return SOLVE_ORDER;
    }

    @Override
    public Expression getExpression() {
        return exp;
    }

    @Override
    public void setExpression(Expression exp) {
        this.exp = exp;
    }

    @Override
    public void setExpression(Evaluator evaluator, List<Member> childMembers) {
        final Expression unresolved = makeExpr(childMembers);
        final Validator validator = evaluator.getQuery().createValidator();
        setExpression(unresolved.accept(validator));
    }

    /** {@code Aggregate({child1, child2, ...})} over the given members. */
    public static Expression makeExpr(final List<Member> childMemberList) {
        Expression[] memberExprs = new Expression[childMemberList.size()];
        for (int i = 0; i < childMemberList.size(); i++) {
            memberExprs[i] = new MemberExpressionImpl(childMemberList.get(i));
        }
        return new UnresolvedFunCallImpl(new FunctionOperationAtom("Aggregate"),
                new Expression[] { new UnresolvedFunCallImpl(new BracesOperationAtom(), memberExprs) });
    }

    @Override
    public int getOrdinal() {
        return member.getOrdinal();
    }

    @Override
    public Member getDataMember() {
        return member;
    }

    @Override
    public String getQualifiedName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Member getMember() {
        return member;
    }

    @Override
    public Object getPropertyValue(String propertyName) {
        return getPropertyValue(propertyName, true);
    }

    @Override
    public Object getPropertyValue(String propertyName, boolean matchCase) {
        StandardProperty property = StandardProperty.lookup(propertyName, matchCase);
        if (property == null) {
            return null;
        }
        if (property == StandardProperty.CHILDREN_CARDINALITY) {
            return member.getPropertyValue(propertyName, matchCase);
        }
        return super.getPropertyValue(propertyName, matchCase);
    }

    @Override
    public boolean isCalculatedInQuery() {
        return false;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isParentChildLeaf() {
        return member.isParentChildLeaf();
    }

    @Override
    public int getDepth() {
        return member.getDepth();
    }

    @Override
    public MetaData getMetaData() {
        return member.getMetaData();
    }

    @Override
    public Property[] getProperties() {
        return member.getProperties();
    }

    @Override
    public void setProperty(String name, Object value) {
        throw new UnsupportedOperationException("a visual total member takes its properties from the member it wraps");
    }

    /** A visual total stands for a member, so the member behind it makes the leaf. */
    @Override
    public Member createPseudoLeafMember(Member child, String name) {
        if (member instanceof ParentChildMember parentChild) {
            return parentChild.createPseudoLeafMember(child, name);
        }
        throw new UnsupportedOperationException(
                "the member a visual total stands for has no children of its own");
    }

    /**
     * Always {@code null}: a visual total is not a row of any table, it is an aggregate over the
     * members that follow it, so there is no key that would address it.
     */
    @Override
    public Object getKey() {
        return null;
    }
}
