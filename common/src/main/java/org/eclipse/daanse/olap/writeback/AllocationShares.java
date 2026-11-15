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
package org.eclipse.daanse.olap.writeback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.result.AllocationPolicy;

/**
 * How a value written to one cell is spread over the cells beneath it.
 * <p>
 * Four ways are named by {@link AllocationPolicy}: give every cell the same share of the new value or of
 * the difference, or give each a share in proportion to what it held. Where nothing was held there is
 * nothing to be in proportion to, so a weighted way falls back to the equal one.
 * <p>
 * The arithmetic says nothing about where the cells are kept, nor about what names them: it takes what
 * they hold, keyed however the caller keys them, and answers with what to take away and what to put back.
 * Whether that is written into a table, into a store or anywhere else is the caller's business.
 */
public final class AllocationShares {

    /**
     * What to do to each cell: {@code removals} is what it held, negated, and {@code additions} is its new
     * share. Both are given, because a writeback that keeps a delta needs the old value too.
     */
    public record Shares<K>(Map<K, Double> removals, Map<K, Double> additions) {
    }

    private AllocationShares() {
        // static access only
    }

    /**
     * Works out the shares.
     *
     * @param held     what each cell holds today; must not be empty, because there would be nothing to
     *                 spread over
     * @param target   the value written to the cell above
     * @param policy   the way to spread it
     * @param integral whether the shares must be whole numbers - then they are handed out by largest
     *                 remainder, so that they still sum to the target
     */
    public static <K> Shares<K> of(Map<K, Object> held, double target, AllocationPolicy policy,
            boolean integral) {
        if (held.isEmpty()) {
            throw new IllegalArgumentException("there are no cells to spread a written value over");
        }
        Map<K, Double> additions = new HashMap<>();
        Map<K, Double> removals = new HashMap<>();
        int size = held.size();
        double sum = held.values().stream().mapToDouble(AllocationShares::num).sum();

        AllocationPolicy effective = policy;
        if (sum == 0d && policy == AllocationPolicy.WEIGHTED_ALLOCATION) {
            effective = AllocationPolicy.EQUAL_ALLOCATION;
        } else if (sum == 0d && policy == AllocationPolicy.WEIGHTED_INCREMENT) {
            effective = AllocationPolicy.EQUAL_INCREMENT;
        }

        switch (effective) {
        case WEIGHTED_ALLOCATION:
            for (Map.Entry<K, Object> entry : held.entrySet()) {
                double old = num(entry.getValue());
                removals.put(entry.getKey(), -old);
                additions.put(entry.getKey(), target / sum * old);
            }
            break;
        case EQUAL_INCREMENT: {
            double offset = target - sum;
            for (Map.Entry<K, Object> entry : held.entrySet()) {
                double old = num(entry.getValue());
                removals.put(entry.getKey(), -old);
                additions.put(entry.getKey(), old + offset / size);
            }
            break;
        }
        case WEIGHTED_INCREMENT: {
            double offset = target - sum;
            for (Map.Entry<K, Object> entry : held.entrySet()) {
                double old = num(entry.getValue());
                removals.put(entry.getKey(), -old);
                additions.put(entry.getKey(), old + offset / sum * old);
            }
            break;
        }
        case EQUAL_ALLOCATION:
        default: {
            double share = target / size;
            for (Map.Entry<K, Object> entry : held.entrySet()) {
                removals.put(entry.getKey(), -num(entry.getValue()));
                additions.put(entry.getKey(), share);
            }
        }
        }

        if (integral) {
            // Rounding each cell on its own drifted: the rounded shares summed to less than the target.
            // The old-value side rounds per cell - it is whole in a whole-numbered fact anyway - and the
            // shares are handed out by largest remainder so that their sum is the target.
            removals.replaceAll((cell, value) -> (double) Math.round(value));
            largestRemainder(additions, Math.round(target));
        }
        return new Shares<>(removals, additions);
    }

    /** Evaluator output is Double, Integer, BigDecimal or null (empty cell). */
    static double num(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0d;
    }

    /**
     * Floors every share and hands the missing units to the largest
     * fractional remainders, so the rounded shares sum exactly to
     * {@code targetTotal}. Mathematically sum(d) == target for every
     * policy above, which makes the remainder count 0..size-1.
     */
    private static <K> void largestRemainder(Map<K, Double> d, long targetTotal) {
        List<Map.Entry<K, Double>> entries = new ArrayList<>(d.entrySet());
        long floorSum = 0;
        double[] remainders = new double[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            double share = entries.get(i).getValue();
            long floor = (long) Math.floor(share);
            remainders[i] = share - floor;
            d.put(entries.get(i).getKey(), (double) floor);
            floorSum += floor;
        }
        long missing = targetTotal - floorSum;
        // hand out one unit per pass to the largest remainder not yet topped up
        boolean[] used = new boolean[entries.size()];
        while (missing > 0) {
            int best = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (!used[i] && (best < 0 || remainders[i] > remainders[best])) {
                    best = i;
                }
            }
            if (best < 0) {
                break; // more units than cells: numeric edge, leave the rest
            }
            used[best] = true;
            d.put(entries.get(best).getKey(), d.get(entries.get(best).getKey()) + 1d);
            missing--;
        }
    }
}
