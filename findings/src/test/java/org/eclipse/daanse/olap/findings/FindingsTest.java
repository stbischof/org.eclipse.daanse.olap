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

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

/** A check records what it found under the policy and acts on the consequence it is given back. */
class FindingsTest {

    private static final Rule MISSING_KEY = new Rule(RuleId.of("data/D1"), Severity.WARNING,
            EnumSet.allOf(Consequence.class), Consequence.SUBSTITUTE, "Every fact key names a row of its dimension.");
    private static final Rule TWO_CUBES = Rule.of("description/M017", Severity.ERROR, Consequence.ABORT,
            "No two cubes of a catalog have one name.");
    private static final Place LINK = Place.parse("catalog Shop / cube Sales / measure group Sales / link Shop");
    private static final Place COLUMN = Place.parse("table sales / column shop_id");
    private static final String MESSAGE = "A sale names the shop 7, which is no row of the shops.";

    private static FindingPolicy at(Consequence consequence) {
        return new FindingPolicy(List.of(new PolicyEntry(MISSING_KEY.id(), LINK, consequence)));
    }

    @Test
    void thePolicysConsequenceIsTakenAndUnderAbortTheFindingIsAnError() {
        for (Consequence consequence : List.of(Consequence.DROP, Consequence.REPORT, Consequence.ABORT)) {
            Finding finding = Findings.of(at(consequence), MISSING_KEY, Layer.DATA, LINK, MESSAGE);
            assertThat(finding.consequence()).as("consequence under %s", consequence).isEqualTo(consequence);
            assertThat(finding.severity()).as("weight under %s", consequence)
                    .isEqualTo(consequence == Consequence.ABORT ? Severity.ERROR : Severity.WARNING);
        }
    }

    @Test
    void aStructuralFindingOccursOnceAndIsReadFromNowhere() {
        Finding finding = Findings.of(FindingPolicy.NONE, TWO_CUBES, Layer.DESCRIPTION, Place.of("catalog", "Shop"),
                "has more than one cube named Returns");
        assertThat(finding.severity()).as("severity").isEqualTo(Severity.ERROR);
        assertThat(finding.consequence()).as("consequence").isEqualTo(Consequence.ABORT);
        assertThat(finding.source()).as("source").isNull();
        assertThat(finding.occurrences()).as("occurrences").isEqualTo(1);
        assertThat(finding.samples()).as("samples").isEmpty();
    }

    @Test
    void ofMoreThanFiveValuesTheFirstFiveAreKept() {
        Finding finding = Findings.of(FindingPolicy.NONE, MISSING_KEY, Layer.DATA, LINK, MESSAGE, COLUMN, 6,
                List.of("7", "9", "11", "13", "15", "17"));
        assertThat(finding.samples()).as("the values kept").containsExactly("7", "9", "11", "13", "15");
        assertThat(finding.occurrences()).as("the count stays").isEqualTo(6);
    }
}
