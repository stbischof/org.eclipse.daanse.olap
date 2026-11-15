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
package org.eclipse.daanse.olap.itests.common.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.olap.testkit.result.Expect.expect;
import static org.eclipse.daanse.olap.testkit.result.Expect.value;
import static org.eclipse.daanse.olap.testkit.result.ResultAssertions.assertThatMdx;

import org.eclipse.daanse.olap.provider.api.Olap;
import org.eclipse.daanse.olap.provider.api.model.CatalogDef;
import org.eclipse.daanse.olap.provider.junit.World;
import org.junit.jupiter.api.Test;

/**
 * A compiled expression says which hierarchies it depends on. Where the context asks for it, every such
 * claim is put to the test: the expression is worked out again with a hierarchy it says it does not need
 * moved to another member, and it must answer the same. A claim that does not hold stops the query.
 * <p>
 * Until now nothing armed that check. It was wired for one kind of catalog and read by a handful of tests
 * that only wanted to know whether it was on; the setting's default is off, so it never ran. These are the
 * first cases that turn it on, and they go red two ways: if an answer changes while the check moves the
 * context around, or if the check raises an alarm that does not hold.
 *
 * <pre>
 * Store            Amount
 * ├─ DE              30
 * │  ├─ Berlin       10
 * │  └─ Jena         20
 * └─ FR              40
 * Time: 2025, 2026
 * </pre>
 */
class ClaimedIndependenceIsPutToTheTestTest {

    static final CatalogDef SHOP = Olap.catalog("Shop")
            .cube("Sales", c -> c
                    .dimension("Store", d -> d.hierarchy(h -> h.level("Country").level("City")))
                    .dimension("Time", d -> d.hierarchy(h -> h.level("Year")))
                    .measure("Amount")
                    .calculatedMember("Double", "[Measures].[Amount] * 2")
                    .calculatedMember("Share", "[Measures].[Amount] / ([Measures].[Amount], [Store].[Store].[DE])"))
            .build();

    static World checking() {
        return World.of(SHOP, store -> store
                .fact("Sales").at("Store", "DE", "Berlin").at("Time", "2025").value("Amount", 10).add()
                .fact("Sales").at("Store", "DE", "Jena").at("Time", "2026").value("Amount", 20).add()
                .fact("Sales").at("Store", "FR", "Paris").at("Time", "2026").value("Amount", 40).add())
                .config("testExpDependencies", 3);
    }


    @Test
    void theEngineAnswersTheSameWhileEveryClaimIsBeingChecked() {
        try (World world = checking()) {
            assertThat(world.connection().getContext().getConfig().testExpDependencies()).isEqualTo(3);
            assertThatMdx(world.run("SELECT {[Measures].[Amount], [Measures].[Double]} ON 0,"
                    + " [Store].[Store].[Country].Members ON 1 FROM [Sales]"))
                    .matches(expect().at("DE", "Amount", value(30)).at("DE", "Double", value(60))
                            .at("FR", "Amount", value(40)).at("FR", "Double", value(80)));
        }
    }

    @Test
    void aFormulaThatReachesForAnotherMemberIsCheckedToo() {
        try (World world = checking()) {
            assertThatMdx(world.run("SELECT {[Measures].[Share]} ON 0,"
                    + " [Store].[Store].[Country].Members ON 1 FROM [Sales]"))
                    .matches(expect().at("DE", "Share", value(1)).at("FR", "Share", value(40.0 / 30.0)));
        }
    }

    @Test
    void aQueryOverTwoHierarchiesIsCheckedInBoth() {
        try (World world = checking()) {
            assertThatMdx(world.run("SELECT {[Measures].[Amount]} ON 0,"
                    + " CrossJoin([Store].[Store].[Country].Members, [Time].[Time].[Year].Members) ON 1"
                    + " FROM [Sales]"))
                    .rows().hasSize(4);
        }
    }
}
