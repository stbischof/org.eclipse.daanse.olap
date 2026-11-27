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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** A place is a path of steps from the catalog down; its text form reads back whatever the names hold. */
class PlaceTest {

    private static final Place LINK = Place.of("catalog", "Shop").then("cube", "Sales").then("measure group", "Sales")
            .then("link", "Shop");

    @Test
    void aPlaceIsWrittenStepByStep() {
        assertThat(LINK.text()).as("the text").isEqualTo("catalog Shop / cube Sales / measure group Sales / link Shop");
        assertThat(LINK.steps()).as("the steps").hasSize(4);
        assertThat(LINK.last()).as("the element").isEqualTo(new Place.Step("link", "Shop"));
    }

    @Test
    void theTextReadsBackAsThePlace() {
        assertThat(Place.parse(LINK.text())).as("the place read from its text").isEqualTo(LINK);
    }

    @Test
    void theParentIsOneStepUpAndTheCatalogHasNone() {
        assertThat(LINK.parent()).as("the parent of the link")
                .contains(Place.of("catalog", "Shop").then("cube", "Sales").then("measure group", "Sales"));
        assertThat(Place.of("catalog", "Shop").parent()).as("the parent of the catalog").isEmpty();
    }

    @Test
    void aPlaceLiesWithinItselfAndWithinEveryPlaceAboveIt() {
        assertThat(LINK.within(LINK)).as("within itself").isTrue();
        assertThat(LINK.within(Place.of("catalog", "Shop").then("cube", "Sales"))).as("within its cube").isTrue();
        assertThat(LINK.within(Place.of("catalog", "Shop"))).as("within its catalog").isTrue();
    }

    @Test
    void aPlaceDoesNotLieWithinAnotherBranchNorBelowItself() {
        assertThat(LINK.within(Place.of("catalog", "Shop").then("cube", "Returns"))).as("another cube").isFalse();
        assertThat(LINK.within(Place.of("catalog", "Shop2"))).as("another catalog").isFalse();
        assertThat(Place.of("catalog", "Shop").within(LINK)).as("a place below").isFalse();
    }

    @Test
    void aStepOfTheWayIsFound() {
        assertThat(LINK.passes("measure group")).as("a measure group on the way").isTrue();
        assertThat(LINK.passes("level")).as("a level on the way").isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "A / B", "A/B", "back\\slash", "trailing ", " leading", "", "dots.and]brackets",
            "ends in /", "set Amount" })
    void aNameHoldingAnythingReadsBack(String name) {
        Place place = Place.of("catalog", "Shop").then("named set", name).then("cube", name).then("named", name);
        assertThat(Place.parse(place.text())).as("the place of the name '%s' read from %s", name, place.text())
                .isEqualTo(place);
    }

    @Test
    void aKindOfSeveralWordsTheTextDoesNotKnowReadsBack() {
        Place place = Place.of("catalog", "Shop").then("data source view", "Main");
        assertThat(Place.parse(place.text())).as("an unknown kind of three words").isEqualTo(place);
    }

    @Test
    void theKindsOfTheDescriptionsChecksReadBack() {
        String text = "catalog Shop / cube Sales / dimension Geo / hierarchy Geo / level City / member property"
                + " Population";
        Place place = Place.parse(text);
        assertThat(place.last()).as("the last step").isEqualTo(new Place.Step("member property", "Population"));
        assertThat(place.text()).as("written again").isEqualTo(text);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "Shop", "catalog", "catalog Shop / cube" })
    void aTextWithoutAKindAndANameIsRefused(String text) {
        assertThatThrownBy(() -> Place.parse(text)).as("the text '%s'", text)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anElementWithoutANameHasTheEmptyName() {
        assertThat(Place.of("catalog", "Shop").then("hierarchy", null).last()).as("a step without a name")
                .isEqualTo(new Place.Step("hierarchy", ""));
    }

    @Test
    void aStepWithoutAKindIsRefused() {
        assertThatThrownBy(() -> Place.of(" ", "Shop")).as("a blank kind").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Place.of("a/b", "Shop")).as("a slash in the kind")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Place(List.of())).as("no step").isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPlaceDoesNotChangeWhenItIsExtended() {
        Place catalog = Place.of("catalog", "Shop");
        catalog.then("cube", "Sales");
        assertThat(catalog.steps()).as("the steps of the place extended").hasSize(1);
        assertThatThrownBy(() -> catalog.steps().add(new Place.Step("cube", "Sales"))).as("the steps held")
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
