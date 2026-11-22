/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * 2002-2017 Hitachi Vantara.
 * 2006      jhyde
 * 
 * Contributors after Fork in 2023:
 *   Sergei Semenkov (2001)
 *   SmartCity Jena - initial
 */


package org.eclipse.daanse.olap.calc.base.compiler;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.DimensionCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.compiler.ParameterSlot;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.WrapExpression;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.common.AbstractQueryPart;

/**
 * Delegating implementation of {@link ExpressionCompiler} that enables the Decorator pattern.
 *
 * <p>This class wraps a parent compiler and delegates all compilation methods to it,
 * while providing hooks for subclasses to intercept and modify the compilation process.
 * The primary extension point is the {@link #afterCompile} method, which is called
 * after each compilation to allow post-processing of the resulting {@link Calc}.</p>
 *
 * <p>Subclasses can override {@code afterCompile} to add behaviors such as:</p>
 * <ul>
 *   <li>Profiling and timing of calculations</li>
 *   <li>Caching of intermediate results</li>
 *   <li>Logging and debugging</li>
 *   <li>Result transformation or optimization</li>
 * </ul>
 *
 * @see BaseExpressionCompiler
 */
public class DelegatingExpressionCompiler implements ExpressionCompiler {
    private final ExpressionCompiler parent;

    /**
     * Creates a delegating compiler that wraps the specified parent compiler.
     *
     * @param parent the parent compiler to delegate to; must not be null
     */
    protected DelegatingExpressionCompiler(ExpressionCompiler parent) {
        this.parent = parent;
    }

    /**
     * Hook for post-processing.
     *
     * @param exp     Expression to compile
     * @param calc    Calculator created by compiler
     * @param mutable Whether the result is mutuable
     * @return Calculator after post-processing
     */
    protected Calc<?> afterCompile(Expression exp, Calc<?> calc, boolean mutable) {
        return calc;
    }

    @Override
    public Evaluator getEvaluator() {
        return parent.getEvaluator();
    }

    @Override
    public Validator getValidator() {
        return parent.getValidator();
    }

    @Override
    public Calc<?> compile(Expression exp) {
        final Calc<?> calc = parent.compile(wrap(exp));
        return afterCompile(exp, calc, false);
    }

    @Override
    public Calc<?> compileAs(Expression exp, Type resultType, List<ResultStyle> preferredResultTypes) {
        // through afterCompile like every other compile: Filter compiles its set this way, and without it the
        // profiling hull and the dependency check never saw that calc
        final Calc<?> calc = parent.compileAs(wrap(exp), resultType, preferredResultTypes);
        return afterCompile(exp, calc, false);
    }

    @Override
    public MemberCalc compileMember(Expression exp) {
        final MemberCalc calc = parent.compileMember(wrap(exp));
        return (MemberCalc) afterCompile(exp, calc, false);
    }

    @Override
    public LevelCalc compileLevel(Expression exp) {
        final LevelCalc calc = parent.compileLevel(wrap(exp));
        return (LevelCalc) afterCompile(exp, calc, false);
    }

    @Override
    public DimensionCalc compileDimension(Expression exp) {
        final DimensionCalc calc = parent.compileDimension(wrap(exp));
        return (DimensionCalc) afterCompile(exp, calc, false);
    }

    @Override
    public HierarchyCalc compileHierarchy(Expression exp) {
        final HierarchyCalc calc = parent.compileHierarchy(wrap(exp));
        return (HierarchyCalc) afterCompile(exp, calc, false);
    }

    @Override
    public IntegerCalc compileInteger(Expression exp) {
        final IntegerCalc calc = parent.compileInteger(wrap(exp));
        return (IntegerCalc) afterCompile(exp, calc, false);
    }

    @Override
    public StringCalc compileString(Expression exp) {
        final StringCalc calc = parent.compileString(wrap(exp));
        return (StringCalc) afterCompile(exp, calc, false);
    }

    @Override
    public DateTimeCalc compileDateTime(Expression exp) {
        final DateTimeCalc calc = parent.compileDateTime(wrap(exp));
        return (DateTimeCalc) afterCompile(exp, calc, false);
    }

    @Override
    public final TupleListCalc compileList(Expression exp) {
        return compileList(exp, false);
    }

    @Override
    public TupleListCalc compileList(Expression exp, boolean mutable) {
        final TupleListCalc calc = parent.compileList(wrap(exp), mutable);
        return (TupleListCalc) afterCompile(exp, calc, mutable);
    }

    @Override
    public TupleIteratorCalc<?> compileIter(Expression exp) {
        final TupleIteratorCalc<?> calc = parent.compileIter(wrap(exp));
        return (TupleIteratorCalc<?>) afterCompile(exp, calc, false);
    }

    @Override
    public BooleanCalc compileBoolean(Expression exp) {
        final BooleanCalc calc = parent.compileBoolean(wrap(exp));
        return (BooleanCalc) afterCompile(exp, calc, false);
    }

    @Override
    public DoubleCalc compileDouble(Expression exp) {
        final DoubleCalc calc = parent.compileDouble(wrap(exp));
        return (DoubleCalc) afterCompile(exp, calc, false);
    }

    @Override
    public TupleCalc compileTuple(Expression exp) {
        final TupleCalc calc = parent.compileTuple(wrap(exp));
        return (TupleCalc) afterCompile(exp, calc, false);
    }

    @Override
    public Calc<?> compileScalar(Expression exp, boolean scalar) {
        final Calc<?> calc = parent.compileScalar(wrap(exp), scalar);
        return afterCompile(exp, calc, false);
    }

    @Override
    public ParameterSlot registerParameter(Parameter parameter) {
        return parent.registerParameter(parameter);
    }

    @Override
    public List<ResultStyle> getAcceptableResultStyles() {
        return parent.getAcceptableResultStyles();
    }

    /**
     * Wrapping an expression ensures that when it is visited, it calls back to this
     * compiler rather than our parent (wrapped) compiler.
     *
     * All methods that pass an expression to the delegate compiler should wrap
     * expressions in this way. Hopefully the delegate compiler doesn't use
     * {@code instanceof}; it should be using the visitor pattern instead.
     *
     * If we didn't do this, the decorator would get forgotten at the first level of
     * recursion. It's not pretty, and I thought about other ways of combining
     * Visitor + Decorator. For instance, I tried replacing
     * {@link #afterCompile(org.eclipse.daanse.olap.api.query.component.Expression, mondrian.calc.Calc, boolean)}
     * with a callback (Strategy), but the exit points in ExpCompiler not clear
     * because there are so many methods.
     *
     * @param e Expression to be wrapped
     * @return wrapper expression
     */
    private Expression wrap(Expression e) {
        return new WrapExpressionImpl(e, this);
    }

    /**
     * See
     * {@link org.eclipse.daanse.olap.calc.base.compiler.DelegatingExpressionCompiler#wrap}.
     */
    private static class WrapExpressionImpl extends AbstractQueryPart implements Expression, WrapExpression {
        private final Expression e;
        private final ExpressionCompiler wrappingCompiler;

        WrapExpressionImpl(Expression e, ExpressionCompiler wrappingCompiler) {
            this.e = e;
            this.wrappingCompiler = wrappingCompiler;
        }

        @Override
        public Expression cloneExp() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataType getCategory() {
            return e.getCategory();
        }

        @Override
        public Type getType() {
            return e.getType();
        }

        @Override
        public void unparse(PrintWriter pw) {
            e.unparse(pw);
        }

        @Override
        public Expression accept(Validator validator) {
            return e.accept(validator);
        }

        @Override
        public Calc<?> accept(ExpressionCompiler compiler) {
            return e.accept(wrappingCompiler);
        }

        @Override
        public Object accept(QueryComponentVisitor visitor) {
            return e.accept(visitor);
        }

        @Override
        public void explain(PrintWriter pw) {
            if (e instanceof QueryComponent queryPart) {
                queryPart.explain(pw);
            } else {
                super.explain(pw);
            }
        }
    }
}
