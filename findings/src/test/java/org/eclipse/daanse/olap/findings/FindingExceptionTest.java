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
import java.util.List;

import org.junit.jupiter.api.Test;

/** The work stops with every finding named, each with its place, message and rule. */
class FindingExceptionTest {

    private static Finding error(String rule, String place, String message) {
        return new Finding(RuleId.of(rule), Severity.ERROR, Place.parse(place), message, Layer.DESCRIPTION,
                Consequence.ABORT, null, 1, List.of(), Instant.EPOCH);
    }

    @Test
    void theExceptionCarriesAndNamesEveryFinding() {
        Finding noMeasure = error("description/M039", "catalog Shop / cube Returns", "has no measure");
        Finding twoCubes = error("description/M017", "catalog Shop", "has more than one cube named Returns");
        FindingException stopped = new FindingException(List.of(noMeasure, twoCubes));
        assertThat(stopped.findings()).as("the findings").containsExactly(noMeasure, twoCubes);
        assertThat(stopped.getMessage()).as("the message")
                .contains("2 finding(s)")
                .contains("catalog Shop / cube Returns: has no measure (description/M039)")
                .contains("catalog Shop: has more than one cube named Returns (description/M017)");
    }

    @Test
    void theWorkDoesNotStopWithoutAFinding() {
        assertThatThrownBy(() -> new FindingException(List.of())).as("no finding")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
