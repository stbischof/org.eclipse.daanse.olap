/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.calc.base;

/**
 * Compensated (Neumaier/Kahan-Babuška) summation accumulator.
 *
 * <p>
 * Plain left-to-right {@code double} summation loses low-order bits whenever
 * addends differ in magnitude; over large aggregations the error grows with
 * O(n). Neumaier's variant tracks the running rounding error in a
 * compensation term and folds it into the result, reducing the error to the
 * order of one ulp independent of n — including the case where the addend is
 * larger in magnitude than the running sum (which classic Kahan mishandles).
 *
 * <p> * Used by the double-lane aggregates
 * ({@code FunUtil.sumDouble}/{@code avg}/{@code var}, rolap
 * {@code SumAggregator}).
 * Not thread-safe; one instance per aggregation loop.
 */
public final class CompensatedSum {

    private double sum;
    private double compensation;

    public void add(double value) {
        double t = sum + value;
        if (Math.abs(sum) >= Math.abs(value)) {
            compensation += (sum - t) + value;
        } else {
            compensation += (value - t) + sum;
        }
        sum = t;
    }

    /** The compensated total. */
    public double value() {
        return sum + compensation;
    }
}
