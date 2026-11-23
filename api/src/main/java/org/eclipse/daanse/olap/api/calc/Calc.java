/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 *
 * For more information please visit the Project: Hitachi Vantara - Mondrian
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

package org.eclipse.daanse.olap.api.calc;

import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;

/**
 * Calc is the base interface for all calculable expressions.
 *
 * <h2>Logical and physical expression languages</h2>
 *
 * <p>
 * Mondrian has two expression languages:
 * </p>
 * <ul>
 * <li>The logical language of parsed MDX fragments
 * ({@link org.eclipse.daanse.olap.api.query.component.Expression}).</li>
 * <li>The physical language of compiled expressions ({@link Calc}).</li>
 * </ul>
 *
 * <p>
 * The two languages allow us to separate logical (how an MDX expression was
 * specified) from physical (how it is to be evaluated). The physical language
 * is more strongly typed, and certain constructs which are implicit in the
 * logical language (such as the addition of calls to the
 * {@code <Member>.CurrentMember} function) are made explicit in the physical
 * language.
 * </p>
 *
 * <h2>Compilation</h2>
 *
 * <p>
 * Expressions are generally created using an expression compiler
 * ({@link ExpressionCompiler}). There are often more than one evaluation
 * strategy for a given expression, and the compilation process gives us an
 * opportunity to choose the optimal one.
 * </p>
 *
 * <h2>Type Hierarchy</h2>
 *
 * <p>
 * The Calc interface has the following sub-interfaces:
 * </p>
 * <ul>
 * <li>{@link BooleanCalc} - boolean values</li>
 * <li>{@link ByteCalc}, {@link IntegerCalc}, {@link LongCalc},
 * {@link FloatCalc}, {@link DoubleCalc} - numeric types</li>
 * <li>{@link StringCalc}, {@link DateTimeCalc} - string and date types</li>
 * <li>{@link MemberCalc}, {@link TupleCalc} - member and tuple types</li>
 * <li>{@link DimensionCalc}, {@link HierarchyCalc}, {@link LevelCalc} - schema
 * element types</li>
 * <li>{@link TupleIterableCalc} - tuple collections</li>
 * <li>{@link ConstantCalc} - constant values</li>
 * <li>{@link VoidCalc} - void expressions (side effects only)</li>
 * </ul>
 *
 * <p>
 * Note: This interface cannot be sealed because some sub-interfaces
 * (TupleIteratorCalc, TupleListCalc) are in the 'todo' subpackage, and
 * cross-package sealing requires named modules.
 * </p>
 *
 * @author jhyde
 * @since Sep 26, 2005
 *
 * @param <E> the result type of the calculation
 */
public interface Calc<E> {
    /**
     * Evaluates this expression.
     *
     * @param evaluator Provides dimensional context in which to evaluate this
     *                  expression
     * @return Result of expression evaluation
     */
    E evaluate(Evaluator evaluator);

    /**
     * Returns whether this expression depends upon a given hierarchy.
     *
     * If it does not depend on the hierarchy, then re-evaluating the expression
     * with a different member of this context must produce the same answer.
     *
     * Some examples:
     *
     * The expression [Measures].[Unit Sales] depends on all dimensions except
     * [Measures].
     *
     * The boolean expression ([Measures].[Unit Sales], [Time].[1997]) &gt; 1000
     * depends on all hierarchies except [Measures] and [Time].
     *
     * The list expression Filter([Store].[USA].Children, [Measures].[Unit Sales]
     * &lt; 50) depends upon all hierarchies except [Store] and [Measures]. How so?
     * Normally the scalar expression would depend upon all hierarchies except
     * [Measures], but the Filter function sets the [Store] context before
     * evaluating the scalar expression, so it is not inherited from the surrounding
     * context.
     *
     *
     *
     * @param hierarchy Hierarchy
     * @return Whether this expression's result depends upon the current member of
     *         the hierarchy
     */
    boolean dependsOn(Hierarchy hierarchy);

    /**
     * Returns the type.
     */
    Type getType();

    /**
     * Returns style in which the result of evaluating this expression is returned.
     *
     * One application of this method is for the compiler to figure out whether the
     * compiled expression is returning a mutable list. If a mutable list is
     * required, the compiler can create a mutable copy.
     *
     * @see ExpressionCompiler#compileMutableList(org.eclipse.daanse.olap.api.query.component.Expression)
     */
    ResultStyle getResultStyle();

    CalculationProfile getCalculationProfile();
}
