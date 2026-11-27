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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The consequences a catalog gives its rules: per rule, for the whole catalog or below an element of it. The catalog
 * carries its policy, so two catalogs that differ in their policy are two catalogs.
 *
 * @param entries the entries, in the order written
 */
public record FindingPolicy(List<PolicyEntry> entries) {

    /** No entry: every rule takes its default. */
    public static final FindingPolicy NONE = new FindingPolicy(List.of());

    public FindingPolicy {
        entries = List.copyOf(Objects.requireNonNull(entries, "the entries of a policy"));
    }

    /**
     * What is wrong with an entry of a policy.
     *
     * @param entry the entry
     * @param kind  what is wrong with it
     */
    public record Problem(PolicyEntry entry, Kind kind) {

        /** What is wrong with an entry. */
        public enum Kind {
            /** The entry names a rule that no check knows. */
            UNKNOWN_RULE,
            /** The entry gives its rule a consequence the rule does not allow. */
            NOT_ALLOWED,
            /** A second entry for one rule and one scope. */
            TWICE
        }
    }

    /** The policy with one more entry. */
    public FindingPolicy with(PolicyEntry entry) {
        List<PolicyEntry> more = new ArrayList<>(entries);
        more.add(entry);
        return new FindingPolicy(more);
    }

    /**
     * The consequence of a finding of the rule at the place: the entry for the rule with the narrowest scope that
     * holds the place, where the rule allows its consequence; else the rule's default.
     */
    public Consequence consequenceFor(Rule rule, Place place) {
        PolicyEntry chosen = null;
        for (PolicyEntry entry : entries) {
            if (entry.rule().equals(rule.id()) && entry.holdsAt(place) && rule.allows(entry.consequence())
                    && (chosen == null || entry.depth() > chosen.depth())) {
                chosen = entry;
            }
        }
        return chosen == null ? rule.byDefault() : chosen.consequence();
    }

    /**
     * The entries that name a rule the function does not know, give a consequence their rule does not allow, or come a
     * second time for one rule and one scope, in the order written.
     */
    public List<Problem> problems(Function<RuleId, Optional<Rule>> known) {
        List<Problem> problems = new ArrayList<>();
        Set<List<Object>> seen = new HashSet<>();
        for (PolicyEntry entry : entries) {
            Optional<Rule> rule = known.apply(entry.rule());
            if (rule.isEmpty()) {
                problems.add(new Problem(entry, Problem.Kind.UNKNOWN_RULE));
            } else if (!rule.get().allows(entry.consequence())) {
                problems.add(new Problem(entry, Problem.Kind.NOT_ALLOWED));
            }
            if (!seen.add(java.util.Arrays.asList(entry.rule(), entry.scope()))) {
                problems.add(new Problem(entry, Problem.Kind.TWICE));
            }
        }
        return problems;
    }
}
