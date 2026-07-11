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
package org.eclipse.daanse.olap.nullsemantics;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Random;

import org.eclipse.daanse.olap.calc.base.CompensatedSum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Neumaier summation vs plain double summation
 * against a BigDecimal reference .
 */
class CompensatedSumTest {

    @Test
    @DisplayName("Classic Kahan failure case: large value entering a small sum is not lost")
    void largeAddendIsNotLost() {
        CompensatedSum sum = new CompensatedSum();
        sum.add(1.0);
        sum.add(1e100);
        sum.add(1.0);
        sum.add(-1e100);
        assertThat(sum.value()).isEqualTo(2.0);

        // The same sequence with plain doubles collapses to 0.0 — this is the
        // error class the compensated accumulator removes.
        double plain = 0.0;
        plain += 1.0;
        plain += 1e100;
        plain += 1.0;
        plain += -1e100;
        assertThat(plain).isEqualTo(0.0);
    }

    @Test
    @DisplayName("10^7 addends of mixed magnitude: compensated result matches BigDecimal reference")
    void adversarialMixedMagnitudes() {
        final int n = 10_000_000;
        // Deterministic pseudo-random values across ~12 orders of magnitude.
        Random random = new Random(42);

        CompensatedSum compensated = new CompensatedSum();
        double plain = 0.0;
        BigDecimal reference = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            double v = random.nextDouble() * Math.pow(10, random.nextInt(12) - 4);
            if ((i & 1) == 0) {
                v = -v;
            }
            compensated.add(v);
            plain += v;
            reference = reference.add(new BigDecimal(v, MathContext.DECIMAL128));
        }

        double referenceValue = reference.doubleValue();
        double compensatedError = Math.abs(compensated.value() - referenceValue);
        double plainError = Math.abs(plain - referenceValue);

        // The compensated sum must be within a few ulps of the reference ...
        assertThat(compensatedError).isLessThanOrEqualTo(4 * Math.ulp(referenceValue));
        // ... and at least as accurate as the naive loop (usually far better).
        assertThat(compensatedError).isLessThanOrEqualTo(plainError);
    }
}
