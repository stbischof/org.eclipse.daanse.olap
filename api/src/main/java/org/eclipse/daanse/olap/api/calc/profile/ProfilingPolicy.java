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

/**
 * Whether the statements of one scope are profiled, how much of each plan is kept, and where the plans go. A scope is
 * the context, a cube (with its catalog), or a connection; a statement with its own {@link ProfileHandler} (EXPLAIN) is
 * always profiled into that handler. The narrower scope wins: connection over cube over context, and an explicit
 * {@link Mode#OFF} on a narrower scope beats {@link Mode#ON} above it. {@link Mode#INHERIT} says nothing and leaves the
 * decision to the wider scope.
 *
 * @param mode        whether the scope profiles
 * @param sampleEvery for {@link Mode#SAMPLE}: every how-many-th statement of the scope is profiled
 * @param detail      how much of each plan is handed on
 * @param topN        for {@link Detail#TOP}: how many calcs, the most expensive by their own time
 * @param sink        where the plans of a profiled statement go
 */
public record ProfilingPolicy(Mode mode, int sampleEvery, Detail detail, int topN, Sink sink) {

    /** Whether a scope profiles. */
    public enum Mode {
        /** The scope says nothing; the wider scope decides. */
        INHERIT,
        /** Not profiled, even if a wider scope says so. */
        OFF,
        /** Every statement profiled. */
        ON,
        /** Every {@code sampleEvery}-th statement of the scope profiled. */
        SAMPLE
    }

    /** How much of a plan is handed on. */
    public enum Detail {
        /** The whole tree of calcs. */
        FULL,
        /** The {@code topN} calcs that took the most time of their own, as a flat list. */
        TOP
    }

    /** Where the plans of a profiled statement go. */
    public enum Sink {
        /** The profile logger {@code daanse.profile}, at INFO, with the connection's id. */
        LOG,
        /** The handler set with {@link ProfilingControl#setSink(ProfileHandler)}. */
        HANDLER,
        /**
         * The {@link ProfileCounters} of the context ({@link ProfilingControl#counters()}): every evaluation of every
         * calc of the statement is counted under its function name, the formulas of calculated members included, and
         * the counts add up over statements. No plan is written.
         */
        COUNTERS
    }

    /** How many calcs {@link Detail#TOP} keeps unless told otherwise. */
    public static final int DEFAULT_TOP_N = 20;

    /** A scope that says nothing. */
    public static final ProfilingPolicy UNSET = new ProfilingPolicy(Mode.INHERIT, 0, Detail.FULL, 0, Sink.LOG);

    public ProfilingPolicy {
        if (mode == Mode.SAMPLE && sampleEvery < 1) {
            throw new IllegalArgumentException("sampleEvery must be at least 1, was " + sampleEvery);
        }
        if (detail == Detail.TOP && topN < 1) {
            throw new IllegalArgumentException("topN must be at least 1, was " + topN);
        }
    }

    public static ProfilingPolicy on(Sink sink) {
        return new ProfilingPolicy(Mode.ON, 0, Detail.FULL, 0, sink);
    }

    public static ProfilingPolicy off() {
        return new ProfilingPolicy(Mode.OFF, 0, Detail.FULL, 0, Sink.LOG);
    }

    public static ProfilingPolicy sample(int every, Sink sink) {
        return new ProfilingPolicy(Mode.SAMPLE, every, Detail.FULL, 0, sink);
    }

    /** This policy, handing on only the {@code n} calcs that took the most time of their own. */
    public ProfilingPolicy top(int n) {
        return new ProfilingPolicy(mode, sampleEvery, Detail.TOP, n, sink);
    }

    /** This scope's policy, or the wider one where this one says nothing. */
    public ProfilingPolicy over(ProfilingPolicy wider) {
        return mode == Mode.INHERIT ? wider : this;
    }
}
