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
package org.eclipse.daanse.olap.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.daanse.olap.findings.Consequence;
import org.eclipse.daanse.olap.findings.RuleId;
import org.eclipse.daanse.olap.findings.Severity;
import org.junit.jupiter.api.Test;

/** The rules of the description: each once, of its two families, and each an error that stops the build. */
class DescriptionRulesTest {

    @Test
    void everyRuleHasAnIdOfItsOwn() {
        Set<RuleId> seen = new HashSet<>();
        assertThat(DescriptionRules.ALL).as("the rules whose id came before")
                .filteredOn(rule -> !seen.add(rule.id())).isEmpty();
    }

    @Test
    void everyRuleIsOfAFamilyTheDescriptionChecks() {
        assertThat(DescriptionRules.ALL).as("the families").extracting(rule -> rule.id().family())
                .allMatch(DescriptionRules.FAMILIES::contains);
    }

    @Test
    void everyRuleIsAnErrorThatStopsTheBuild() {
        assertThat(DescriptionRules.ALL).as("each rule and the one consequence it allows").allSatisfy(rule -> {
            assertThat(rule.severity()).as("the weight of %s", rule.id()).isEqualTo(Severity.ERROR);
            assertThat(rule.allowed()).as("the consequences of %s", rule.id()).containsExactly(Consequence.ABORT);
            assertThat(rule.byDefault()).as("the default of %s", rule.id()).isEqualTo(Consequence.ABORT);
        });
    }

    @Test
    void everyRuleSaysWhatItAsks() {
        assertThat(DescriptionRules.ALL).as("what the rules say").allSatisfy(
                rule -> assertThat(rule.says()).as("what %s says", rule.id()).isNotBlank().endsWith("."));
    }
}
