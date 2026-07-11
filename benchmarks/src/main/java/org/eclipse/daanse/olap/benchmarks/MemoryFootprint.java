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
package org.eclipse.daanse.olap.benchmarks;

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Random;

/**
 * Heap-delta measurement (not JMH): what 10 million cells cost in the
 * segment-storage representations under comparison — the
 * {@code DenseDoubleSegmentDataset} shape (primitive array + null BitSet)
 * vs a full-BigDecimal object dataset. Run via the benchmarks profile:
 * mvn -Pbenchmarks -pl benchmarks compile exec:java
 *   -Dexec.mainClass=org.eclipse.daanse.olap.benchmarks.MemoryFootprint
 */
public final class MemoryFootprint {

    static final int CELLS = 10_000_000;

    private MemoryFootprint() {
    }

    public static void main(String[] args) {
        Random random = new Random(42);

        long before = usedHeap();
        double[] doubles = new double[CELLS];
        BitSet nulls = new BitSet(CELLS);
        for (int i = 0; i < CELLS; i++) {
            doubles[i] = random.nextDouble() * 10_000;
        }
        long doubleCost = usedHeap() - before;
        System.out.printf("double[] + BitSet   : %,14d bytes  (%.1f B/cell)%n", doubleCost,
                (double) doubleCost / CELLS);

        before = usedHeap();
        BigDecimal[] decimals = new BigDecimal[CELLS];
        for (int i = 0; i < CELLS; i++) {
            decimals[i] = BigDecimal.valueOf(doubles[i]);
        }
        long decimalCost = usedHeap() - before;
        System.out.printf("BigDecimal[]        : %,14d bytes  (%.1f B/cell)  factor %.1fx%n", decimalCost,
                (double) decimalCost / CELLS, (double) decimalCost / doubleCost);

        // keep references alive past the measurements
        System.out.printf("(checksums: %f / %s / %d)%n", doubles[CELLS - 1], decimals[CELLS - 1], nulls.length());
    }

    private static long usedHeap() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 5; i++) {
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return rt.totalMemory() - rt.freeMemory();
    }
}
