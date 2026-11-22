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
package org.eclipse.daanse.olap.api.calc.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Evaluations counted under the name of the function a calc computes, over every statement profiled into this sink
 * ({@link ProfilingPolicy.Sink#COUNTERS}). As many entries as there are function names: a long-running context can keep
 * them for good, and a metrics exporter reads {@link #counts()}.
 *
 * <p>The profile tree of a statement holds only what a handler is shown (axes, named sets); the formulas of calculated
 * members are compiled while cells are evaluated and reach no handler. The hulls of a counted statement count here at
 * every evaluation, so the formulas are in, and so is the time a calc took of its own -- what it took minus what the
 * calcs it called took, the number to rank by.
 */
public final class ProfileCounters {

    /**
     * What was counted for one function.
     *
     * @param function the function name, or the simple class name of a calc nobody named
     * @param calls    evaluations
     * @param nanos    their time together, the calcs they called included
     * @param ownNanos their time without the calcs they called
     * @param elements the elements of the sets they returned
     */
    public record Count(String function, long calls, long nanos, long ownNanos, long elements) {
    }

    /** The counts of one function, added to at every evaluation of a counted hull. */
    public static final class Function {
        private final String name;
        private final LongAdder calls = new LongAdder();
        private final LongAdder nanos = new LongAdder();
        private final LongAdder ownNanos = new LongAdder();
        private final LongAdder elements = new LongAdder();

        private Function(String name) {
            this.name = name;
        }

        /** One evaluation: its time, its own time and the elements of the set it returned (0 for a value). */
        public void add(long nanos, long ownNanos, long elements) {
            this.calls.increment();
            this.nanos.add(nanos);
            this.ownNanos.add(ownNanos);
            if (elements != 0) {
                this.elements.add(elements);
            }
        }

        Count count() {
            return new Count(name, calls.sum(), nanos.sum(), ownNanos.sum(), elements.sum());
        }
    }

    private final ConcurrentHashMap<String, Function> functions = new ConcurrentHashMap<>();

    /** The counts of a function, made on first use. */
    public Function function(String name) {
        return functions.computeIfAbsent(name, Function::new);
    }

    /** Every function counted so far, the most expensive by own time first. */
    public List<Count> counts() {
        List<Count> counts = new ArrayList<>(functions.size());
        for (Function function : functions.values()) {
            counts.add(function.count());
        }
        counts.sort(Comparator.comparingLong(Count::ownNanos).reversed().thenComparing(Count::function));
        return counts;
    }

    /** The {@code n} functions that took the most time of their own. */
    public List<Count> top(int n) {
        List<Count> counts = counts();
        return counts.size() <= n ? counts : List.copyOf(counts.subList(0, n));
    }

    /** Forgets every count. */
    public void reset() {
        functions.clear();
    }
}
