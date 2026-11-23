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
 */
package org.eclipse.daanse.olap.evaluator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.CompileHook;
import org.eclipse.daanse.olap.api.calc.compiler.CompileRequest;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleIterableCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.common.Util;

/**
 * The compile hook which introduces the {@link DependencyCheck}: every compiled expression is wrapped in a
 * calc that asks the check of the evaluator before answering. It also checks that the caller does not modify
 * lists unless it has explicitly asked for a mutable list.
 *
 * <p>
 * The query hooks it into its compiler when {@code testExpDependencies} is on, beside profiling and outside it (the
 * profile measures the calc, not the check; before, profiling trumped and a profiled statement was never checked);
 * the check itself hangs on the root of whichever evaluator the art runs. A wrapped calc under an evaluator without
 * a check -- the hook installed by hand -- answers like the calc it wraps.
 */
public class DependencyCheckHook implements CompileHook {

    private final Validator validator;

    /** A hook for the expressions validated by {@code validator}, whose query names the cube. */
    public DependencyCheckHook(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Calc<?> afterCompile(Expression exp, Calc<?> calc, CompileRequest request) {
        final boolean mutable = request.mutable();
        Hierarchy[] dimensions = getIndependentHierarchies(calc);
        Calc<?> checking;
        if (calc.getType() instanceof SetType) {
            // a list stays a list, as in the original: compileList takes the result as a TupleListCalc
            checking = calc instanceof TupleListCalc
                    ? new CheckedListCalc(calc, dimensions, mutable, Util.unparse(exp))
                    : new CheckedIterCalc(calc, dimensions, Util.unparse(exp));
        } else {
            checking = new CheckedScalarCalc(calc, dimensions, Util.unparse(exp));
        }
        return withTheKindOf(calc, checking);
    }

    /** The real evaluation, or the checked one where the evaluator runs under a check. */
    private static Object checked(Evaluator evaluator, Calc<?> calc, Hierarchy[] independentHierarchies,
            String mdxString) {
        DependencyCheck check = DependencyCheck.of(evaluator);
        return check == null ? calc.evaluate(evaluator)
                : check.evaluate((EvaluatorImpl) evaluator, calc, independentHierarchies, mdxString);
    }

    /**
     * The checking calc, wearing every calc interface the checked one wears. The typed compile methods
     * cast what this compiler hands back -- to a {@code DoubleCalc}, a {@code TupleCalc} and so on -- so a
     * wrapper of the plain kind failed on the first cast and the check could not run at all, under any art.
     * Mondrian's wrapper was a {@code GenericCalc}, which wore every scalar kind; the typed
     * calcs here are markers without methods of their own, so a proxy that hands every call to the
     * checking calc wears them faithfully.
     */
    private static Calc<?> withTheKindOf(Calc<?> checked, Calc<?> checking) {
        Set<Class<?>> kinds = new LinkedHashSet<>();
        for (Class<?> c = checked.getClass(); c != null; c = c.getSuperclass()) {
            collectCalcInterfaces(c, kinds);
        }
        boolean missing = kinds.stream().anyMatch(kind -> !kind.isInstance(checking));
        if (!missing) {
            return checking;
        }
        for (Class<?> c = checking.getClass(); c != null; c = c.getSuperclass()) {
            collectCalcInterfaces(c, kinds);
        }
        return (Calc<?>) java.lang.reflect.Proxy.newProxyInstance(checked.getClass().getClassLoader(),
                kinds.toArray(Class<?>[]::new), (proxy, method, args) -> {
                    try {
                        return method.invoke(checking, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static void collectCalcInterfaces(Class<?> type, Set<Class<?>> kinds) {
        for (Class<?> i : type.getInterfaces()) {
            if (Calc.class.isAssignableFrom(i)) {
                kinds.add(i);
            }
            collectCalcInterfaces(i, kinds);
        }
    }

    /**
     * Returns the hierarchies an expression does not depend on. If the current member of any of these
     * changes, the expression will return the same result.
     */
    private Hierarchy[] getIndependentHierarchies(Calc<?> calc) {
        List<Hierarchy> list = new ArrayList<>();
        final List<? extends Hierarchy> hierarchies = validator.getQuery().getCube().getHierarchies();
        for (Hierarchy hierarchy : hierarchies) {
            if (!calc.dependsOn(hierarchy)) {
                list.add(hierarchy);
            }
        }
        return list.toArray(Hierarchy[]::new);
    }

    /** Checks the dependencies of an underlying scalar expression. */
    private static class CheckedScalarCalc extends AbstractNestedUnknownCalc {
        private final Calc<?> calc;
        private final Hierarchy[] independentHierarchies;
        private final String mdxString;

        CheckedScalarCalc(Calc<?> calc, Hierarchy[] independentHierarchies, String mdxString) {
            super(calc.getType());
            this.calc = calc;
            this.independentHierarchies = independentHierarchies;
            this.mdxString = mdxString;
        }

        @Override
        public Calc<?>[] getChildCalcs() {
            return new Calc[] { calc };
        }

        @Override
        public Object evaluateInternal(Evaluator evaluator) {
            return checked(evaluator, calc, independentHierarchies, mdxString);
        }

        @Override
        public ResultStyle getResultStyle() {
            return calc.getResultStyle();
        }
    }

    /** Checks the dependencies of an underlying iterable expression. */
    private static class CheckedIterCalc extends AbstractNestedTupleIterableCalc {
        private final Calc<?> calc;
        private final Hierarchy[] independentHierarchies;
        private final String mdxString;

        CheckedIterCalc(Calc<?> calc, Hierarchy[] independentHierarchies, String mdxString) {
            super(calc.getType());
            this.calc = calc;
            this.independentHierarchies = independentHierarchies;
            this.mdxString = mdxString;
        }

        @Override
        public Calc<?>[] getChildCalcs() {
            return new Calc[] { calc };
        }

        @Override
        public TupleIterable evaluateInternal(Evaluator evaluator) {
            return (TupleIterable) checked(evaluator, calc, independentHierarchies, mdxString);
        }

        @Override
        public ResultStyle getResultStyle() {
            return calc.getResultStyle();
        }
    }

    /**
     * Checks the dependencies and the list immutability of an underlying list expression: a caller that did
     * not ask for a mutable list gets one it cannot change.
     */
    private static class CheckedListCalc extends AbstractNestedTupleListCalc {
        private final Calc<?> calc;
        private final Hierarchy[] independentHierarchies;
        private final boolean mutableList;
        private final String mdxString;

        CheckedListCalc(Calc<?> calc, Hierarchy[] independentHierarchies, boolean mutableList, String mdxString) {
            super(calc.getType());
            this.calc = calc;
            this.independentHierarchies = independentHierarchies;
            this.mutableList = mutableList;
            this.mdxString = mdxString;
        }

        @Override
        public Calc<?>[] getChildCalcs() {
            return new Calc[] { calc };
        }

        @Override
        public TupleList evaluateInternal(Evaluator evaluator) {
            Object o = checked(evaluator, calc, independentHierarchies, mdxString);
            TupleList list;
            if (o instanceof TupleList tupleList) {
                list = tupleList;
            } else {
                // an iterable from a list calc: materialised here, not by evaluating this calc again (the
                // original called evaluate(evaluator) on itself at this point and would have recursed)
                final TupleIterable iterable = (TupleIterable) o;
                list = TupleCollections.createList(iterable.getArity());
                final var cursor = iterable.tupleCursor();
                while (cursor.forward()) {
                    list.addCurrent(cursor);
                }
            }
            return mutableList ? list : TupleCollections.unmodifiableList(list);
        }

        /**
         * The style of what this calc hands out: a list it made unmodifiable is never announced as
         * MUTABLE_LIST, whatever the wrapped calc announces -- an outer check would otherwise probe the
         * list for mutability and fail on the wrapper's own doing.
         */
        @Override
        public ResultStyle getResultStyle() {
            ResultStyle style = calc.getResultStyle();
            return !mutableList && style == ResultStyle.MUTABLE_LIST ? ResultStyle.LIST : style;
        }
    }
}
