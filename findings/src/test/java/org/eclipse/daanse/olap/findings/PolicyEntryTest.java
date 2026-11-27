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

import org.junit.jupiter.api.Test;

/** An entry of a policy holds below its scope, or everywhere without one. */
class PolicyEntryTest {

    private static final Place CUBE = Place.parse("catalog Shop / cube Sales");

    @Test
    void anEntryWithoutAScopeHoldsInTheWholeCatalog() {
        PolicyEntry entry = new PolicyEntry(RuleId.of("data/D1"), null, Consequence.DROP);
        assertThat(entry.holdsAt(Place.parse("catalog Shop / cube Returns"))).as("anywhere").isTrue();
        assertThat(entry.scopeIfAny()).as("the scope").isEmpty();
    }

    @Test
    void anEntryWithAScopeHoldsOnlyBelowIt() {
        PolicyEntry entry = new PolicyEntry(RuleId.of("data/D1"), CUBE, Consequence.DROP);
        assertThat(entry.holdsAt(CUBE.then("measure group", "Sales").then("link", "Shop"))).as("below the cube")
                .isTrue();
        assertThat(entry.holdsAt(Place.parse("catalog Shop / cube Returns"))).as("in another cube").isFalse();
        assertThat(entry.scopeIfAny()).as("the scope").contains(CUBE);
    }
}
