/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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

package org.eclipse.daanse.olap.api.calc.compiler;

import java.util.List;

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
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.Type;

/**
 * Mediates the compilation of an expression (
 * org.eclipse.daanse.olap.api.query.component.Expression) into a compiled
 * expression ( Calc).
 * <p>
 * {@link #compile(Expression, CompileRequest)} is the one way in: every typed entry ({@code compileDouble},
 * {@code compileList}, ...) is that method with a request of its kind; a result type with preferred styles is
 * {@code compile(exp, CompileRequest.as(type, styles))}. A decorator overrides that one
 * method and sees every compiled calc once; before, it rebuilt every entry and missed the one it forgot
 * ({@code compileAs}).
 *
 * @author jhyde
 * @since Sep 28, 2005
 */
public interface ExpressionCompiler {

    /**
     * Returns the evaluator to be used for evaluating expressions during the
     * compilation process.
     */
    Evaluator getEvaluator();

    /**
     * Returns the validator which was used to validate this expression.
     *
     * @return validator
     */
    Validator getValidator();

    /**
     * Compiles an expression as the request asks: the calc of the expression ({@link CompileRequest#ANY}), under a
     * result type and preferred styles ({@link CompileRequest#as}), or converted to the asked kind.
     *
     * @param exp     Expression
     * @param request what the caller asks for
     * @return Compiled expression, or null if none can satisfy a request of kind ANY
     */
    Calc<?> compile(Expression exp, CompileRequest request);

    /**
     * Compiles an expression.
     *
     * @param exp Expression
     * @return Compiled expression
     */
    default Calc<?> compile(Expression exp) {
        return compile(exp, CompileRequest.ANY);
    }

    /**
     * Compiles an expression which yields a Member result.
     */
    default MemberCalc compileMember(Expression exp) {
        return (MemberCalc) compile(exp, CompileRequest.MEMBER);
    }

    /**
     * Compiles an expression which yields a Level result.
     */
    default LevelCalc compileLevel(Expression exp) {
        return (LevelCalc) compile(exp, CompileRequest.LEVEL);
    }

    /**
     * Compiles an expression which yields a Dimension result.
     */
    default DimensionCalc compileDimension(Expression exp) {
        return (DimensionCalc) compile(exp, CompileRequest.DIMENSION);
    }

    /**
     * Compiles an expression which yields a Hierarchy result.
     */
    default HierarchyCalc compileHierarchy(Expression exp) {
        return (HierarchyCalc) compile(exp, CompileRequest.HIERARCHY);
    }

    /**
     * Compiles an expression which yields an int result. The expression is
     * implicitly converted into a scalar.
     */
    default IntegerCalc compileInteger(Expression exp) {
        return (IntegerCalc) compile(exp, CompileRequest.INTEGER);
    }

    /**
     * Compiles an expression which yields a String result. The expression is
     * implicitly converted into a scalar.
     */
    default StringCalc compileString(Expression exp) {
        return (StringCalc) compile(exp, CompileRequest.STRING);
    }

    /**
     * Compiles an expression which yields a java.util.Date result. The expression
     * is implicitly converted into a scalar.
     */
    default DateTimeCalc compileDateTime(Expression exp) {
        return (DateTimeCalc) compile(exp, CompileRequest.DATE_TIME);
    }

    /**
     * Compiles an expression which yields an immutable TupleList result: a list of members or of tuples. The caller
     * must not change it; {@link #compileMutableList} gives one it may change.
     */
    default TupleListCalc compileList(Expression exp) {
        return (TupleListCalc) compile(exp, CompileRequest.LIST);
    }

    /** Compiles an expression which yields a TupleList the caller may change (a copy of an immutable one). */
    default TupleListCalc compileMutableList(Expression exp) {
        return (TupleListCalc) compile(exp, CompileRequest.MUTABLE_LIST);
    }

    /**
     * Compiles an expression which yields an immutable Iterable result.
     *
     * @param exp Expression
     * @return Calculator which yields an Iterable
     */
    default TupleIteratorCalc<?> compileIterable(Expression exp) {
        return (TupleIteratorCalc<?>) compile(exp, CompileRequest.ITERABLE);
    }

    /**
     * Compiles an expression which yields a boolean result.
     *
     * @param exp Expression
     * @return Calculator which yields a boolean
     */
    default BooleanCalc compileBoolean(Expression exp) {
        return (BooleanCalc) compile(exp, CompileRequest.BOOLEAN);
    }

    /**
     * Compiles an expression which yields a double result.
     *
     * @param exp Expression
     * @return Calculator which yields a double
     */
    default DoubleCalc compileDouble(Expression exp) {
        return (DoubleCalc) compile(exp, CompileRequest.DOUBLE);
    }

    /**
     * Compiles an expression which yields a tuple result.
     *
     * @param exp Expression
     * @return Calculator which yields a tuple
     */
    default TupleCalc compileTuple(Expression exp) {
        return (TupleCalc) compile(exp, CompileRequest.TUPLE);
    }

    /**
     * Compiles an expression to yield a scalar result. If the expression yields a member or tuple, the calculator
     * applies that member or tuple to the current dimensional context and returns the value of the current measure.
     *
     * @param exp Expression
     * @return Calculation which returns the scalar value of the expression
     */
    default Calc<?> compileScalar(Expression exp) {
        return compile(exp, CompileRequest.SCALAR);
    }

    /**
     * Compiles an expression to yield a scalar of its exact type: a string expression as {@link #compileString}, a
     * numeric one as {@link #compileDouble}, a boolean one as {@link #compileBoolean}.
     *
     * @param exp Expression
     * @return Calculation which returns the scalar value of the expression
     */
    default Calc<?> compileExactScalar(Expression exp) {
        return compile(exp, CompileRequest.EXACT_SCALAR);
    }

    /**
     * Implements a parameter, returning a unique slot which will hold the
     * parameter's value.
     *
     * @param parameter Parameter
     * @return Slot
     */
    ParameterSlot registerParameter(Parameter parameter);

    /**
     * Returns a list of the ResultStyles acceptable to the caller.
     */
    List<ResultStyle> getAcceptableResultStyles();

}
