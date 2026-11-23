 /*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1998-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
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
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.query.component;

import java.io.PrintWriter;
import java.text.MessageFormat;

import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.AxisOrdinal;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.FunctionCall;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.result.SubtotalVisibility;
import org.eclipse.daanse.olap.api.type.DimensionType;
import org.eclipse.daanse.olap.api.type.HierarchyType;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.common.AbstractQueryPart;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.util.type.TypeUtil;

/**
 * An axis in an MDX query. For example, the typical MDX query has two axes,
 * which appear as the "ON COLUMNS" and "ON ROWS" clauses.
 *
 * @author jhyde, 20 January, 1999
 */
public class QueryAxisImpl extends AbstractQueryPart implements QueryAxis {

    private boolean nonEmpty;
    private boolean ordered;
    private Expression exp;
    private final AxisOrdinal axisOrdinal;

    /**
     * Whether to show subtotals on this axis.
     * The "(show\hide)Subtotals" operation changes its valud.
     */
    private SubtotalVisibility subtotalVisibility;
    private final Id[] dimensionProperties;
    private final static String mdxAxisIsNotSet = "Axis ''{0}'' expression is not a set";

    /**
     * Creates an axis.
     *
     * @param nonEmpty Whether to filter out members of this axis whose cells
     *    are all empty
     * @param set Expression to populate the axis
     * @param axisOrdinal Which axis (ROWS, COLUMNS, etc.)
     * @param subtotalVisibility Whether to show subtotals
     * @param dimensionProperties List of dimension properties
     */
    public QueryAxisImpl(
        boolean nonEmpty,
        Expression set,
        AxisOrdinal axisOrdinal,
        SubtotalVisibility subtotalVisibility,
        Id[] dimensionProperties)
    {
        if (dimensionProperties == null || axisOrdinal == null) {
            throw new IllegalArgumentException("QueryAxis: dimensionProperties and axisOrdinal should not be null");
        }
        // enableNonEmptyOnAllAxis is applied by QueryImpl, not here: axes are built
        // while parsing, where no Context is bound, so an axis cannot read it.
        this.nonEmpty = nonEmpty;
        this.exp = set;
        this.axisOrdinal = axisOrdinal;
        this.subtotalVisibility = subtotalVisibility;
        this.dimensionProperties = dimensionProperties;
        this.ordered = false;
    }

    /**
     * Creates an axis with no dimension properties.
     *
     * QueryAxis(boolean,Exp,AxisOrdinal,mondrian.olap.QueryAxisImpl.SubtotalVisibility,Id[])
     */
    public QueryAxisImpl(
        boolean nonEmpty,
        Expression set,
        AxisOrdinal axisOrdinal,
        SubtotalVisibility subtotalVisibility)
    {
        this(nonEmpty, set, axisOrdinal, subtotalVisibility, new Id[0]);
    }

    public QueryAxisImpl(QueryAxis queryAxis) {
        this(
            queryAxis.isNonEmpty(), queryAxis.getSet().cloneExp(), queryAxis.getAxisOrdinal(),
            queryAxis.getSubtotalVisibility(), queryAxis.getDimensionProperties().clone());
    }

    static QueryAxis[] cloneArray(QueryAxis[] a) {
        QueryAxisImpl[] a2 = new QueryAxisImpl[a.length];
        for (int i = 0; i < a.length; i++) {
            a2[i] = new QueryAxisImpl(a[i]);
        }
        return a2;
    }

    public Object accept(QueryComponentVisitor visitor) {
        final Object o = visitor.visitQueryAxis(this);

        if (visitor.visitChildren()) {
            // visit the expression which forms the axis
            exp.accept(visitor);
        }
        return o;
    }

    @Override
    public Calc<?> compile(ExpressionCompiler compiler, ResultStyle resultStyle) {
        Expression expInner = this.exp;
        if (axisOrdinal.isFilter()) {
            expInner = normalizeSlicerExpression(expInner);
            expInner = expInner.accept(compiler.getValidator());
        }
        switch (resultStyle) {
        case LIST:
            return compiler.compileList(expInner);
        case MUTABLE_LIST:
            return compiler.compileMutableList(expInner);
        case ITERABLE:
            return compiler.compileIterable(expInner);
        default:
            throw Util.unexpected(resultStyle);
        }
    }

    private static Expression normalizeSlicerExpression(Expression exp) {
        Expression slicer = exp;
        if (slicer instanceof LevelExpression
            || slicer instanceof HierarchyExpressionImpl
            || slicer instanceof DimensionExpression)
        {
			slicer = new UnresolvedFunCallImpl(new PlainPropertyOperationAtom("DefaultMember"),
					new Expression[] { slicer });
        }

        if (slicer instanceof FunctionCall funCall
            && funCall.getOperationAtom() instanceof ParenthesesOperationAtom)
        {
            slicer =
                new UnresolvedFunCallImpl(
                    new BracesOperationAtom(), new Expression[] {slicer});
        } else {
            slicer =
                new UnresolvedFunCallImpl(
                		new BracesOperationAtom(), new Expression[] {
                        new UnresolvedFunCallImpl(
                        		new ParenthesesOperationAtom(), new Expression[] {
                                slicer})});
        }

        return slicer;
    }

    @Override
    public String getAxisName() {
        return axisOrdinal.name();
    }

    /**
     * Returns the ordinal of this axis, for example
     * {@link org.eclipse.daanse.olap.api.query.component.AxisOrdinal.StandardAxisOrdinal#ROWS}.
     */
    @Override
    public AxisOrdinal getAxisOrdinal() {
        return axisOrdinal;
    }

    /**
     * Returns whether the axis has the NON EMPTY property set.
     */
    @Override
    public boolean isNonEmpty() {
        return nonEmpty;
    }

    /**
     * Sets whether the axis has the NON EMPTY property set.
     * See {@link #isNonEmpty()}.
     */
    public void setNonEmpty(boolean nonEmpty) {
        this.nonEmpty = nonEmpty;
    }

     /**
     * Returns whether the axis has the ORDER property set.
     */
    @Override
    public boolean isOrdered() {
        return ordered;
    }

    /**
     * Sets whether the axis has the ORDER property set.
     */
    @Override
    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }

    /**
     * Returns the expression which is used to compute the value of this axis.
     */
    @Override
    public Expression getSet() {
        return exp;
    }



    /**
     * Sets the expression which is used to compute the value of this axis.
     * See {@link #getSet()}.
     */
    @Override
    public void setSet(Expression set) {
        this.exp = set;
    }

    @Override
    public void resolve(Validator validator) {
        exp = validator.validate(exp, false);
        final Type type = exp.getType();
        if (!TypeUtil.isSet(type)) {
            // If expression is a member or a tuple, implicitly convert it
            // into a set. Dimensions and hierarchies can be converted to
            // members, thence to sets.
            if (type instanceof MemberType
                || type instanceof TupleType
                || type instanceof DimensionType
                || type instanceof HierarchyType)
            {
                exp =
                    new UnresolvedFunCallImpl(
                    		new BracesOperationAtom(),
                        new Expression[] {exp});
                exp = validator.validate(exp, false);
            } else {
                throw new OlapRuntimeException(MessageFormat.format(mdxAxisIsNotSet,
                    axisOrdinal.name()));
            }
        }
    }

    @Override
	public Object[] getChildren() {
        return new Object[] {exp};
    }

    @Override
	public void unparse(PrintWriter pw) {
        if (nonEmpty) {
            pw.print("NON EMPTY ");
        }
        if (exp != null) {
            exp.unparse(pw);
        }
        if (dimensionProperties.length > 0) {
            pw.print(" DIMENSION PROPERTIES ");
            for (int i = 0; i < dimensionProperties.length; i++) {
                Id dimensionProperty = dimensionProperties[i];
                if (i > 0) {
                    pw.print(", ");
                }
                dimensionProperty.unparse(pw);
            }
        }
        if (!axisOrdinal.isFilter()) {
            pw.print(" ON " + axisOrdinal.name());
        }
    }

    @Override
    public void addLevel(Level level) {
        Util.assertTrue(level != null, "addLevel needs level");
        exp = new UnresolvedFunCallImpl(new FunctionOperationAtom("Crossjoin")
            ,  new Expression[] {
                exp,
                new UnresolvedFunCallImpl( new  PlainPropertyOperationAtom("Members"),
                    new Expression[] {
                        new LevelExpressionImpl(level)})});
    }

    public SubtotalVisibility getSubtotalVisibility() {
        return subtotalVisibility;
    }

    @Override
    public void validate(Validator validator) {
        if (axisOrdinal.isFilter() && exp != null) {
            exp = validator.validate(exp, false);
        }
    }

    public Id[] getDimensionProperties() {
        return dimensionProperties;
    }


}
