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
import static org.assertj.core.api.Assertions.tuple;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** A policy gives a rule the consequence of its narrowest entry that holds at a place, else the rule's default. */
class FindingPolicyTest {

    private static final Rule MISSING_KEY = new Rule(RuleId.of("data/D1"), Severity.WARNING,
            EnumSet.allOf(Consequence.class), Consequence.SUBSTITUTE, "Every fact key names a row of its dimension.");
    private static final Rule NULL_KEY = new Rule(RuleId.of("data/D2"), Severity.WARNING,
            EnumSet.allOf(Consequence.class), Consequence.SUBSTITUTE, "No fact key is NULL.");
    private static final Rule TWO_CUBES = Rule.of("description/M017", Severity.ERROR, Consequence.ABORT,
            "No two cubes of a catalog have one name.");

    private static final Place CATALOG = Place.of("catalog", "Shop");
    private static final Place SALES = CATALOG.then("cube", "Sales");
    private static final Place SHOP = SALES.then("measure group", "Sales").then("link", "Shop");
    private static final Place DAY = SALES.then("measure group", "Sales").then("link", "Day");
    private static final Place RETURNS_SHOP = CATALOG.then("cube", "Returns").then("measure group", "Returns")
            .then("link", "Shop");

    @Test
    void withoutAnEntryARuleTakesItsDefault() {
        assertThat(FindingPolicy.NONE.consequenceFor(MISSING_KEY, SHOP)).as("no entry")
                .isEqualTo(Consequence.SUBSTITUTE);
    }

    @Test
    void anEntryForTheWholeCatalogHoldsEverywhereForItsRuleOnly() {
        FindingPolicy policy = new FindingPolicy(List.of(new PolicyEntry(MISSING_KEY.id(), null, Consequence.DROP)));
        assertThat(policy.consequenceFor(MISSING_KEY, SHOP)).as("the rule of the entry").isEqualTo(Consequence.DROP);
        assertThat(policy.consequenceFor(NULL_KEY, SHOP)).as("another rule").isEqualTo(Consequence.SUBSTITUTE);
    }

    @Test
    void theNarrowestEntryThatHoldsDecides() {
        FindingPolicy policy = FindingPolicy.NONE.with(new PolicyEntry(MISSING_KEY.id(), null, Consequence.REPORT))
                .with(new PolicyEntry(MISSING_KEY.id(), SHOP, Consequence.DROP))
                .with(new PolicyEntry(MISSING_KEY.id(), SALES, Consequence.SUBSTITUTE));
        assertThat(policy.consequenceFor(MISSING_KEY, SHOP)).as("the link's entry").isEqualTo(Consequence.DROP);
        assertThat(policy.consequenceFor(MISSING_KEY, DAY)).as("another link of the cube")
                .isEqualTo(Consequence.SUBSTITUTE);
        assertThat(policy.consequenceFor(MISSING_KEY, RETURNS_SHOP)).as("a link of another cube")
                .isEqualTo(Consequence.REPORT);
    }

    @Test
    void anEntryTheRuleDoesNotAllowLeavesTheDefault() {
        FindingPolicy policy = new FindingPolicy(List.of(new PolicyEntry(TWO_CUBES.id(), null, Consequence.DROP)));
        assertThat(policy.consequenceFor(TWO_CUBES, CATALOG)).as("drop of a rule that only aborts")
                .isEqualTo(Consequence.ABORT);
    }

    @Test
    void anUnknownRuleADisallowedConsequenceAndASecondEntryAreProblems() {
        Map<RuleId, Rule> known = Map.of(MISSING_KEY.id(), MISSING_KEY, TWO_CUBES.id(), TWO_CUBES);
        PolicyEntry unknown = new PolicyEntry(RuleId.of("data/D9"), null, Consequence.DROP);
        PolicyEntry notAllowed = new PolicyEntry(TWO_CUBES.id(), null, Consequence.DROP);
        PolicyEntry first = new PolicyEntry(MISSING_KEY.id(), SHOP, Consequence.DROP);
        PolicyEntry second = new PolicyEntry(MISSING_KEY.id(), SHOP, Consequence.REPORT);
        PolicyEntry elsewhere = new PolicyEntry(MISSING_KEY.id(), SALES, Consequence.REPORT);
        FindingPolicy policy = new FindingPolicy(List.of(unknown, notAllowed, first, second, elsewhere));

        assertThat(policy.problems(id -> Optional.ofNullable(known.get(id))))
                .as("the problems of the policy, in the order written")
                .extracting(FindingPolicy.Problem::entry, FindingPolicy.Problem::kind)
                .containsExactly(tuple(unknown, FindingPolicy.Problem.Kind.UNKNOWN_RULE),
                        tuple(notAllowed, FindingPolicy.Problem.Kind.NOT_ALLOWED),
                        tuple(second, FindingPolicy.Problem.Kind.TWICE));
    }

    @Test
    void anEmptyPolicyHasNoProblem() {
        assertThat(FindingPolicy.NONE.problems(id -> Optional.empty())).as("no entry, no problem").isEmpty();
    }
}
