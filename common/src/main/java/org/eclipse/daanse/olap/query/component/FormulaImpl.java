/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2000-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * Copyright (C) 2021 Sergei Semenkov
 * All Rights Reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */


package org.eclipse.daanse.olap.query.component;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.CalculatedMember;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.FunctionCall;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.MemberProperty;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.common.AbstractQueryPart;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.element.OlapMetaDataBase;
import org.eclipse.daanse.olap.element.SetBase;
import org.eclipse.daanse.olap.exceptions.MdxMemberExpIsSetException;
import org.eclipse.daanse.olap.util.type.TypeUtil;



/**
 * A Formula is a clause in an MDX query which defines a Set or a
 * Member.
 */
public class FormulaImpl extends AbstractQueryPart implements Formula {

    /** name of set or member */
    private final Id id;
    /** defining expression */
    private Expression exp;
    // properties/solve order of member
    private final MemberProperty[] memberProperties;

    /**
     * true is this is a member,
     * false if it is a set.
     */
    private final boolean isMember;

    private Member mdxMember;
    private NamedSet mdxSet;
    private final static String mdxCalculatedHierarchyError = "Hierarchy for calculated member ''{0}'' not found";
    private final static String mdxSetExpNotSet = "Set expression ''{0}'' must be a set";

    /**
     * Constructs formula specifying a set.
     */
    public FormulaImpl(Id id, Expression exp) {
        this(false, id, exp, new MemberProperty[0], null, null);
        createElement(null);
    }

    /**
     * Constructs a formula specifying a member.
     */
    public FormulaImpl(
        Id id,
        Expression exp,
        MemberProperty[] memberProperties)
    {
        this(true, id, exp, memberProperties, null, null);
    }

    FormulaImpl(
        boolean isMember,
        Id id,
        Expression exp,
        MemberProperty[] memberProperties,
        Member mdxMember,
        NamedSet mdxSet)
    {
        this.isMember = isMember;
        this.id = id;
        this.exp = exp;
        this.memberProperties = memberProperties;
        this.mdxMember = mdxMember;
        this.mdxSet = mdxSet;
        assert !(!isMember && mdxMember != null);
        assert !(isMember && mdxSet != null);
    }

    public FormulaImpl(Formula formula) {
        this(formula.isMember(),
            formula.getId(),
            formula.getExp().cloneExp(),
            MemberPropertyImpl.cloneArray(formula.getPemberProperties()),
            formula.getMdxMember(),
            formula.getMdxSet());
    }

    static Formula[] cloneArray(Formula[] x) {
        Formula[] x2 = new Formula[x.length];
        for (int i = 0; i < x.length; i++) {
            x2[i] = new FormulaImpl(x[i]);
        }
        return x2;
    }

    /**
     * Resolves identifiers into objects.
     *
     * @param validator Validation context to resolve the identifiers in this
     *   formula
     */
    @Override
    public void accept(Validator validator) {
        final boolean scalar = isMember;
        exp = validator.validate(exp, scalar);
        String idInner = this.id.toString();
        final Type type = exp.getType();
        if (isMember) {
            if (!TypeUtil.canEvaluate(type)) {
                throw new MdxMemberExpIsSetException(
                    exp.toString());
            }
        } else {
            if (!TypeUtil.isSet(type)) {
                throw new OlapRuntimeException(MessageFormat.format(mdxSetExpNotSet, idInner));
            }
        }
        for (MemberProperty memberProperty : memberProperties) {
            validator.validate(memberProperty);
        }
        // Get the format expression from the property list, or derive it from
        // the formula.
        if (isMember) {
            Expression formatExp = getFormatExp(validator);
            if (formatExp != null) {
                mdxMember.setProperty(
                    StandardProperty.FORMAT_EXP_PARSED.getName(), formatExp);
                mdxMember.setProperty(
                		StandardProperty.FORMAT_EXP.getName(), Util.unparse(formatExp));
            }

            final List<MemberProperty> memberPropertyList =
                new ArrayList<>(Arrays.asList(memberProperties));

            // put CELL_FORMATTER_SCRIPT_LANGUAGE first, if it exists; we must
            // see it before CELL_FORMATTER_SCRIPT.
            for (int i = 0; i < memberPropertyList.size(); i++) {
                MemberProperty memberProperty = memberPropertyList.get(i);
                if (memberProperty.getName().equals(
                		StandardProperty.CELL_FORMATTER_SCRIPT_LANGUAGE.getName()))
                {
                    memberPropertyList.remove(i);
                    memberPropertyList.add(0, memberProperty);
                }
            }

            // For each property of the formula, make it a property of the
            // member.
            for (MemberProperty memberProperty : memberPropertyList) {
                if (StandardProperty.FORMAT_PROPERTIES.contains(
                        memberProperty.getName()))
                {
                    continue; // we already dealt with format_string props
                }
                final Expression expInner = memberProperty.getExp();
                if (expInner instanceof Literal literal) {
                    String value = String.valueOf(literal.getValue());
                    mdxMember.setProperty(memberProperty.getName(), value);
                }
            }
        }
    }

    /**
     * Creates the {@link Member} or {@link NamedSet} object which this formula
     * defines.
     */
    @Override
    public void createElement(Query q) {
        // first resolve the name, bit by bit
        final List<Segment> segments = id.getSegments();
        if (isMember) {
            if (mdxMember != null) {
                return;
            }
            OlapElement mdxElement = q.getCube();
            final CatalogReader schemaReader = q.getCatalogReader(false);
            for (int i = 0; i < segments.size(); i++) {
                final Segment segment0 = segments.get(i);
                if (!(segment0 instanceof NameSegment segment)) {
                    throw Util.newError(
                        "Calculated member name must not contain member keys");
                }
                OlapElement parent = mdxElement;
                mdxElement = null;
                // The last segment of the id is the name of the calculated
                // member so no need to look for a pre-existing child.  This
                // avoids unnecessarily executing SQL and loading children into
                // cache.
                if (i != segments.size() - 1) {
                    mdxElement = schemaReader.getElementChild(parent, segment);
                }

                // Don't try to look up the member which the formula is
                // defining. We would only find one if the member is overriding
                // a member at the cube or schema level, and we don't want to
                // change that member's properties.
                if (mdxElement == null || i == segments.size() - 1) {
                    // this part of the name was not found... define it
                    Level level;
                    Member parentMember = null;
                    if (parent instanceof Member member) {
                        parentMember = member;
                        level = parentMember.getLevel().getChildLevel();
                        if (level == null) {
                            throw Util.newError(
                                new StringBuilder("The '")
                                    .append(segment)
                                    .append("' calculated member cannot be created ")
                                    .append("because its parent is at the lowest level ")
                                    .append("in the ")
                                    .append(parentMember.getHierarchy().getUniqueName())
                                    .append(" hierarchy.").toString());
                        }
                    } else {
                        final Hierarchy hierarchy;
                        if (parent instanceof Dimension dimension)
                        {
                            if (dimension.getHierarchies().size() == 1) {
                                hierarchy = dimension.getHierarchies().getFirst();
                            } else {
                                hierarchy = null;
                            }
                        } else {
                            hierarchy = parent.getHierarchy();
                        }
                        if (hierarchy == null) {
                            throw new OlapRuntimeException(
                                MessageFormat.format(mdxCalculatedHierarchyError, id.toString()));
                        }
                        level = hierarchy.getLevels().getFirst();
                    }
                    if (parentMember != null
                        && parentMember.isCalculated())
                    {
                        throw Util.newError(
                            new StringBuilder("The '")
                                .append(parent)
                                .append("' calculated member cannot be used as a parent")
                                .append(" of another calculated member.").toString());
                    }
                    Member mdxMemberInner =
                        level.getHierarchy().createMember(
                            parentMember, level, segment.getName(), this);
                    assert mdxMemberInner != null;
                    mdxElement = mdxMemberInner;
                }
            }
            this.mdxMember = (Member) mdxElement;
        } else {
            // don't need to tell query... it's already in query.formula
            Util.assertTrue(
                segments.size() == 1,
                "set names must not be compound");
            final Segment segment0 = segments.get(0);
            if (!(segment0 instanceof NameSegment)) {
                throw Util.newError(
                    "Calculated member name must not contain member keys");
            }
            // Caption and description are initialized to null, and annotations
            // to the empty map. If named set is defined in the schema, we will
            // give these their true values later.
            mdxSet =
                new SetBase(
                    ((NameSegment) segment0).getName(),
                    null,
                    null,
                    exp,
                    false,
                    OlapMetaDataBase.empty());
        }
    }

    @Override
	public Object[] getChildren() {
        Object[] children = new Object[1 + memberProperties.length];
        children[0] = exp;
        System.arraycopy(
            memberProperties, 0,
            children, 1, memberProperties.length);
        return children;
    }

    @Override
	public void unparse(PrintWriter pw)
    {
        if (isMember) {
            pw.print("member ");
            if (mdxMember != null) {
                pw.print(mdxMember.getUniqueName());
            } else {
                id.unparse(pw);
            }
        } else {
            pw.print("set ");
            id.unparse(pw);
        }
        pw.print(" as '");
        exp.unparse(pw);
        pw.print("'");
        if (memberProperties != null) {
            for (MemberProperty memberProperty : memberProperties) {
                pw.print(", ");
                memberProperty.unparse(pw);
            }
        }
    }

    @Override
    public boolean isMember() {
        return isMember;
    }

    @Override
    public NamedSet getNamedSet() {
        return mdxSet;
    }

    /**
     * Returns the Identifier of the set or member which is declared by this
     * Formula.
     *
     * @return Identifier
     */
    @Override
    public Id getIdentifier() {
        return id;
    }

    /** Returns this formula's name. */
    @Override
    public String getName() {
        return (isMember)
            ? mdxMember.getName()
            : mdxSet.getName();
    }

    /** Returns this formula's caption. */
    @Override
    public String getCaption() {
        return (isMember)
            ? mdxMember.getCaption()
            : mdxSet.getName();
    }

    /**
     * Changes the last part of the name to newName. For example,
     * [Abc].[Def].[Ghi] becomes [Abc].[Def].[Xyz];
     * and the member or set is renamed from Ghi to
     * Xyz.
     */
    @Override
    public void rename(String newName)
    {
        String oldName = getElement().getName();
        final List<Segment> segments = this.id.getSegments();
        assert Util.last(segments) instanceof NameSegment;
        assert ((NameSegment) Util.last(segments)).getName()
            .equalsIgnoreCase(oldName);
        segments.set(
            segments.size() - 1,
            new IdImpl.NameSegmentImpl(newName));
        if (isMember) {
            mdxMember.setName(newName);
        } else {
            mdxSet.setName(newName);
        }
    }

    /** Returns the unique name of the member or set. */
    @Override
    public String getUniqueName() {
        return (isMember)
            ? mdxMember.getUniqueName()
            : mdxSet.getUniqueName();
    }

    @Override
    public OlapElement getElement() {
        return (isMember)
            ? (OlapElement) mdxMember
            : (OlapElement) mdxSet;
    }

    @Override
    public Expression getExpression() {
        return exp;
    }

    @Override
    public Expression setExpression(Expression exp) {
        return this.exp = exp;
    }

    private Expression getMemberProperty(String name) {
        return MemberPropertyImpl.get(memberProperties, name);
    }

    /**
     * Returns the Member. (Not valid if this formula defines a set.)
     *
     *  isMember()
     *  return != null
     */
    @Override
    public Member getMdxMember() {
        return mdxMember;
    }

    /**
     * Returns the solve order. (Not valid if this formula defines a set.)
     *
     *  isMember()
     * @return Solve order, or null if SOLVE_ORDER property is not specified
     *   or is not a number or is not constant
     */
    @Override
    public Number getSolveOrder() {
        return getIntegerMemberProperty(StandardProperty.SOLVE_ORDER.getName());
    }

    /**
     * Returns the integer value of a given constant.
     * If the property is not set, or its
     * value is not an integer, or its value is not a constant,
     * returns null.
     *
     * @param name Property name
     * @return Value of the property, or null if the property is not set, or its
     *   value is not an integer, or its value is not a constant.
     */
    private Number getIntegerMemberProperty(String name) {
        Expression expInner = getMemberProperty(name);
        if (expInner != null && expInner.getType() instanceof NumericType) {
            return quickEval(expInner);
        }
        return null;
    }

    /**
     * Evaluates a constant numeric expression.
     * @param exp Expression
     * @return Result as a number, or null if the expression is not a constant
     *   or not a number.
     */
    private static Number quickEval(Expression exp) {
        if (exp instanceof Literal literal) {
            final Object value = literal.getValue();
            if (value instanceof Number number) {
                return number;
            } else {
                return null;
            }
        }

        if (exp instanceof FunctionCall call && call.getOperationAtom().name().equals("-")
                && call.getOperationAtom() instanceof PrefixOperationAtom) {
                final Number number = quickEval(call.getArg(0));
                if (number == null) {
                    return null;
                } else if (number instanceof Integer) {
                    return - number.intValue();
                } else {
                    return - number.doubleValue();
                }
        }
        return null;
    }

    /**
     * Deduces a formatting expression for this calculated member. First it
     * looks for properties called "format", "format_string", etc. Then it looks
     * inside the expression, and returns the formatting expression for the
     * first member it finds.
     * @param validator
     */
    private Expression getFormatExp(Validator validator) {
        // If they have specified a format string (which they can do under
        // several names) return that.
        for (String prop : StandardProperty.FORMAT_PROPERTIES) {
            Expression formatExp = getMemberProperty(prop);
            if (formatExp != null) {
                return formatExp;
            }
        }

        // Choose a format appropriate to the expression.
        // For now, only do it for decimals.
        final Type type = exp.getType();
        if (type instanceof DecimalType decimalType) {
            int scale = decimalType.getScale();
            String formatString = "#,##0";
            if (scale > 0) {
                formatString = new StringBuilder(formatString).append(".").toString();
                while (scale-- > 0) {
                    formatString = new StringBuilder(formatString).append("0").toString();
                }
            }
            return StringLiteralImpl.create(formatString);
        }

        if (!mdxMember.isMeasure()) {
            // Don't try to do any format string inference on non-measure
            // calculated members; that can hide the correct formatting
            // from base measures (see TestCalculatedMembers.testFormatString
            // for an example).
            return null;
        }

        // Burrow into the expression. If we find a member, use its format
        // string.
        try {
            exp.accept(new FormatFinder(validator));
            return null;
        } catch (FoundOne foundOne) {
            return foundOne.exp;
        }
    }

    @Override
    public void compile() {
        // nothing to do
    }

    @Override
    public Id getId() {
        return id;
    }

    @Override
    public Expression getExp() {
        return exp;
    }

    @Override
    public NamedSet getMdxSet() {
        return mdxSet;
    }

    @Override
    public MemberProperty[] getPemberProperties() {
        return memberProperties;
    }

    /**
     * Accepts a visitor to this Formula.
     * The default implementation dispatches to the
     * {@link QueryComponentVisitor#visitFormula(FormulaImpl)} method.
     *
     * @param visitor Visitor
     */
    @Override
    public Object accept(QueryComponentVisitor visitor) {
        final Object o = visitor.visitFormula(this);

        if (visitor.visitChildren()) {
            // visit the expression
            exp.accept(visitor);
        }
        return o;
    }

    private static class FoundOne extends RuntimeException {
        private final Expression exp;

        public FoundOne(Expression exp) {
            super();
            this.exp = exp;
        }
    }

    /**
     *A visitor for burrowing format information given a member.
     */
    private static class FormatFinder extends MdxVisitorImpl {
        private final Validator validator;

        /**
         *
         * @param validator to resolve unresolved expressions
         */
        public FormatFinder(Validator validator) {
            this.validator = validator;
        }

        @Override
		public Object visitMemberExpression(MemberExpression memberExpr) {
            Member member = memberExpr.getMember();
            returnFormula(member);
            if (member.isCalculated()
                    && member instanceof CalculatedMember rolapCalculatedMember
                    && !hasCyclicReference(memberExpr))
            {
                Formula formula = rolapCalculatedMember.getFormula();
                formula.accept(validator);
                returnFormula(member);
            }

            return super.visitMemberExpression(memberExpr);
        }

        /**
         *
         * @param expr
         * @return true if there is cyclic reference in expression.
         * This check is required to avoid infinite recursion
         */
        private boolean hasCyclicReference(Expression expr) {
            List<MemberExpression> expList = new ArrayList<>();
            return hasCyclicReference(expr, expList);
        }

        private boolean hasCyclicReference(Expression expr, List<MemberExpression> expList) {
            if (expr instanceof MemberExpression memberExpr) {
                if (expList.contains(expr)) {
                    return true;
                }
                expList.add(memberExpr);
                Member member = memberExpr.getMember();
                if (member instanceof CalculatedMember calculatedMember) {
                    Expression exp1 =
                        calculatedMember.getExpression().accept(validator);
                    return hasCyclicReference(exp1, expList);
                }
            }
            if (expr instanceof FunctionCall funCall) {
                Expression[] exps = funCall.getArgs();
                for (int i = 0; i < exps.length; i++) {
                    if (hasCyclicReference(
                            exps[i], cloneForEachBranch(expList)))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        private List<MemberExpression> cloneForEachBranch(List<MemberExpression> expList) {
            return new ArrayList<>(expList);
        }

        private void returnFormula(Member member) {
            if (getFormula(member) != null) {
                throw new FoundOne(getFormula(member));
            }
        }

        private Expression getFormula(Member member) {
            return (Expression)
                member.getPropertyValue(StandardProperty.FORMAT_EXP_PARSED.getName());
        }
    }
}
