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
import java.util.Objects;

/**
 * What a check found: the rule broken, how much it weighs, where it sits, what it says, on which layer it was made,
 * what was done about it, and -- for a finding about data -- where the data was read, how often it occurred and a few
 * of the values.
 *
 * @param rule        the rule broken
 * @param severity    the weight: the rule's, an error under {@link Consequence#ABORT}
 * @param place       where in the catalog
 * @param message     what is wrong and what to do, never blank
 * @param layer       where the check ran
 * @param consequence what was done
 * @param source      the table or column the finding was read from, or {@code null}
 * @param occurrences how often it occurred, at least one; one for a finding about the catalog's structure
 * @param samples     at most {@link #MAX_SAMPLES} of the values found, in the order met
 * @param at          when it was found; a finding about data describes the data as of then
 */
public record Finding(RuleId rule, Severity severity, Place place, String message, Layer layer,
        Consequence consequence, Place source, long occurrences, List<String> samples, Instant at) {

    /** The most values a finding carries. */
    public static final int MAX_SAMPLES = 5;

    public Finding {
        Objects.requireNonNull(rule, "the rule of a finding");
        Objects.requireNonNull(severity, "the severity of a finding of " + rule);
        Objects.requireNonNull(place, "the place of a finding of " + rule);
        Objects.requireNonNull(message, "the message of a finding of " + rule);
        Objects.requireNonNull(layer, "the layer of a finding of " + rule);
        Objects.requireNonNull(consequence, "the consequence of a finding of " + rule);
        Objects.requireNonNull(samples, "the samples of a finding of " + rule);
        Objects.requireNonNull(at, "the time of a finding of " + rule);
        if (message.isBlank()) {
            throw new IllegalArgumentException("a finding of " + rule + " at " + place + " says nothing");
        }
        if (occurrences < 1) {
            throw new IllegalArgumentException("a finding of " + rule + " at " + place + " occurred " + occurrences
                    + " times; at least once");
        }
        if (samples.size() > MAX_SAMPLES) {
            throw new IllegalArgumentException("a finding of " + rule + " at " + place + " carries " + samples.size()
                    + " values; at most " + MAX_SAMPLES);
        }
        samples = List.copyOf(samples);
    }

    /** Whether the work stops because of it. */
    public boolean aborts() {
        return consequence == Consequence.ABORT;
    }

    /** One line: rule, weight, consequence, place and message. */
    @Override
    public String toString() {
        return rule + " " + severity + " " + consequence + " at " + place.text() + ": " + message;
    }
}
