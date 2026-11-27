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

import java.util.Objects;
import java.util.Optional;

/**
 * One entry of a catalog's policy: the consequence a rule takes within a scope.
 *
 * @param rule        the rule
 * @param scope       the element below which the entry holds, or {@code null} for the whole catalog
 * @param consequence the consequence a finding of the rule takes there
 */
public record PolicyEntry(RuleId rule, Place scope, Consequence consequence) {

    public PolicyEntry {
        Objects.requireNonNull(rule, "the rule of a policy entry");
        Objects.requireNonNull(consequence, "the consequence of a policy entry for " + rule);
    }

    /** The scope; empty for the whole catalog. */
    public Optional<Place> scopeIfAny() {
        return Optional.ofNullable(scope);
    }

    /** Whether the entry holds at the place. */
    public boolean holdsAt(Place place) {
        return scope == null || place.within(scope);
    }

    /** How deep the scope lies: none for the whole catalog, else its number of steps. */
    int depth() {
        return scope == null ? 0 : scope.steps().size();
    }
}
