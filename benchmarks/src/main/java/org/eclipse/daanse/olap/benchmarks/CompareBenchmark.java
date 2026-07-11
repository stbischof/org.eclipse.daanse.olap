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
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * The Order/TopCount path: sorting 100k cell values. Compares primitive
 * ordering against Double.compareTo and BigDecimal.compareTo (scale-aware,
 * allocation-free but multi-word comparison).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class CompareBenchmark {

    static final int N = 100_000;

    double[] primitivesTemplate;
    Double[] boxedTemplate;
    BigDecimal[] decimalsTemplate;

    double[] primitives;
    Double[] boxed;
    BigDecimal[] decimals;

    @Setup
    public void setup() {
        Random random = new Random(7);
        primitivesTemplate = new double[N];
        boxedTemplate = new Double[N];
        decimalsTemplate = new BigDecimal[N];
        for (int i = 0; i < N; i++) {
            double v = random.nextDouble() * Math.pow(10, random.nextInt(8) - 2);
            primitivesTemplate[i] = v;
            boxedTemplate[i] = v;
            decimalsTemplate[i] = BigDecimal.valueOf(v);
        }
    }

    @Setup(Level.Invocation)
    public void refresh() {
        primitives = primitivesTemplate.clone();
        boxed = boxedTemplate.clone();
        decimals = decimalsTemplate.clone();
    }

    @Benchmark
    public double[] sortPrimitive() {
        Arrays.sort(primitives);
        return primitives;
    }

    @Benchmark
    public Double[] sortBoxedDouble() {
        Arrays.sort(boxed, Double::compareTo);
        return boxed;
    }

    @Benchmark
    public BigDecimal[] sortBigDecimal() {
        Arrays.sort(decimals, BigDecimal::compareTo);
        return decimals;
    }
}
