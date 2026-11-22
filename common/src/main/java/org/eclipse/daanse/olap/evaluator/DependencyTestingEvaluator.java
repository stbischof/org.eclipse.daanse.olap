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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.daanse.olap.evaluator.EvaluatorRoot;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleCursor;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.calc.base.compiler.DelegatingExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleIterableCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.result.ResultEvaluatorRoot;
import org.eclipse.daanse.olap.result.ResultImpl;

/**
 * Evaluator which checks dependencies of expressions.
 *
 * For each expression evaluation, this valuator evaluates each
 * expression more times, and makes sure that the results of the expression
 * are independent of dimensions which the expression claims to be
 * independent of.
 *
 * Since it evaluates each expression twice, it also exposes function
 * implementations which change the context of the evaluator.
 *
 * @author jhyde
 * @since September, 2005
 */
public class DependencyTestingEvaluator extends EvaluatorImpl {

    /**
     * Creates an dependency-testing evaluator.
     *
     * @param result Result we are building
     * @param expDeps Number of dependencies to check
     */
    public DependencyTestingEvaluator(ResultImpl result, int expDeps) {
        super(new DteRoot(result, expDeps));
    }

    /**
     * Creates a child evaluator.
     *
     * @param root Root evaluation context
     * @param evaluator Parent evaluator
     */
    private DependencyTestingEvaluator(
        EvaluatorRoot root,
        DependencyTestingEvaluator evaluator,
        List<List<Member>> aggregationList)
    {
        super(root, evaluator, aggregationList);
    }

    public Object evaluate(
        Calc calc,
        Hierarchy[] independentHierarchies,
        String mdxString)
    {
        final DteRoot dteRoot =
                (DteRoot) root;
        if (dteRoot.faking) {
            ++dteRoot.fakeCallCount;
        } else {
            ++dteRoot.callCount;
        }
        // Evaluate the call for real.
        final Object result = calc.evaluate(this);
        if (dteRoot.result.isDirty()) {
            return result;
        }

        // If the result is a list and says that it is mutable, see whether it
        // really is.
        if (calc.getResultStyle() == ResultStyle.MUTABLE_LIST) {
            List<Object> list = (List) result;
            if (!list.isEmpty()) {
                final Object zeroth = list.getFirst();
                list.set(0, zeroth);
            }
        }

        // Change one of the allegedly independent dimensions and evaluate
        // again.
        //
        // Don't do it if the faking is disabled,
        // or if we're already faking another dimension,
        // or if we're filtering out nonempty cells (which makes us
        // dependent on everything),
        // or if the ratio of fake evals to real evals is too high (which
        // would make us too slow).
        if (dteRoot.disabled
            || dteRoot.faking
            || isNonEmpty()
            || (double) dteRoot.fakeCallCount
            > (double) dteRoot.callCount * dteRoot.random.nextDouble()
            * 2 * dteRoot.expDeps)
        {
            return result;
        }
        if (independentHierarchies.length == 0) {
            return result;
        }
        dteRoot.faking = true;
        ++dteRoot.fakeCount;
        ++dteRoot.fakeCallCount;
        final int i = dteRoot.random.nextInt(independentHierarchies.length);
        final Member saveMember = getContext(independentHierarchies[i]);
        final Member otherMember =
            dteRoot.chooseOtherMember(
                saveMember, getQuery().getCatalogReader(false));
        setContext(otherMember);
        final Object otherResult = calc.evaluate(this);
        if (!equals(otherResult, result)) {
            final Member[] members = getMembers();
            final StringBuilder buf = new StringBuilder();
            for (int j = 0; j < members.length; j++) {
                if (j > 0) {
                    buf.append(", ");
                }
                buf.append(members[j].getUniqueName());
            }
            throw Util.newInternal(
                new StringBuilder("Expression '").append(mdxString)
                .append("' claims to be independent of dimension ")
                .append(saveMember.getDimension()).append(" but is not; context is {")
                .append(buf.toString()).append("}; First result: ")
                .append(toString(result)).append(", Second result: ")
                .append(toString(otherResult)).toString());
        }
        // Restore context.
        setContext(saveMember);
        dteRoot.faking = false;
        return result;
    }

    @Override
	public EvaluatorImpl pushClone(List<List<Member>> aggregationList) {
        return new DependencyTestingEvaluator(getRoot(), this, aggregationList);
    }

    private boolean equals(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        if (o2 == null) {
            return false;
        }
        if (o1 instanceof Object[] a1) {
            if (o2 instanceof Object[] a2) {
                if (a1.length == a2.length) {
                    for (int i = 0; i < a1.length; i++) {
                        if (!equals(a1[i], a2[i])) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }
        if (o1 instanceof List) {
            return o2 instanceof List
                && equals(
                    ((List) o1).toArray(),
                    ((List) o2).toArray());
        }
        if (o1 instanceof Iterable) {
            if (o2 instanceof Iterable) {
                return equals(toList((Iterable) o1), toList((Iterable) o2));
            } else {
                return false;
            }
        }
        return o1.equals(o2);
    }

    private String toString(Object o) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        toString(o, pw);
        return sw.toString();
    }

    private <T> List<T> toList(Iterable<T> iterable) {
        final ArrayList<T> list = new ArrayList<>();
        for (T t : iterable) {
            list.add(t);
        }
        return list;
    }

    private void toString(Object o, PrintWriter pw) {
        switch (o) {
        case Object[] a -> {
            pw.print("{");
            for (int i = 0; i < a.length; i++) {
                Object o1 = a[i];
                if (i > 0) {
                    pw.print(", ");
                }
                toString(o1, pw);
            }
            pw.print("}");
        }
        case List list -> toString(list.toArray(), pw);
        case Member member -> pw.print(member.getUniqueName());
        case null, default -> pw.print(o);
        }
    }

    /**
     * Holds context for a tree of {@link DependencyTestingEvaluator}.
     */
    public static class DteRoot extends ResultEvaluatorRoot {
        final int expDeps;
        int callCount;
        int fakeCallCount;
        int fakeCount;
        boolean faking;
        public boolean disabled;
        final Random random = new SecureRandom();


        DteRoot(ResultImpl result, int expDeps) {
            super(result);
            this.expDeps = expDeps;
        }

        /**
         * Chooses another member of the same hierarchy.
         * The member will come from all levels with the same probability.
         * Calculated members are not included.
         *
         * @param save Previous member
         * @param schemaReader Schema reader
         * @return other member of same hierarchy
         */
        private Member chooseOtherMember(
            final Member save,
            CatalogReader schemaReader)
        {
            final Hierarchy hierarchy = save.getHierarchy();
            int attempt = 0;
            while (true) {
                // Choose a random level.
                final List<? extends Level> levels = hierarchy.getLevels();
                final int levelDepth = random.nextInt(levels.size()) + 1;
                Member member = null;
                for (int i = 0; i < levelDepth; i++) {
                    List<Member> members;
                    if (i == 0) {
                        members =
                            schemaReader.getLevelMembers(levels.get(i), false);
                    } else {
                        members = schemaReader.getMemberChildren(member);
                    }
                    if (members.isEmpty()) {
                        break;
                    }
                    member = members.get(random.nextInt(members.size()));
                }
                // If the member chosen happens to be the same as the original
                // member, try again. Give up after 100 attempts (in case the
                // hierarchy has only one member).
                if (member != save || ++attempt > 100) {
                    return member;
                }
            }
        }
    }

    /**
     * Expression which checks dependencies of an underlying scalar expression.
     */
    private static class DteScalarCalcImpl extends AbstractNestedUnknownCalc {
        private final Calc calc;
        private final Hierarchy[] independentHierarchies;
        private final String mdxString;

        DteScalarCalcImpl(
            Calc calc,
            Hierarchy[] independentHierarchies,
            String mdxString)
        {
            super(calc.getType());
            this.calc = calc;
            this.independentHierarchies = independentHierarchies;
            this.mdxString = mdxString;
        }

        @Override
		public Calc[] getChildCalcs() {
            return new Calc[] {calc};
        }

        @Override
		public Object evaluateInternal(Evaluator evaluator) {
            DependencyTestingEvaluator dtEval =
                (DependencyTestingEvaluator) evaluator;
            return dtEval.evaluate(calc, independentHierarchies, mdxString);
        }

        @Override
		public ResultStyle getResultStyle() {
            return calc.getResultStyle();
        }
    }

    /**
     * Expression which checks dependencies and list immutability of an
     * underlying list or iterable expression.
     */
    private static class DteIterCalcImpl extends AbstractNestedTupleIterableCalc {
        private final Calc calc;
        private final Hierarchy[] independentHierarchies;
        private final boolean mutableList;
        private final String mdxString;

        DteIterCalcImpl(
            Calc calc,
            Hierarchy[] independentHierarchies,
            boolean mutableList,
            String mdxString)
        {
            super(calc.getType());
            this.calc = calc;
            this.independentHierarchies = independentHierarchies;
            this.mutableList = mutableList;
            this.mdxString = mdxString;
        }

        @Override
		public Calc[] getChildCalcs() {
            return new Calc[] {calc};
        }

        @Override
		public TupleIterable evaluateInternal(Evaluator evaluator) {
            DependencyTestingEvaluator dtEval =
                    (DependencyTestingEvaluator) evaluator;
            return (TupleIterable) dtEval.evaluate(calc, independentHierarchies, mdxString);
        }

        @Override
		public ResultStyle getResultStyle() {
            return calc.getResultStyle();
        }
    }


    /**
     * Expression which checks dependencies and list immutability of an
     * underlying list or iterable expression.
     */
    private static class DteListCalcImpl extends AbstractNestedTupleListCalc {
        private final Calc calc;
        private final Hierarchy[] independentHierarchies;
        private final boolean mutableList;
        private final String mdxString;

        DteListCalcImpl(
            Calc calc,
            Hierarchy[] independentHierarchies,
            boolean mutableList,
            String mdxString)
        {
            super(calc.getType());
            this.calc = calc;
            this.independentHierarchies = independentHierarchies;
            this.mutableList = mutableList;
            this.mdxString = mdxString;
        }

        @Override
        public Calc[] getChildCalcs() {
            return new Calc[] {calc};
        }



        @Override
        public TupleList evaluateInternal(Evaluator evaluator) {
            DependencyTestingEvaluator dtEval = (DependencyTestingEvaluator) evaluator;
            Object o = dtEval.evaluate(calc, independentHierarchies, mdxString);

            if (o instanceof TupleList tupleList) {
                return tupleList;
            }
            // from GenericIterCalc may cause issues
            final TupleIterable iterable = (TupleIterable) o;
            final TupleList tupleList = TupleCollections.createList(iterable.getArity());
            final TupleCursor cursor = iterable.tupleCursor();
            while (cursor.forward()) {
                tupleList.addCurrent(cursor);
            }
            TupleList list = evaluate(evaluator);
            if (!mutableList) {
                list = TupleCollections.unmodifiableList(list);
            }
            return list;
        }

        @Override
        public ResultStyle getResultStyle() {
            return calc.getResultStyle();
        }
    }
    /**
     * Expression compiler which introduces dependency testing.
     *
     * It also checks that the caller does not modify lists unless it has
     * explicitly asked for a mutable list.
     */
    public static class DteCompiler extends DelegatingExpressionCompiler {
        public DteCompiler(ExpressionCompiler compiler) {
            super(compiler);
        }

        @Override
		protected Calc afterCompile(Expression exp, Calc calc, boolean mutable) {
            Hierarchy[] dimensions = getIndependentHierarchies(calc);
            calc = super.afterCompile(exp, calc, mutable);
            if (calc.getType() instanceof SetType) {
                return new DteIterCalcImpl(
                    calc,
                    dimensions,
                    mutable,
                    Util.unparse(exp));
            } else {
                return new DteScalarCalcImpl(
                    calc,
                    dimensions,
                    Util.unparse(exp));
            }
        }

        /**
         * Returns the dimensions an expression does not depend on. If the
         * current member of any of these dimensions changes, the expression
         * will return the same result.
         *
         * @param calc Expression
         * @return List of dimensions that the expression does not depend on
         */
        private Hierarchy[] getIndependentHierarchies(Calc calc) {
            List<Hierarchy> list = new ArrayList<>();
            final List<Hierarchy> hierarchies =
                getValidator().getQuery().getCube().getHierarchies();
            for (Hierarchy hierarchy : hierarchies) {
                if (!calc.dependsOn(hierarchy)) {
                    list.add(hierarchy);
                }
            }
            return list.toArray(Hierarchy[]::new);
        }
    }
}
