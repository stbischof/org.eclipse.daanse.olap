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
package org.eclipse.daanse.olap.metadata;

import static org.eclipse.daanse.olap.metadata.FindingAssert.assertThatModel;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * The model check names every broken drill-through action at once and with its path: two defaults, one
 * name twice, a return that is no level, level property or stored measure, a calculated measure, a null.
 */
class ActionsInTheModelCheckTest {

    private static final String SALES = "catalog Shop / cube Sales";

    @Test
    void everyBrokenActionIsNamedWithItsPath() {
        assertThatModel(() -> Olap.catalog("Shop").cube("Sales", c -> c
                .dimension("H", d -> d.hierarchy(h -> h.level("L", l -> l.property("P",
                        org.eclipse.daanse.olap.metadata.model.ValueKind.STRING))))
                .measure("M")
                .calculatedMember("C", "[Measures].[M] * 2")
                .drillThrough("one", true, "[H].[H].[L]", "[H].[H].[L].[P]", "[Measures].[M]")
                .drillThrough("two", true, "[H].[H].[L]")
                .drillThrough("d", false, "[H].[H].[L]")
                .drillThrough("D", false, "[H].[H].[L]")
                .drillThrough("nix", false, "[H].[H].[Nix]")
                .drillThrough("calc", false, "[Measures].[C]")
                .drillThrough("nulls", false, Arrays.asList("[H].[H].[L]", null).toArray(String[]::new)))
                .build())
                .fails()
                .withProblem(DescriptionRules.M048, SALES, "has 2 default drill-through actions")
                .withProblem(DescriptionRules.M028, SALES, "has more than one action named D")
                .withProblem(DescriptionRules.M052, SALES + " / action nix",
                        "returns [H].[H].[Nix], which is no level, level property or stored measure")
                .withProblem(DescriptionRules.M051, SALES + " / action calc",
                        "returns the calculated measure [Measures].[C]")
                .withProblem(DescriptionRules.M050, SALES + " / action nulls", "returns a null entry");
    }

    @Test
    void aLevelItsPropertyAndAStoredMeasureAreFineInAnySpelling() {
        assertThatModel(() -> Olap.catalog("Shop").cube("Sales", c -> c
                .dimension("H", d -> d.hierarchy(h -> h.level("L", l -> l.property("P",
                        org.eclipse.daanse.olap.metadata.model.ValueKind.STRING))))
                .measure("M")
                .drillThrough("ok", true, "[h].[H].[l]", "[H].[H].[L].[p]", "[measures].[m]"))
                .build())
                .isValid();
    }
}
