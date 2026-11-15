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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.evaluator.CalculableMember;
import org.eclipse.daanse.olap.evaluator.EvaluatorImpl;
import org.eclipse.daanse.olap.evaluator.EvaluatorRoot;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;

/**
 * A {@link MemberBase} that also takes part in the calculation order.
 * <p>
 * Three of the answers {@link CalculableMember} asks for are the same whatever a member is made of:
 * where its hierarchy sits in the cube, what it does to the context while it is being worked out, and
 * how its formula is compiled. They are given here once.
 * <p>
 * The answers that do differ stay with the member: its key, whether its formula aggregates, and how it
 * makes a pseudo leaf. A member that is not calculable does not extend this class, so the evaluator can
 * still refuse one that has no business in a context.
 */
public abstract class CalculableMemberBase extends MemberBase implements CalculableMember {

    private Boolean containsAggregateFunction;

    protected CalculableMemberBase(Member parentMember, Level level, MemberType memberType) {
        super(parentMember, level, memberType);
    }

    /** For a member that is put together after construction, as {@link MemberBase} allows. */
    protected CalculableMemberBase() {
        super();
    }

    @Override
    public int getHierarchyOrdinal() {
        return getHierarchy().getOrdinalInCube();
    }

    /**
     * Puts the hierarchy back to its default member and names this member as the one being worked out.
     * Removing the calculation again happens in {@code setContext} itself.
     */
    @Override
    public void setContextIn(EvaluatorImpl evaluator) {
        evaluator.setContext(evaluator.getRoot().defaultMembers[getHierarchyOrdinal()]);
        evaluator.setExpanding(this);
    }

    @Override
    public Calc<?> getCompiledExpression(EvaluatorRoot root) {
        return root.getCompiled(getExpression(), true, null);
    }

    /**
     * The formula that makes this a calculated member, or {@code null} when it carries none. Where an art
     * keeps its formula is its own business -- one holds it final from its constructor, another is given it
     * afterwards -- so the formula is asked for here rather than held here.
     *
     * @return the formula, or null
     */
    protected Formula formula() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>What the formula says, and nothing when there is no formula.
     */
    @Override
    public Expression getExpression() {
        Formula formula = formula();
        return formula == null ? null : formula.getExpression();
    }

    /**
     * {@inheritDoc}
     *
     * <p>What the formula says. A member that carries none keeps the answer of a member that has no order
     * to be placed by, and a formula that names no order counts as zero -- the two are not the same, and
     * the engine compares them.
     */
    @Override
    public int getSolveOrder() {
        Formula formula = formula();
        if (formula == null) {
            return super.getSolveOrder();
        }
        Number order = formula.getSolveOrder();
        return order == null ? 0 : order.intValue();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only a member a query itself wrote says yes. A catalog publishes its members through the same
     * clause but marks them as belonging to their cube, and the engine places, expands and secures the two
     * differently.
     */
    @Override
    public boolean isCalculatedInQuery() {
        if (formula() == null) {
            return false;
        }
        Object scope = getPropertyValue(StandardProperty.MEMBER_SCOPE.getName());
        return scope == null || "QUERY".equals(scope);
    }

  /**
   * Whether the expression of this member aggregates a set somewhere, which the engine asks to place the
   * calculation in the scope it belongs to. Searching the expression is not free, so the answer is kept.
   */
  @Override
  public boolean containsAggregateFunction() {
    if ( containsAggregateFunction == null ) {
      containsAggregateFunction = aggregatesSomewhere( getExpression() );
    }
    return containsAggregateFunction;
  }

  private static boolean aggregatesSomewhere( Expression expression ) {
    if ( !( expression instanceof ResolvedFunCall call ) ) {
      return false;
    }
    if ( call.getFunDef().aggregates() ) {
      return true;
    }
    for ( Expression argument : call.getArgs() ) {
      if ( aggregatesSomewhere( argument ) ) {
        return true;
      }
    }
    return false;
  }

}
