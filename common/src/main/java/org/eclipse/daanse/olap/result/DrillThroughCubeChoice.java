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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.Measure;
import org.eclipse.daanse.olap.api.element.VirtualCube;

/**
 * Which cube carries the rows behind a cell.
 *
 * <p>A cell's coordinates name the rows; the cube says where they live. Usually the cell's measure
 * answers it, because a stored measure belongs to the cube whose facts it reads. Three cases need a rule:
 *
 * <ul>
 * <li>Two stored measures from cubes over different facts: no rows. There is no single set of rows that
 * would answer both.</li>
 * <li>A cell whose measure names no facts at all -- a constant, or an expression over no measure -- in a
 * cube of its own: that cube. The measure does not narrow the facts, so the other coordinates alone say
 * which rows are meant, which is the point of offering a drill-through for such a cell.</li>
 * <li>The same in a <em>virtual</em> cube: the cubes it draws on answer, if they stand for the same facts.
 * Where they do not, the answer depends on {@link Mode}.</li>
 * </ul>
 */
public final class DrillThroughCubeChoice {

    /** Says which way a decision went, for the question "why is no drill-through offered here?". */
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(DrillThroughCubeChoice.class);

    /** What to answer where a virtual cube draws on more than one set of facts. */
    public enum Mode {

        /** No rows: an arbitrary set is worse than none. */
        NONE,

        /**
         * The set declared first, which the older line of this engine answered. The order of the measures
         * then decides which rows a client is shown, so a catalog is warned where this takes effect.
         */
        FIRST_FACT_SET;

        /** The mode of this name, {@link #NONE} for a name that is none of them. */
        public static Mode named(String name) {
            if (name == null) {
                return NONE;
            }
            return switch (name.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "FIRST_FACT_SET", "FIRSTFACTSET", "FIRST" -> FIRST_FACT_SET;
            default -> NONE;
            };
        }
    }

    private DrillThroughCubeChoice() {
    }

    /**
     * The cube whose rows lie behind these coordinates, empty when none does.
     *
     * @param coordinates the members of the cell, its measure among them
     * @param queried     the cube the statement was asked of
     * @param mode        what to answer where a virtual cube draws on several sets of facts
     */
    public static Optional<Cube> cubeBehind(Member[] coordinates, Cube queried, Mode mode) {
        Visitor visitor = new Visitor();
        if (!visitor.accepts(coordinates)) {
            return Optional.empty();
        }
        if (visitor.cube != null) {
            return Optional.of(visitor.cube);
        }
        // No measure named a cube: the coordinates alone say which rows are meant, so the cube asked of
        // answers -- unless it is virtual and has no single set of facts of its own.
        if (queried instanceof VirtualCube) {
            return oneFactSetOf(queried, mode);
        }
        return Optional.ofNullable(queried);
    }

    /**
     * The stored measures the members of a cell lead to, in the order they are met: a stored measure
     * itself, and the ones a formula reads where the formula does no more than combine them with
     * arithmetic, {@code CoalesceEmpty} and constants. Empty where the formula does more, which is the
     * same reading {@link #cubeBehind(Member[], Cube, Mode)} applies.
     *
     * <p>Rows behind a cell are the facts of a cube restricted by the cell's coordinates, so any of these
     * measures addresses the same rows. A source that needs a stored measure to ask for them can take the
     * first.
     *
     * @param coordinates the members of the cell, its measure among them
     */
    public static List<Member> storedMeasuresBehind(Member[] coordinates) {
        Visitor visitor = new Visitor();
        return visitor.accepts(coordinates) ? List.copyOf(visitor.measures) : List.of();
    }

    /**
     * The one set of facts a virtual cube draws on, by the cubes of its stored measures. Cubes that stand
     * for the same facts count once.
     */
    public static List<Cube> factSetsOf(Cube virtual) {
        List<Cube> distinct = new ArrayList<>();
        for (Member measure : measuresOf(virtual)) {
            Optional<Cube> facts = measure.standsFor() instanceof Measure named ? named.factCube()
                    : Optional.empty();
            if (facts.isPresent() && distinct.stream().noneMatch(seen -> seen.standsForTheSameFactsAs(facts.get()))) {
                distinct.add(facts.get());
            }
        }
        return distinct;
    }

    private static Optional<Cube> oneFactSetOf(Cube virtual, Mode mode) {
        List<Cube> factSets = factSetsOf(virtual);
        if (factSets.size() == 1) {
            return Optional.of(factSets.getFirst());
        }
        if (factSets.isEmpty()) {
            LOGGER.debug("no rows behind a cell of cube {}: it draws on no facts of its own", virtual.getName());
            return Optional.empty();
        }
        if (mode == Mode.FIRST_FACT_SET) {
            LOGGER.debug("a cell of cube {} names no fact, so its rows are read from {}, declared first of {}",
                    virtual.getName(), factSets.getFirst().getName(), factSets.size());
            return Optional.of(factSets.getFirst());
        }
        LOGGER.debug("no rows behind a cell of cube {} that names no fact: it draws on {} fact sets",
                virtual.getName(), factSets.size());
        return Optional.empty();
    }

    private static Iterable<Member> measuresOf(Cube cube) {
        Set<Member> measures = new LinkedHashSet<>();
        if (cube.getMeasures() != null) {
            measures.addAll(cube.getMeasures());
        }
        return measures;
    }

    /**
     * What a member means when the rows behind a cell are looked for: a stored measure names the cube the
     * rows live in, a member standing for another is followed to it, and a calculated member is followed
     * into its formula by the base.
     */
    private static final class Visitor extends DrillThroughExpressionCheck {

        private Cube cube;
        private final List<Member> measures = new ArrayList<>();

        @Override
        protected void handleMember(Member member) {
            if (member.getHierarchy() != null && member.getHierarchy().isScenario()) {
                // The stand a cell is read under is no coordinate of it: it says which values are in force,
                // and its member is worked out by a formula for exactly that reason. Following it would make
                // every cell of a catalog that takes writes unreachable.
                return;
            }
            Member stands = member.standsFor();
            if (stands != member) {
                handleMember(stands);
                return;
            }
            if (member instanceof Measure measure) {
                Optional<Cube> facts = measure.factCube();
                if (facts.isEmpty()) {
                    // a measure over no facts names no cube of its own; its formula may name one
                    super.handleMember(member);
                    return;
                }
                if (cube == null) {
                    cube = facts.get();
                } else if (!cube.standsForTheSameFactsAs(facts.get())) {
                    // two measures over different facts: no single set of rows answers both
                    throw tooComplex();
                }
                measures.add(member);
                return;
            }
            // Any other member names a coordinate, not a set of facts, and a formula of its own says
            // nothing about where the rows live -- Aggregate({a, b}) on an axis leaves the cube as clear
            // as a plain member does. Whether such a formula makes the cell itself undrillable is asked
            // once, by the cell, and a count is answered even where it is: the constraint is dropped
            // rather than the question.
        }
    }
}
