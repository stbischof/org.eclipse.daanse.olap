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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.result.ResultImpl;

/**
 * The dependency check of one query: every calc that claims not to depend on a hierarchy is made to prove it
 * by being evaluated again in another member of that hierarchy. Switched on with {@code testExpDependencies}.
 *
 * <p>
 * A <em>mode</em> of the evaluator, not an art of its own: the check hangs on the {@link EvaluatorRoot} of the
 * evaluator the art runs anyway, so a ROLAP query keeps its {@code RolapEvaluator} (slicer predicates, compound
 * slicer, cube of the measure) and a provider query its root hooks (the what-if stand) while being checked.
 * Before, the check was a subclass of {@link EvaluatorImpl} that {@code ResultImpl} put in place of the
 * art's evaluator, so every one of those hooks fell away as soon as the switch was on -- and under ROLAP the
 * casts to {@code RolapEvaluator} failed outright.
 *
 * <p>
 * Where it is asked: the calcs the {@link DependencyCheckHook} wraps around every compiled expression
 * call {@link #evaluate} with the hierarchies the expression claims to be independent of.
 *
 * @author jhyde (the check, 2005)
 */
public final class DependencyCheck {

    private final ResultImpl result;
    private final int expDeps;
    private int callCount;
    private int fakeCallCount;
    private int fakeCount;
    private boolean faking;
    private boolean disabled;
    private final Random random = new SecureRandom();

    /**
     * @param result the result being built; a dirty result is never checked, its values are not final
     * @param expDeps how hard to check: the ratio of checking evaluations to real ones is capped at
     *            {@code 2 * expDeps}, at random
     */
    public DependencyCheck(ResultImpl result, int expDeps) {
        this.result = result;
        this.expDeps = expDeps;
    }

    /** The check the evaluator runs under, or null when the query is not being checked. */
    public static DependencyCheck of(Evaluator evaluator) {
        return evaluator instanceof EvaluatorImpl impl ? impl.getRoot().dependencyCheck() : null;
    }

    /** Stops checking from now on; the query keeps answering (the eval-depth guard calls it). */
    public void disable() {
        disabled = true;
    }

    /** How many checking evaluations ran. */
    public int fakeCount() {
        return fakeCount;
    }

    /**
     * Evaluates {@code calc} for real, then -- within the budget -- once more in another member of one of
     * the hierarchies it claims to be independent of, and fails the query when the two answers differ.
     */
    public Object evaluate(EvaluatorImpl evaluator, Calc<?> calc, Hierarchy[] independentHierarchies,
            String mdxString) {
        if (faking) {
            ++fakeCallCount;
        } else {
            ++callCount;
        }
        // Evaluate the call for real.
        final Object real = calc.evaluate(evaluator);
        if (result.isDirty()) {
            return real;
        }

        // If the result is a list and says that it is mutable, see whether it really is: a caller that was
        // promised a list it may change must not meet an unmodifiable one.
        if (calc.getResultStyle() == ResultStyle.MUTABLE_LIST) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) real;
            if (!list.isEmpty()) {
                final Object zeroth = list.getFirst();
                try {
                    list.set(0, zeroth);
                } catch (UnsupportedOperationException notMutable) {
                    throw Util.newInternal(notMutable, "Expression '" + mdxString + "' announces a mutable list ("
                            + calc.getClass().getName() + ") but hands out one that cannot be changed ("
                            + real.getClass().getName() + ")");
                }
            }
        }

        // Change one of the allegedly independent dimensions and evaluate again. Not when the check is
        // disabled, when it is already faking another dimension, when non-empty cells are being filtered out
        // (which makes the expression dependent on everything), or when the ratio of fake evaluations to real
        // ones is too high (which would make the query too slow).
        if (disabled
            || faking
            || evaluator.isNonEmpty()
            || (double) fakeCallCount > (double) callCount * random.nextDouble() * 2 * expDeps) {
            return real;
        }
        if (independentHierarchies.length == 0) {
            return real;
        }
        faking = true;
        ++fakeCount;
        ++fakeCallCount;
        final int i = random.nextInt(independentHierarchies.length);
        final Member saveMember = evaluator.getContext(independentHierarchies[i]);
        final Member otherMember = chooseOtherMember(saveMember, evaluator.getQuery().getCatalogReader(false));
        evaluator.setContext(otherMember);
        final Object other = calc.evaluate(evaluator);
        if (!same(other, real)) {
            final Member[] members = evaluator.getMembers();
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
                .append(buf).append("}; First result: ")
                .append(text(real)).append(", Second result: ")
                .append(text(other)).toString());
        }
        // Restore context.
        evaluator.setContext(saveMember);
        faking = false;
        return real;
    }

    /**
     * Chooses another member of the same hierarchy. The member will come from all levels with the same
     * probability. Calculated members are not included.
     */
    private Member chooseOtherMember(final Member save, CatalogReader schemaReader) {
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
                    members = schemaReader.getLevelMembers(levels.get(i), false);
                } else {
                    members = schemaReader.getMemberChildren(member);
                }
                if (members.isEmpty()) {
                    break;
                }
                member = members.get(random.nextInt(members.size()));
            }
            // If the member chosen happens to be the same as the original member, try again. Give up after
            // 100 attempts (in case the hierarchy has only one member).
            if (member != save || ++attempt > 100) {
                return member;
            }
        }
    }

    private static boolean same(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        if (o2 == null) {
            return false;
        }
        if (o1 instanceof Object[] a1) {
            if (o2 instanceof Object[] a2 && a1.length == a2.length) {
                for (int i = 0; i < a1.length; i++) {
                    if (!same(a1[i], a2[i])) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        if (o1 instanceof List<?> l1) {
            return o2 instanceof List<?> l2 && same(l1.toArray(), l2.toArray());
        }
        if (o1 instanceof Iterable<?> i1) {
            return o2 instanceof Iterable<?> i2 && same(toList(i1), toList(i2));
        }
        return o1.equals(o2);
    }

    private static <T> List<T> toList(Iterable<T> iterable) {
        final ArrayList<T> list = new ArrayList<>();
        for (T t : iterable) {
            list.add(t);
        }
        return list;
    }

    private static String text(Object o) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        text(o, pw);
        return sw.toString();
    }

    private static void text(Object o, PrintWriter pw) {
        switch (o) {
        case Object[] a -> {
            pw.print("{");
            for (int i = 0; i < a.length; i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                text(a[i], pw);
            }
            pw.print("}");
        }
        case List<?> list -> text(list.toArray(), pw);
        case Member member -> pw.print(member.getUniqueName());
        case null, default -> pw.print(o);
        }
    }
}
