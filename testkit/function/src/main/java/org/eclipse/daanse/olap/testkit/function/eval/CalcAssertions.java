/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.testkit.function.eval;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.ConstantCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.testkit.assertions.AssertionMessages;

/** Assertions about a compiled expression: type, dependencies and result shape. */
public final class CalcAssertions {

    private CalcAssertions() {
    }

    /** Compiles {@code mdx} as a calculated measure. */
    public static CalcAssert assertThatScalarExpr(Connection connection, String cubeName, String mdx) {
        return new CalcAssert(connection, cubeName, mdx, true, null);
    }

    /** Compiles {@code mdx} as a set on the columns axis. */
    public static CalcAssert assertThatSetExpr(Connection connection, String cubeName, String mdx) {
        return assertThatSetExpr(connection, cubeName, mdx, ResultStyle.MUTABLE_LIST);
    }

    public static CalcAssert assertThatSetExpr(Connection connection, String cubeName, String mdx,
            ResultStyle requested) {
        return new CalcAssert(connection, cubeName, mdx, false, requested);
    }

    /** Compiles {@code {mdx}} — the singleton set around a member expression. */
    public static CalcAssert assertThatMemberExpr(Connection connection, String cubeName, String mdx) {
        return assertThatSetExpr(connection, cubeName, "{" + mdx + "}");
    }

    public static final class CalcAssert {

        private static final String NL = System.lineSeparator();

        private final Connection connection;
        private final String cubeName;
        private final String expression;
        private final boolean scalar;
        private final ResultStyle requested;

        private Query query;
        private Calc<?> calc;

        private CalcAssert(Connection connection, String cubeName, String expression, boolean scalar,
                ResultStyle requested) {
            this.connection = connection;
            this.cubeName = cubeName;
            this.expression = expression;
            this.scalar = scalar;
            this.requested = requested;
        }

        // ============ Promise 7: dependencies ==============================

        public CalcAssert dependsOnExactly(String... hierarchyUniqueNames) {
            Set<String> expected = new LinkedHashSet<>(List.of(hierarchyUniqueNames));
            Set<String> actual = actualDependencies();
            if (!expected.equals(actual)) {
                throw AssertionMessages.mismatch("hierarchy dependencies", "MDX", mdx(),
                        render(expected), render(actual), dependencyNarrative(expected, actual));
            }
            return this;
        }

        public CalcAssert dependsOnNothing() {
            return dependsOnExactly();
        }

        /** Subset check: these hierarchies must be among the dependencies. */
        public CalcAssert dependsOn(String... hierarchyUniqueNames) {
            Set<String> actual = actualDependencies();
            Set<String> missing = new LinkedHashSet<>(List.of(hierarchyUniqueNames));
            missing.removeAll(actual);
            if (!missing.isEmpty()) {
                throw AssertionMessages.mismatch("hierarchy dependencies", "MDX", mdx(),
                        render(new LinkedHashSet<>(List.of(hierarchyUniqueNames))), render(actual),
                        "Missing: " + missing);
            }
            return this;
        }

        /**
         * These hierarchies must <em>not</em> be reported. This is the interesting direction:
         * a context-setting function binds the hierarchies of its set argument, and an
         * over-reported dependency silently disables expression caching.
         */
        public CalcAssert doesNotDependOn(String... hierarchyUniqueNames) {
            Set<String> actual = actualDependencies();
            Set<String> unexpected = new LinkedHashSet<>(List.of(hierarchyUniqueNames));
            unexpected.retainAll(actual);
            if (!unexpected.isEmpty()) {
                throw AssertionMessages.mismatch("hierarchy dependencies", "MDX", mdx(),
                        "without " + List.of(hierarchyUniqueNames), render(actual),
                        """
                        A function that iterates over a set binds that set's hierarchies: re-evaluating
                        it with a different current member of those hierarchies must give the same
                        answer. Reporting them anyway is safe but disables expression reuse.
                        Reporting too few is the dangerous direction: it produces wrong numbers with
                        no error at all.
                        """);
            }
            return this;
        }

        // ============ Promise 8: result shape ==============================

        /**
         * The expression really was compiled, and did not fold into a constant.
         *
         * <p>A formula the MDX parser cannot read is taken as a string literal rather than
         * refused, and then compiles to a constant calc. Every shape it reports is the
         * constant's, not the function's, so a shape case would pass while measuring
         * nothing. This is the same trap the value promise guards against by comparing the
         * cell with the text of the expression.
         */
        public CalcAssert isNotACompiledConstant() {
            Calc<?> compiled = calc();
            if (compiled instanceof ConstantCalc) {
                throw AssertionMessages.failed(
                        "the expression was never compiled" + NL + "MDX:" + NL + mdx() + NL + NL
                                + """
                                It folded into a constant. A calculated member whose formula the
                                parser cannot read is taken as a string literal rather than refused,
                                so what is measured here is that constant and not the function.
                                """,
                        "a compiled call", compiled.getClass().getSimpleName());
            }
            return this;
        }

        /** The style the compiled expression reports, for a caller that wants to look. */
        public ResultStyle resultStyle() {
            return calc().getResultStyle();
        }

        /**
         * The reported style is exactly the one the contract names.
         *
         * <p>This is the regression guard: it says what the function does today, so that a
         * change to it is noticed. It is not the safety rule. What actually has to hold is
         * {@link #satisfiesRequestedStyle()}, and the two are checked together.
         */
        public CalcAssert hasResultStyle(ResultStyle expected) {
            ResultStyle actual = calc().getResultStyle();
            if (actual != expected) {
                throw AssertionMessages.mismatch("result style", "MDX", mdx(),
                        expected.name(), actual.name(),
                        """
                        The compiler was asked for ResultStyle.%s, the contract says %s.getResultStyle()
                        answers %s, and it answers %s.
                        If %s still satisfies the request, this is a change in what the function
                        promises, and the contract has to be updated deliberately rather than
                        silently. If it does not, satisfiesRequestedStyle() fails as well and that
                        is the real defect.
                        """.formatted(requested, calc().getClass().getSimpleName(), expected, actual,
                                actual));
            }
            return this;
        }

        /**
         * The reported style is at least as strong as the one asked for.
         *
         * <p>Answering with a stronger style is allowed and common: a caller who asks for
         * something merely iterable can be handed a list it may even modify, and nothing
         * it does with that breaks. The dangerous direction is the other one. A caller who
         * asked for a mutable list and is handed an unmodifiable one has no way of knowing
         * before it writes, and finds out as an exception in the middle of evaluation.
         */
        public CalcAssert satisfiesRequestedStyle() {
            ResultStyle actual = calc().getResultStyle();
            if (!satisfies(actual, requested)) {
                throw AssertionMessages.mismatch("result style", "MDX", mdx(),
                        "at least " + requested.name(), actual.name(),
                        """
                        The compiler was asked for ResultStyle.%s and %s.getResultStyle() reports %s,
                        which is weaker. A caller that trusts the requested style and modifies the
                        result gets an UnsupportedOperationException in the middle of evaluation
                        instead of a refusal at compile time.
                        """.formatted(requested, calc().getClass().getSimpleName(), actual));
            }
            return this;
        }

        /** Whether {@code actual} covers everything a caller asking for {@code wanted} may do. */
        private static boolean satisfies(ResultStyle actual, ResultStyle wanted) {
            if (wanted == null || wanted == ResultStyle.ANY || actual == wanted) {
                return true;
            }
            return switch (wanted) {
                // A list may be iterated; a mutable list may be iterated and read.
                case ITERABLE -> actual == ResultStyle.LIST || actual == ResultStyle.MUTABLE_LIST;
                case LIST -> actual == ResultStyle.MUTABLE_LIST;
                default -> false;
            };
        }

        public CalcAssert hasResultStyleIn(ResultStyle... allowed) {
            ResultStyle actual = calc().getResultStyle();
            if (!List.of(allowed).contains(actual)) {
                throw AssertionMessages.mismatch("result style", "MDX", mdx(),
                        Arrays.toString(allowed), actual.name(), null);
            }
            return this;
        }

        /**
         * A list promised as {@code MUTABLE_LIST} must be the caller's to keep: mutating it
         * must not change what the expression evaluates to. This is the {@code F-37} probe.
         */
        /**
         * Evaluates inside an execution scope.
         *
         * <p>Some functions ask for the current execution context while they run, to read
         * a deadline or to check for cancellation. Outside a query nothing binds one, and
         * the function fails on the lookup rather than on anything to do with its result.
         * A throwaway scope with no timeout puts the assertion back on the promise it is
         * about.
         */
        private static <T> T inExecutionScope(java.util.function.Supplier<T> body) {
            if (org.eclipse.daanse.olap.api.execution.ExecutionContext.currentOrNull() != null) {
                return body.get();
            }
            org.eclipse.daanse.olap.api.execution.ExecutionContext scope =
                    org.eclipse.daanse.olap.api.execution.ExecutionContext.root(
                            java.util.Optional.empty(),
                            org.eclipse.daanse.olap.api.execution.ExecutionMetadata.empty());
            java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
            org.eclipse.daanse.olap.api.execution.ExecutionContext.where(scope,
                    () -> result.set(body.get()));
            return result.get();
        }

        public CalcAssert producesIndependentMutableList() {
            hasResultStyle(ResultStyle.MUTABLE_LIST);
            TupleListCalc listCalc = (TupleListCalc) calc();
            Evaluator evaluator = evaluator();

            TupleList first = inExecutionScope(() -> listCalc.evaluate(evaluator));
            int sizeBefore = first.size();
            List<List<Member>> snapshot = List.copyOf(first);

            try {
                if (!first.isEmpty()) {
                    first.remove(0);
                }
                first.clear();
            } catch (UnsupportedOperationException e) {
                throw AssertionMessages.failed(
                        "list promised as MUTABLE_LIST is not mutable" + NL + "MDX:" + NL + mdx() + NL + NL
                                + "Calc.getResultStyle() reports MUTABLE_LIST, but the returned list "
                                + "rejects modification.",
                        "list.remove(0) succeeds", e.getClass().getName(), e);
            }

            TupleList second = inExecutionScope(() -> listCalc.evaluate(evaluator));
            if (second.size() != sizeBefore || !second.equals(snapshot)) {
                throw AssertionMessages.mismatch("second evaluation after mutating the result",
                        "MDX", mdx(),
                        "second evaluation: " + sizeBefore + " tuples",
                        "second evaluation: " + second.size() + " tuples",
                        """
                        Clearing the returned list changed what the expression evaluates to. The Calc
                        handed out a TupleList.subList view (or the child's own list) over something it
                        caches, so the caller's mutation reached the child.

                        ResultStyle.MUTABLE_LIST promises a list the caller owns. Return a copy,
                        TupleCollections.createList(arity, size) filled from the child, or construct
                        the Calc with mutable = false so the compiler inserts CopyingListCalc.
                        """);
            }
            return this;
        }

        public CalcAssert producesListOfSize(int expected) {
            TupleList list = ((TupleListCalc) calc()).evaluate(evaluator());
            if (list.size() != expected) {
                throw AssertionMessages.mismatch("tuple count", "MDX", mdx(),
                        Integer.toString(expected), Integer.toString(list.size()), null);
            }
            return this;
        }

        // ============ Promise 4: actual type ===============================

        public CalcAssert hasType(Class<? extends Type> expected) {
            Type actual = calc().getType();
            if (!expected.isInstance(actual)) {
                throw AssertionMessages.mismatch("expression type", "MDX", mdx(),
                        expected.getName(), actual.getClass().getName(),
                        "This is the type the call actually produces (FunctionDefinition.createCall), "
                                + "not the category declared on the overload.");
            }
            return this;
        }

        public CalcAssert hasCalcOfType(Class<? extends Calc> expected) {
            if (!expected.isInstance(calc())) {
                throw AssertionMessages.mismatch("compiled calc", "MDX", mdx(),
                        expected.getName(), calc().getClass().getName(), null);
            }
            return this;
        }

        /** The compiled expression, for custom assertions. */
        public Calc<?> calc() {
            if (calc == null) {
                query = connection.parseQuery(mdx());
                query.resolve();
                Expression exp = scalar
                        ? query.getFormulas()[0].getExpression()
                        : query.getAxes()[0].getSet();
                calc = query.compileExpression(exp, scalar, scalar ? null : requested);
            }
            return calc;
        }

        // ---- internals -----------------------------------------------------

        private Evaluator evaluator() {
            calc();
            return connection.getContext().createEvaluator(query.getStatement());
        }

        private Set<String> actualDependencies() {
            Calc<?> compiled = calc();
            Set<String> actual = new LinkedHashSet<>();
            for (Hierarchy hierarchy : query.getCube().getHierarchies()) {
                if (compiled.dependsOn(hierarchy)) {
                    actual.add(hierarchy.getUniqueName());
                }
            }
            return actual;
        }

        private String mdx() {
            return scalar
                    ? "WITH MEMBER [Measures].[Foo] AS " + Util.singleQuoteString(expression)
                            + " SELECT FROM " + quotedCube()
                    : "SELECT {" + expression + "} ON COLUMNS FROM " + quotedCube();
        }

        private String quotedCube() {
            return cubeName.indexOf(' ') >= 0 ? Util.quoteMdxIdentifier(cubeName) : cubeName;
        }

        private static String render(Set<String> hierarchyUniqueNames) {
            return hierarchyUniqueNames.stream().sorted().collect(Collectors.joining(NL));
        }

        private String dependencyNarrative(Set<String> expected, Set<String> actual) {
            Set<String> extra = new LinkedHashSet<>(actual);
            extra.removeAll(expected);
            Set<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(actual);
            StringBuilder buf = new StringBuilder();
            if (!missing.isEmpty()) {
                buf.append("""
                        UNDER-REPORTED: %s
                        This is the dangerous direction. Calc.dependsOn(h) == false is a promise that
                        re-evaluating with a different current member of h gives the same answer.
                        Breaking it produces wrong numbers with no exception and no message.
                        """.formatted(missing));
            }
            if (!extra.isEmpty()) {
                buf.append("""
                        OVER-REPORTED: %s
                        Safe but costly: the expression cannot be reused across cells and is
                        re-evaluated for every position of those hierarchies.
                        A context-setting function should override dependsOn with
                        HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy).
                        """.formatted(extra));
            }
            buf.append("Compiled tree:").append(NL).append(calc().getClass().getSimpleName());
            return buf.toString();
        }
    }
}