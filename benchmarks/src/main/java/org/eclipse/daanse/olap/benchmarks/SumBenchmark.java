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
import java.math.MathContext;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.eclipse.daanse.olap.calc.base.CompensatedSum;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * The aggregation hot loop: summing 1e6 values of mixed magnitude in the
 * candidate representations. Basis for the full-BigDecimal evaluation and
 * the CompensatedSum-vs-plain-double comparison.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class SumBenchmark {

    static final int N = 1_000_000;

    double[] primitives;
    Double[] boxed;
    BigDecimal[] decimals;

    @Setup
    public void setup() {
        Random random = new Random(42);
        primitives = new double[N];
        boxed = new Double[N];
        decimals = new BigDecimal[N];
        for (int i = 0; i < N; i++) {
            double v = random.nextDouble() * Math.pow(10, random.nextInt(12) - 4);
            if ((i & 1) == 0) {
                v = -v;
            }
            primitives[i] = v;
            boxed[i] = v;
            decimals[i] = BigDecimal.valueOf(v);
        }
    }

    @Benchmark
    public double primitiveDouble() {
        double sum = 0.0;
        for (double v : primitives) {
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public double boxedDouble() {
        // the calc layer's actual shape: Double cell values, unboxed per add
        double sum = 0.0;
        for (Double v : boxed) {
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public double compensatedSum() {
        CompensatedSum sum = new CompensatedSum();
        for (Double v : boxed) {
            sum.add(v);
        }
        return sum.value();
    }

    @Benchmark
    public BigDecimal bigDecimalPreboxed() {
        // values already exist as BigDecimal (post-intake ideal case)
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : decimals) {
            sum = sum.add(v);
        }
        return sum;
    }

    @Benchmark
    public BigDecimal bigDecimalDecimal128() {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : decimals) {
            sum = sum.add(v, MathContext.DECIMAL128);
        }
        return sum;
    }

    @Benchmark
    public BigDecimal bigDecimalFromDouble() {
        // full-conversion scenario: double cells converted at aggregation time
        BigDecimal sum = BigDecimal.ZERO;
        for (double v : primitives) {
            sum = sum.add(BigDecimal.valueOf(v));
        }
        return sum;
    }
}
