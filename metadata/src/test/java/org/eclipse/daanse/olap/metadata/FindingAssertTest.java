/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.metadata;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.daanse.olap.metadata.FindingAssert.assertThatModel;

import org.eclipse.daanse.olap.metadata.model.CatalogDef;
import org.junit.jupiter.api.Test;

/**
 * The assertion reads a finding by its rule, place, weight and message, and names every finding when one is missing.
 */
class FindingAssertTest {

    private static CatalogDef aCatalogWithAMissingDefaultMeasure() {
        return Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City"))).measure("Amount")
                        .defaultMeasure("Missing"))
                .build();
    }

    private static CatalogDef aCatalogThatIsFine() {
        return Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Geo", d -> d.hierarchy(h -> h.level("City"))).measure("Amount"))
                .build();
    }

    @Test
    void aDescriptionThatTheBuilderRefusesIsReadWithoutCatchingAnything() {
        assertThatCode(() -> assertThatModel(FindingAssertTest::aCatalogWithAMissingDefaultMeasure).fails()
                .withProblemCount(1)
                .withProblem(DescriptionRules.O4, "catalog Shop / cube Sales",
                        "default measure Missing is not a measure of the cube")
                .withAMessageNamingEveryProblem()).doesNotThrowAnyException();
    }

    @Test
    void aDescriptionThatIsFineIsReadTheSameWay() {
        assertThatCode(() -> assertThatModel(aCatalogThatIsFine()).isValid()).doesNotThrowAnyException();
    }

    @Test
    void theAssertNamesEveryProblemItFoundWhenTheExpectedOneIsMissing() {
        assertThatThrownBy(() -> assertThatModel(FindingAssertTest::aCatalogWithAMissingDefaultMeasure)
                .withProblem(DescriptionRules.M039, "catalog Shop / cube Sales", "has no measure"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("expected: one problem description/M039 at 'catalog Shop / cube Sales' saying "
                        + "'has no measure'")
                .hasMessageContaining("but was: no such problem")
                .hasMessageContaining("rule").hasMessageContaining("place").hasMessageContaining("message")
                .hasMessageContaining("olap/O4").hasMessageContaining("default measure Missing");
    }

    @Test
    void theRuleOfAProblemCountsAsMuchAsItsPlaceAndMessage() {
        assertThatThrownBy(() -> assertThatModel(FindingAssertTest::aCatalogWithAMissingDefaultMeasure)
                .withProblem(DescriptionRules.M039, "catalog Shop / cube Sales",
                        "default measure Missing is not a measure of the cube"))
                .as("the place and the message of olap/O4 under another rule").isInstanceOf(AssertionError.class)
                .hasMessageContaining("but was: no such problem");
    }

    @Test
    void theAssertSaysSoWhenADescriptionItExpectedToFailIsAccepted() {
        assertThatThrownBy(() -> assertThatModel(aCatalogThatIsFine()).fails())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("expected: at least one problem")
                .hasMessageContaining("but was: a description the validator accepts")
                .hasMessageContaining("the validator found nothing to blame");
    }

    @Test
    void theAssertNamesBothCountsWhenTheyDiffer() {
        assertThatThrownBy(() -> assertThatModel(FindingAssertTest::aCatalogWithAMissingDefaultMeasure)
                .withProblemCount(3)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("expected: 3 problem(s)").hasMessageContaining("but was: 1 problem(s)");
    }

    @Test
    void theAssertSaysSoWhenADescriptionItExpectedToBeFineIsBlamed() {
        assertThatThrownBy(() -> assertThatModel(FindingAssertTest::aCatalogWithAMissingDefaultMeasure).isValid())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("expected: a description without a single problem")
                .hasMessageContaining("but was: 1 problem(s)");
    }
}
