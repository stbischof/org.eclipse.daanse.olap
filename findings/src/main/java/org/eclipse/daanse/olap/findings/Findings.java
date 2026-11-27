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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.findings;

import java.time.Instant;
import java.util.List;

/** How a check records what it found: under the catalog's policy, with the consequence it is to act on. */
public final class Findings {

    private Findings() {
    }

    /**
     * The finding of the rule at the place under the policy, without values: found once, read from nowhere in
     * particular, now.
     */
    public static Finding of(FindingPolicy policy, Rule rule, Layer layer, Place place, String message) {
        return of(policy, rule, layer, place, message, null, 1, List.of());
    }

    /**
     * The finding of the rule at the place under the policy, found now. Its severity is the rule's, an error where the
     * consequence is {@link Consequence#ABORT}; of more than {@link Finding#MAX_SAMPLES} values the first are kept.
     */
    public static Finding of(FindingPolicy policy, Rule rule, Layer layer, Place place, String message, Place source,
            long occurrences, List<String> samples) {
        Consequence consequence = policy.consequenceFor(rule, place);
        Severity severity = consequence == Consequence.ABORT ? Severity.ERROR : rule.severity();
        List<String> kept = samples.size() > Finding.MAX_SAMPLES ? samples.subList(0, Finding.MAX_SAMPLES) : samples;
        return new Finding(rule.id(), severity, place, message, layer, consequence, source, occurrences, kept,
                Instant.now());
    }
}
