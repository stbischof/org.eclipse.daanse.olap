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

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** A rule id is a family and a code, written family/code; a code alone does not tell two families apart. */
class RuleIdTest {

    @Test
    void anIdIsReadFromItsTextAndWrittenBack() {
        RuleId id = RuleId.of("data/D1");
        assertThat(id.family()).as("the family").isEqualTo("data");
        assertThat(id.code()).as("the code").isEqualTo("D1");
        assertThat(id).as("the id read equals the id made").isEqualTo(new RuleId("data", "D1"));
        assertThat(id.toString()).as("the id written").isEqualTo("data/D1");
    }

    @ParameterizedTest
    @ValueSource(strings = { "olap/O14", "mapping/R9", "description/M017", "description/Mpolicy1", "knowledge/K10",
            "rolapmapping/X001", "cwmconnector/Y003" })
    void theIdsOfTheRuleTableReadBack(String text) {
        assertThat(RuleId.of(text).toString()).as("the id %s read and written", text).isEqualTo(text);
    }

    @Test
    void oneCodeInTwoFamiliesIsTwoRules() {
        assertThat(Set.of(RuleId.of("olap/O1"), RuleId.of("obligations/O1"))).as("two families, one code")
                .hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = { "D1", "data/", "/D1", "", "data/D1/x", "Data/D1", "data/D 1", "1data/D1" })
    void aTextThatIsNoFamilyAndCodeIsRefused(String text) {
        assertThatThrownBy(() -> RuleId.of(text)).as("the text '%s'", text)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noTextIsRefused() {
        assertThatThrownBy(() -> RuleId.of(null)).as("a null text").isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rule id");
    }
}
