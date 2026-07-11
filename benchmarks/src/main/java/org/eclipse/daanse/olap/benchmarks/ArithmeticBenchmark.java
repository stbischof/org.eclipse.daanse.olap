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
 * Multiply and divide over 100k value pairs — the calculated-member path.
 * BigDecimal division REQUIRES a MathContext (unbounded division throws on
 * non-terminating expansions), which is the structural "exactness ends at
 * division" argument of the evaluation.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class ArithmeticBenchmark {

    static final int N = 100_000;

    double[] a;
    double[] b;
    BigDecimal[] da;
    BigDecimal[] db;

    @Setup
    public void setup() {
        Random random = new Random(4242);
        a = new double[N];
        b = new double[N];
        da = new BigDecimal[N];
        db = new BigDecimal[N];
        for (int i = 0; i < N; i++) {
            a[i] = random.nextDouble() * 10_000 + 0.0001;
            b[i] = random.nextDouble() * 100 + 0.0001;
            da[i] = BigDecimal.valueOf(a[i]);
            db[i] = BigDecimal.valueOf(b[i]);
        }
    }

    @Benchmark
    public double doubleMultiply() {
        double acc = 0;
        for (int i = 0; i < N; i++) {
            acc += a[i] * b[i];
        }
        return acc;
    }

    @Benchmark
    public BigDecimal bigDecimalMultiply() {
        BigDecimal acc = BigDecimal.ZERO;
        for (int i = 0; i < N; i++) {
            acc = acc.add(da[i].multiply(db[i]));
        }
        return acc;
    }

    @Benchmark
    public BigDecimal bigDecimalMultiplyDecimal128() {
        BigDecimal acc = BigDecimal.ZERO;
        for (int i = 0; i < N; i++) {
            acc = acc.add(da[i].multiply(db[i], MathContext.DECIMAL128), MathContext.DECIMAL128);
        }
        return acc;
    }

    @Benchmark
    public double doubleDivide() {
        double acc = 0;
        for (int i = 0; i < N; i++) {
            acc += a[i] / b[i];
        }
        return acc;
    }

    @Benchmark
    public BigDecimal bigDecimalDivideDecimal128() {
        BigDecimal acc = BigDecimal.ZERO;
        for (int i = 0; i < N; i++) {
            acc = acc.add(da[i].divide(db[i], MathContext.DECIMAL128), MathContext.DECIMAL128);
        }
        return acc;
    }
}
