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
 * Proxy for the calc-layer expression path: a nested "calc" produces a boxed
 * value per cell, an operator combines two of them and re-boxes the result —
 * the allocation profile of {@code Calc<Double>} vs {@code Calc<BigDecimal>}
 * trees (one wrapper allocation per intermediate value per cell).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class BoxingChurnBenchmark {

    interface DoubleCell {
        Double evaluate(int i);
    }

    interface DecimalCell {
        BigDecimal evaluate(int i);
    }

    static final int N = 100_000;

    double[] left;
    double[] right;
    BigDecimal[] dleft;
    BigDecimal[] dright;

    DoubleCell doublePlus;
    DecimalCell decimalPlus;

    @Setup
    public void setup() {
        Random random = new Random(11);
        left = new double[N];
        right = new double[N];
        dleft = new BigDecimal[N];
        dright = new BigDecimal[N];
        for (int i = 0; i < N; i++) {
            left[i] = random.nextDouble() * 1000;
            right[i] = random.nextDouble() * 1000;
            dleft[i] = BigDecimal.valueOf(left[i]);
            dright[i] = BigDecimal.valueOf(right[i]);
        }
        DoubleCell l = i -> left[i];
        DoubleCell r = i -> right[i];
        doublePlus = i -> l.evaluate(i) + r.evaluate(i); // unbox, add, rebox

        DecimalCell dl = i -> dleft[i];
        DecimalCell dr = i -> dright[i];
        decimalPlus = i -> dl.evaluate(i).add(dr.evaluate(i), MathContext.DECIMAL128);
    }

    @Benchmark
    public double doubleCalcTree() {
        double acc = 0;
        for (int i = 0; i < N; i++) {
            acc += doublePlus.evaluate(i);
        }
        return acc;
    }

    @Benchmark
    public BigDecimal decimalCalcTree() {
        BigDecimal acc = BigDecimal.ZERO;
        for (int i = 0; i < N; i++) {
            acc = acc.add(decimalPlus.evaluate(i));
        }
        return acc;
    }
}
