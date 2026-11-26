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
package org.eclipse.daanse.olap.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Measure;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.VirtualCube;
import org.eclipse.daanse.olap.query.component.NumericLiteralImpl;
import org.eclipse.daanse.olap.result.DrillThroughCubeChoice.Mode;
import org.junit.jupiter.api.Test;

/**
 * Which cube carries the rows behind a cell: the cube of its stored measure; none where two measures read
 * different facts; for a measure over no facts the cube asked of -- and in a virtual cube the one set of facts
 * it draws on, or, where it draws on several, what the mode says.
 */
class DrillThroughCubeChoiceTest {

    private final Cube sales = cube();
    private final Cube salesAgain = cube();
    private final Cube warehouse = cube();

    {
        // two cubes over the same table stand for the same facts
        lenient().when(sales.standsForTheSameFactsAs(salesAgain)).thenReturn(true);
        lenient().when(salesAgain.standsForTheSameFactsAs(sales)).thenReturn(true);
    }

    private final Measure amount = stored(sales);
    private final Measure amountAgain = stored(salesAgain);
    private final Measure stock = stored(warehouse);
    private final Measure one = constant();

    @Test
    void aStoredMeasureNamesItsCube() {
        assertThat(DrillThroughCubeChoice.cubeBehind(new Member[] { amount }, warehouse, Mode.NONE)).contains(sales);
    }

    @Test
    void twoMeasuresOverDifferentFactsNameNoRows() {
        assertThat(DrillThroughCubeChoice.cubeBehind(new Member[] { amount, stock }, sales, Mode.FIRST_FACT_SET))
                .isEmpty();
        assertThat(DrillThroughCubeChoice.cubeBehind(new Member[] { amount, amountAgain }, sales, Mode.NONE))
                .contains(sales);
    }

    @Test
    void aMeasureOverNoFactsLeavesTheCubeAskedOf() {
        assertThat(DrillThroughCubeChoice.cubeBehind(new Member[] { one }, warehouse, Mode.NONE)).contains(warehouse);
    }

    /** A virtual cube over cubes of the same facts answers with them, and ones over no facts count for none. */
    @Test
    void aVirtualCubeOverOneSetOfFactsAnswersWithIt() {
        VirtualCube virtual = virtual(amount, one, amountAgain);

        assertThat(DrillThroughCubeChoice.factSetsOf(virtual)).containsExactly(sales);
        assertThat(DrillThroughCubeChoice.cubeBehind(new Member[] { one }, virtual, Mode.NONE)).contains(sales);
    }

    /** Over several sets of facts the mode decides: none, or the one declared first. */
    @Test
    void aVirtualCubeOverSeveralSetsOfFactsAnswersAsTheModeSays() {
        VirtualCube virtual = virtual(stock, amount);

        assertThat(DrillThroughCubeChoice.factSetsOf(virtual)).containsExactly(warehouse, sales);
        assertThat(DrillThroughCubeChoice.cubeBehind(new Member[] { one }, virtual, Mode.NONE)).isEmpty();
        assertThat(DrillThroughCubeChoice.cubeBehind(new Member[] { one }, virtual, Mode.FIRST_FACT_SET))
                .contains(warehouse);
        // a stored measure still names its own cube, whatever the mode
        assertThat(DrillThroughCubeChoice.cubeBehind(new Member[] { amount }, virtual, Mode.NONE)).contains(sales);
    }

    @Test
    void aVirtualCubeOverNoFactsNamesNoRows() {
        assertThat(DrillThroughCubeChoice.cubeBehind(new Member[] { one }, virtual(one), Mode.FIRST_FACT_SET))
                .isEmpty();
    }

    @Test
    void aModeIsReadByNameAndAnyOtherNameIsNone() {
        assertThat(Mode.named("first_fact_set")).isEqualTo(Mode.FIRST_FACT_SET);
        assertThat(Mode.named(" FirstFactSet ")).isEqualTo(Mode.FIRST_FACT_SET);
        assertThat(Mode.named("first")).isEqualTo(Mode.FIRST_FACT_SET);
        assertThat(Mode.named("none")).isEqualTo(Mode.NONE);
        assertThat(Mode.named("something")).isEqualTo(Mode.NONE);
        assertThat(Mode.named(null)).isEqualTo(Mode.NONE);
    }

    private static Cube cube() {
        Cube cube = mock(Cube.class);
        lenient().when(cube.standsForTheSameFactsAs(cube)).thenReturn(true);
        return cube;
    }

    private static Measure stored(Cube facts) {
        Measure measure = mock(Measure.class);
        lenient().when(measure.standsFor()).thenReturn(measure);
        lenient().when(measure.factCube()).thenReturn(Optional.of(facts));
        return measure;
    }

    /** A measure a formula of a constant works out: it reads no facts. */
    private static Measure constant() {
        Measure measure = mock(Measure.class);
        lenient().when(measure.standsFor()).thenReturn(measure);
        lenient().when(measure.factCube()).thenReturn(Optional.empty());
        lenient().when(measure.isCalculated()).thenReturn(true);
        lenient().when(measure.getExpression()).thenReturn(NumericLiteralImpl.create(BigDecimal.ONE));
        return measure;
    }

    private static VirtualCube virtual(Member... measures) {
        VirtualCube virtual = mock(VirtualCube.class);
        doReturn(List.of(measures)).when(virtual).getMeasures();
        return virtual;
    }
}
