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
package org.eclipse.daanse.olap.evaluator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.MetaData;
import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.element.CalculableMemberBase;
import org.eclipse.daanse.olap.element.MemberBase;
import org.eclipse.daanse.olap.element.OlapMetaDataBase;
import org.eclipse.daanse.olap.util.type.TypeWrapperExp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The member that carries a compound slicer, for a provider that brings no member of its own
 * for it. It stands where the null member of the sliced hierarchy stands and evaluates the
 * aggregation over the tuples of the slicer.
 */
public class CompoundSlicerMemberImpl extends CalculableMemberBase implements CompoundSlicerMember {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompoundSlicerMemberImpl.class);

    private final Member nullMember;
    private final Calc<?> calc;
    private final TupleList tuples;
    private final int solveOrder;
    private final Map<String, Object> properties = new ConcurrentHashMap<>();

    public CompoundSlicerMemberImpl(Member nullMember, Calc<?> calc, TupleList tuples, int solveOrder) {
        super(null, nullMember.getLevel(), MemberType.NULL);
        this.nullMember = nullMember;
        this.calc = calc;
        this.tuples = tuples;
        this.solveOrder = solveOrder;
        this.uniqueName = nullMember.getUniqueName();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public boolean isEvaluated() {
        return true;
    }

    @Override
    public Expression getExpression() {
        return new TypeWrapperExp(calc.getType());
    }

    @Override
    public Calc<?> getCompiledExpression(EvaluatorRoot root) {
        return calc;
    }

    @Override
    public int getSolveOrder() {
        return solveOrder;
    }

    @Override
    public boolean containsAggregateFunction() {
        return true;
    }

    /** On the chain of {@code otherMember} when one of the slicer's tuples is. */
    @Override
    public boolean isOnSameHierarchyChain(Member otherMember) {
        return isOnSameHierarchyChainInternal((MemberBase) otherMember);
    }

    @Override
    public boolean isOnSameHierarchyChainInternal(MemberBase otherMember) {
        for (List<Member> tuple : tuples) {
            if (tuple.isEmpty() || !otherMember.getHierarchy().equals(tuple.get(0).getHierarchy())) {
                return false;
            }
            if (otherMember.isOnSameHierarchyChainInternal((MemberBase) tuple.get(0))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return nullMember.getName();
    }

    @Override
    public Object getCaptionValue() {
        return nullMember.getCaption();
    }

    @Override
    public int getOrdinal() {
        return nullMember.getOrdinal();
    }

    @Override
    public int getDepth() {
        return nullMember.getDepth();
    }

    /** The null member it stands for has no key. */
    @Override
    public Object getKey() {
        return null;
    }

    @Override
    public Object getPropertyValue(String propertyName) {
        return getPropertyValue(propertyName, true);
    }

    @Override
    public Object getPropertyValue(String propertyName, boolean matchCase) {
        Object own = properties.get(propertyName);
        return own != null ? own : nullMember.getPropertyValue(propertyName, matchCase);
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        if (value != null) {
            properties.put(propertyName, value);
        }
    }

    @Override
    public Property[] getProperties() {
        return nullMember.getProperties();
    }

    @Override
    public boolean isCalculatedInQuery() {
        return false;
    }

    @Override
    public void setName(String newName) {
        throw new UnsupportedOperationException("The member of a compound slicer has no name of its own");
    }

    @Override
    public Member createPseudoLeafMember(Member parent, String name) {
        throw new UnsupportedOperationException("The member of a compound slicer has no children");
    }

    @Override
    public int compareTo(Object other) {
        return other instanceof Member m ? getUniqueName().compareTo(m.getUniqueName()) : -1;
    }
}
