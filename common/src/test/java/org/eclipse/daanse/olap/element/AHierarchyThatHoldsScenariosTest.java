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
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.element;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.junit.jupiter.api.Test;

/**
 * Which hierarchy a write chooses its scenario from is a question the hierarchy answers now, instead of
 * a name the engine compares. What the default answers is what every catalog has answered so far, down
 * to the spelling; a catalog that calls its writing dimension something else overrides it.
 */
class AHierarchyThatHoldsScenariosTest {

    @Test
    void theOneNamedScenarioHoldsThem() {
        assertThat(named("Scenario").isScenario()).isTrue();
    }

    @Test
    void anotherNameDoesNotByItself() {
        assertThat(named("Versions").isScenario()).isFalse();
    }

    @Test
    void andTheSpellingCounts() {
        assertThat(named("scenario").isScenario()).isFalse();
    }

    private static Hierarchy named(String name) {
        Hierarchy hierarchy = mock(Hierarchy.class, CALLS_REAL_METHODS);
        when(hierarchy.getName()).thenReturn(name);
        return hierarchy;
    }
}
