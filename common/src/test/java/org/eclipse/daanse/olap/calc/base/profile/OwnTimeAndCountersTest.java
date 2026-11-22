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
package org.eclipse.daanse.olap.calc.base.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;
import org.eclipse.daanse.olap.api.calc.profile.ProfileCounters;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedDoubleCalc;
import org.junit.jupiter.api.Test;

/**
 * P5: a hull knows the time its calc took of its own -- what it took minus what the hulls it called took -- and, for
 * the counters sink, counts every evaluation under the function's name; a plan cut to the top calcs keeps the most
 * expensive by that own time.
 */
class OwnTimeAndCountersTest {

    /** Spends about {@code micros} and then evaluates its children. */
    static final class Busy extends AbstractNestedDoubleCalc {
        private final long micros;

        Busy(long micros, Calc<?>... children) {
            super(NumericType.INSTANCE, children);
            this.micros = micros;
        }

        @Override
        public Double evaluateInternal(Evaluator evaluator) {
            long until = System.nanoTime() + micros * 1_000;
            while (System.nanoTime() < until) {
                Thread.onSpinWait();
            }
            for (Calc<?> child : getChildCalcs()) {
                child.evaluate(evaluator);
            }
            return 1.0;
        }
    }

    @Test
    void theOwnTimeOfACalcLeavesOutTheHullsItCalls() {
        Calc<?> inner = ProfilingCalc.wrap(new Busy(2_000), "Inner");
        Calc<?> outer = ProfilingCalc.wrap(new Busy(1_000, inner), "Outer");

        outer.evaluate(null);
        outer.evaluate(null);

        CalculationProfile innerProfile = inner.getCalculationProfile();
        CalculationProfile outerProfile = outer.getCalculationProfile();
        assertThat(innerProfile.ownNanos()).as("a leaf owns all its time").isEqualTo(innerProfile.nanos());
        assertThat(outerProfile.ownNanos()).isEqualTo(outerProfile.nanos() - innerProfile.nanos());
        assertThat(outerProfile.ownNanos()).isPositive().isLessThan(outerProfile.nanos());
        assertThat(innerProfile.nanos()).isGreaterThanOrEqualTo(2 * 2_000_000L);
    }

    @Test
    void theCountersAddUpEveryEvaluationUnderTheFunctionName() {
        ProfileCounters counters = new ProfileCounters();
        Calc<?> leftMinus = ProfilingCalc.wrap(new Busy(300), "-", counters);
        Calc<?> rightMinus = ProfilingCalc.wrap(new Busy(300), "-", counters);
        Calc<?> divide = ProfilingCalc.wrap(new Busy(50, leftMinus, rightMinus), "/", counters);
        Calc<?> nameless = ProfilingCalc.wrap(new Busy(10), null, counters);

        divide.evaluate(null);
        divide.evaluate(null);
        nameless.evaluate(null);

        Map<String, ProfileCounters.Count> byName = new java.util.HashMap<>();
        counters.counts().forEach(count -> byName.put(count.function(), count));
        assertThat(byName.get("-").calls()).as("two hulls of one function, twice each").isEqualTo(4);
        assertThat(byName.get("/").calls()).isEqualTo(2);
        assertThat(byName.get("/").ownNanos()).isLessThan(byName.get("/").nanos());
        assertThat(byName.get("Busy").calls()).as("a calc nobody named counts under its class").isEqualTo(1);

        counters.reset();
        assertThat(counters.counts()).isEmpty();
    }

    /**
     * The order of the counters, on times given rather than measured: on the wall clock a preemption during the own part of
     * {@code /} under the load of a chain put it ahead of {@code -}. {@code /} took the most in all, {@code -}
     * the most of its own -- the top is by its own.
     */
    @Test
    void theTopIsTheFunctionThatTookTheMostOfItsOwn() {
        ProfileCounters counters = new ProfileCounters();
        counters.function("/").add(1_400_000, 200_000, 0);
        counters.function("-").add(600_000, 600_000, 0);
        counters.function("-").add(600_000, 600_000, 0);
        counters.function("Busy").add(10_000, 10_000, 0);

        assertThat(counters.top(1)).extracting(ProfileCounters.Count::function).containsExactly("-");
        assertThat(counters.top(3)).extracting(ProfileCounters.Count::function).containsExactly("-", "/", "Busy");
        assertThat(counters.top(1).get(0).ownNanos()).isEqualTo(1_200_000);
    }

    @Test
    void aPlanCutToTheTopKeepsTheCalcsThatTookTheMostOfTheirOwn() {
        CalculationProfile b = profile("b", 30, List.of());
        CalculationProfile a = profile("a", 50, List.of(b));
        CalculationProfile c = profile("c", 40, List.of());
        CalculationProfile root = profile("root", 1, List.of(a, c));

        CalculationProfile top = ProfilingScopes.top(root, 2);

        assertThat(top.childProfiles()).extracting(p -> p.name().orElseThrow()).containsExactly("a", "c");
        assertThat(top.childProfiles()).allSatisfy(p -> assertThat(p.childProfiles()).isEmpty());
        assertThat(top.nanos()).isEqualTo(root.nanos());
    }

    private static CalculationProfile profile(String name, long own, List<CalculationProfile> children) {
        long nanos = own + children.stream().mapToLong(CalculationProfile::nanos).sum();
        return new CalcProfileR(Busy.class, Optional.of(name), NumericType.INSTANCE, ResultStyle.VALUE, nanos, own, 0, 0,
                Map.of(), children);
    }
}
