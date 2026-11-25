/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.api.element;

import java.util.Optional;
import java.util.Set;

public interface Measure extends Member {

    /**
     * Which of the dimensions this tuple names the measure does not reach.
     * <p>
     * Asked of the measure and not of a cube, because what a measure reaches is a property of the measure:
     * a catalog that groups its measures some other way than by cube can answer it too. A measure that
     * reaches everything - the usual case - answers with nothing.
     */
    default Set<Dimension> dimensionsNotReached(Member[] tuple) {
        return Set.of();
    }

    /**
     * The member that counts the facts behind this measure, where the catalog keeps one. A cell of such a
     * measure is empty when nothing was counted, even if the measure itself works out to zero.
     */
    default Optional<Member> factCount() {
        return Optional.empty();
    }

    /**
     * The level of this measure's own dimensionality that answers for {@code level}, or empty when the
     * measure does not reach it at all. A measure whose dimensionality is the one asked about answers with
     * the level itself.
     */
    default Optional<Level> levelReached(Level level) {
        return Optional.of(level);
    }

    /**
     * Whether this measure adds its cells up, so that a value written to a cell above can be spread over
     * the cells beneath it. A measure that counts distinct things or takes a minimum cannot be spread, and
     * a client is told so rather than being allowed to write nonsense.
     */
    default boolean addsUp() {
        return false;
    }

    /**
     * The name of the measure group this measure belongs to.
     *
     * <p>A measure group is the granularity its measures share, and with it the facts they are read from.
     * A metadata document names it for every measure, and a cube with several groups is what used to be
     * a cube over several fact tables.
     *
     * <p>Nothing means the cube is the only group there is, which is what a catalog that does not speak
     * of groups says.
     */
    default Optional<String> measureGroup() {
        return Optional.empty();
    }

    /**
     * Whether a value written into a cell of this measure has somewhere to go.
     *
     * <p>Writing belongs to the measure group: the value is kept beside the facts of that group, every
     * measure of it has to add up for a difference to mean anything, and a catalog may keep somewhere to
     * put the values of one group and not of another. The default says yes, and the cube is still asked
     * as well -- a catalog with nowhere to put anything says so once, at the cube.
     */
    default boolean takesWrites() {
        return true;
    }

    /**
     * Whether this measure varies by that dimension at all.
     *
     * <p>What a measure varies by is the dimensions of its measure group: a group is read from one set of
     * facts, and those facts carry a coordinate for some dimensions and none for the others. A measure
     * that varies by everything -- the usual case, and what a cube with a single group means -- says yes
     * to every dimension.
     */
    default boolean varies(Dimension dimension) {
        return true;
    }

    /**
     * The level of that dimension the facts behind this measure reach, where the catalog names one.
     *
     * <p>Above that level the facts are already summed up; below it there is nothing that was measured.
     * Nothing means as finely as the dimension goes, which is the answer for every measure whose catalog
     * does not speak of granularity.
     */
    default Optional<Level> granularityIn(Dimension dimension) {
        return Optional.empty();
    }
}
