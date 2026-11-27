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

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** A rule names the consequences a policy may give it and defaults to one of them. */
class RuleTest {

    @Test
    void aRuleAllowsWhatItSaysAndDefaultsToOneOfIt() {
        Rule missingKey = new Rule(RuleId.of("data/D1"), Severity.WARNING, EnumSet.allOf(Consequence.class),
                Consequence.SUBSTITUTE, "Every fact key names a row of its dimension.");
        assertThat(missingKey.allows(Consequence.DROP)).as("drop is allowed").isTrue();
        assertThat(missingKey.byDefault()).as("the default").isEqualTo(Consequence.SUBSTITUTE);
    }

    @Test
    void aRuleOfOneConsequenceAllowsOnlyThat() {
        Rule twoCubes = Rule.of("description/M017", Severity.ERROR, Consequence.ABORT,
                "No two cubes of a catalog have one name.");
        assertThat(twoCubes.allowed()).as("the allowed consequences").containsExactly(Consequence.ABORT);
        assertThat(twoCubes.allows(Consequence.DROP)).as("drop").isFalse();
    }

    @Test
    void aRuleThatDefaultsToWhatItDoesNotAllowIsRefused() {
        assertThatThrownBy(() -> new Rule(RuleId.of("assl/A002"), Severity.WARNING,
                EnumSet.of(Consequence.DROP, Consequence.ABORT), Consequence.REPORT, "says"))
                .as("a default outside the allowed").isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assl/A002");
    }

    @Test
    void aRuleThatAllowsNothingIsRefused() {
        assertThatThrownBy(() -> new Rule(RuleId.of("assl/A002"), Severity.WARNING, Set.of(), Consequence.DROP,
                "says")).as("no consequence").isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theAllowedConsequencesCannotBeChangedAfterwards() {
        EnumSet<Consequence> given = EnumSet.of(Consequence.DROP);
        Rule rule = new Rule(RuleId.of("assl/A001"), Severity.WARNING, given, Consequence.DROP, "says");
        given.add(Consequence.ABORT);
        assertThat(rule.allows(Consequence.ABORT)).as("a change of the set given after").isFalse();
        assertThatThrownBy(() -> rule.allowed().add(Consequence.ABORT)).as("a change of the set held")
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
