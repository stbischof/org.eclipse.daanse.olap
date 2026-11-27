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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** A finding carries its rule, weight, place, message, layer and consequence, and for data a few values. */
class FindingTest {

    private static final Place LINK = Place.parse("catalog Shop / cube Sales / measure group Sales / link Shop");
    private static final Place COLUMN = Place.parse("table sales / column shop_id");

    private static Finding missingKey(String message, long occurrences, List<String> samples) {
        return new Finding(RuleId.of("data/D1"), Severity.WARNING, LINK, message, Layer.DATA, Consequence.REPORT,
                COLUMN, occurrences, samples, Instant.EPOCH);
    }

    @Test
    void aFindingHoldsWhatItWasGiven() {
        Finding finding = missingKey("A sale names shop 7, which is no shop.", 1, List.of("7"));
        assertThat(finding.rule()).as("rule").isEqualTo(RuleId.of("data/D1"));
        assertThat(finding.place()).as("place").isEqualTo(LINK);
        assertThat(finding.source()).as("source").isEqualTo(COLUMN);
        assertThat(finding.samples()).as("samples").containsExactly("7");
        assertThat(finding.aborts()).as("a reported finding stops nothing").isFalse();
    }

    @Test
    void theOneLineNamesRuleWeightConsequenceAndPlace() {
        String line = missingKey("A sale names shop 7, which is no shop.", 1, List.of("7")).toString();
        assertThat(line).as("the line").startsWith("data/D1 WARNING REPORT at catalog Shop / cube Sales")
                .endsWith(": A sale names shop 7, which is no shop.");
    }

    @Test
    void aFindingThatSaysNothingIsRefused() {
        assertThatThrownBy(() -> missingKey(" ", 1, List.of())).as("a blank message")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aFindingThatNeverOccurredIsRefused() {
        assertThatThrownBy(() -> missingKey("says", 0, List.of())).as("no occurrence")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aFindingCarriesAtMostFiveValues() {
        assertThat(missingKey("says", 9, List.of("7", "9", "11", "13", "15")).samples()).as("five values").hasSize(5);
        assertThatThrownBy(() -> missingKey("says", 9, List.of("7", "9", "11", "13", "15", "17")))
                .as("six values").isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theValuesCannotBeChangedAfterwards() {
        List<String> given = new ArrayList<>(List.of("7"));
        Finding finding = missingKey("says", 1, given);
        given.add("9");
        assertThat(finding.samples()).as("the values after the list given changed").containsExactly("7");
    }

    @Test
    void aFindingUnderAbortStopsTheWork() {
        Finding finding = new Finding(RuleId.of("description/M017"), Severity.ERROR, Place.of("catalog", "Shop"),
                "The catalog Shop has two cubes named Returns.", Layer.DESCRIPTION, Consequence.ABORT, null, 1,
                List.of(), Instant.EPOCH);
        assertThat(finding.aborts()).as("abort").isTrue();
        assertThat(finding.source()).as("a structural finding is read from nowhere").isNull();
    }
}
