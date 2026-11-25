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
package org.eclipse.daanse.olap.writeback;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.result.AllocationPolicy;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.olap.api.result.WritebackCell;
import org.eclipse.daanse.olap.api.type.ScalarType;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;

/**
 * A stand in which a client tries values out. What it holds is not written anywhere: the values live here
 * and are applied while a cell is evaluated, so the data stays as it was loaded and a client without this
 * stand keeps seeing what the source says. Making a stand permanent is a separate step, and each art does
 * it its own way -- one writes rows to a table, another hands them to its sink.
 *
 * <p>Two kinds of held state live here and they behave differently. The cells recorded through
 * {@link #record} are what-if only and are never made permanent. The rows added through
 * {@link #addPendingRows} are what a commit writes, and they belong to the cube they were produced for --
 * handing a row to another cube would write values that were never meant for it.
 *
 * <p>What a subclass adds is how a write is turned into a recorded cell: that needs to know where a cell's
 * value comes from, which is the one thing an art does not share.
 */
public abstract class ScenarioBase implements Scenario {

    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final int id = NEXT_ID.getAndIncrement();

    /** Copy-on-write: a write appends while an evaluation walks the list. */
    private final List<WritebackCell> writebackCells = new CopyOnWriteArrayList<>();

    private final Map<Cube, List<Map<String, Map.Entry<DataTypeJdbc, Object>>>> pending =
            Collections.synchronizedMap(new LinkedHashMap<>());

    private Member member;

    /**
     * The member that stands for this stand in each scenario hierarchy: an art publishes one member per cube that has
     * a scenario hierarchy, and a cube is read under the one of its own hierarchy -- the member last published belongs
     * to another cube, at another place in it (identity: each cube's hierarchy is its own).
     */
    private final Map<org.eclipse.daanse.olap.api.element.Hierarchy, Member> memberByHierarchy =
            Collections.synchronizedMap(new java.util.IdentityHashMap<>());

    private String cubeName;

    @Override
    public String getId() {
        return Integer.toString(id);
    }

    @Override
    public List<WritebackCell> getWritebackCells() {
        return writebackCells;
    }

    /** Remembers a cell a client wrote. Its value is applied from now on, wherever it reaches. */
    protected void record(WritebackCell cell) {
        writebackCells.add(cell);
    }

    /** The member that stands for this stand, so that a query can name it. */
    public Member getMember() {
        return member;
    }

    /** The member that stands for this stand in the given scenario hierarchy; {@code null} when none was published. */
    public Member getMember(org.eclipse.daanse.olap.api.element.Hierarchy hierarchy) {
        return memberByHierarchy.get(hierarchy);
    }

    /** Names the member that stands for this stand; the art that publishes it says which. */
    protected void member(Member value) {
        this.member = value;
        if (value != null) {
            memberByHierarchy.put(value.getHierarchy(), value);
        }
    }

    public String getCubeName() {
        return cubeName;
    }

    public void setCubeName(String cubeName) {
        this.cubeName = cubeName;
    }

    @Override
    public List<Map<String, Map.Entry<DataTypeJdbc, Object>>> pendingRows(Cube cube) {
        List<Map<String, Map.Entry<DataTypeJdbc, Object>>> rows = pending.get(cube);
        return rows == null ? List.of() : rows;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Nothing added leaves the cube unmentioned: a caller that produced no rows for a cube has not made
     * that cube pending, and a commit that walked it would write nothing while looking as if it had work.
     * Handing over nothing at all counts as nothing added.
     */
    @Override
    public void addPendingRows(Cube cube, List<Map<String, Map.Entry<DataTypeJdbc, Object>>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        pending.computeIfAbsent(cube, c -> new java.util.ArrayList<>()).addAll(rows);
    }

    @Override
    public Set<Cube> pendingCubes() {
        synchronized (pending) {
            return Set.copyOf(pending.keySet());
        }
    }

    @Override
    public void clearPendingRows(Cube cube) {
        pending.remove(cube);
    }

    @Override
    public boolean hasPendingChanges() {
        return !writebackCells.isEmpty() || !pending.isEmpty();
    }

    @Override
    public void clear() {
        writebackCells.clear();
        pending.clear();
    }

    /**
     * The calculation that carries this stand: asked for a cell, it reads what the cell is worth without
     * the stand and then lets every written cell that reaches it have its say.
     *
     * @return the calculation to attach to the member that stands for this stand
     */
    public Calc<?> overlay() {
        return new OverlayCalc(this);
    }

    /**
     * The stand a calculation carries, when it carries one. A member that stands for a stand is published
     * with its calculation, so this is the way back from the member to the stand it names.
     *
     * @param calc a calculation, possibly one of these
     * @return the stand, or nothing when this calculation is not one
     */
    public static Optional<Scenario> of(Calc<?> calc) {
        return calc instanceof OverlayCalc overlay ? Optional.of(overlay.scenario) : Optional.empty();
    }

    /**
     * How many atomic cells the cell now being evaluated covers. A cube that counts them says so; one that
     * does not leaves every cell counting as one, which is what a stand needs to divide by.
     */
    private static double atomicCellCount(Evaluator evaluator) {
        Optional<Member> counted = evaluator.getCube().atomicCellCountMeasure();
        if (counted.isEmpty()) {
            return 1d;
        }
        int savepoint = evaluator.savepoint();
        try {
            evaluator.setContext(counted.get());
            Object o = evaluator.evaluateCurrent();
            return o instanceof Number number ? number.doubleValue() : 0d;
        } finally {
            evaluator.restore(savepoint);
        }
    }

    /**
     * Reads a cell as this stand has it: first what the source says, then each written cell that stands at,
     * above or below it. A cell may be reached by several writes, and then the order they were written in
     * decides.
     */
    /** The scenario hierarchy the evaluator's cube is read under; the stand's own member names it otherwise. */
    private static org.eclipse.daanse.olap.api.element.Hierarchy standHierarchy(Evaluator evaluator,
            ScenarioBase scenario) {
        for (Member member : evaluator.getMembers()) {
            if (member.getHierarchy().isScenario()) {
                return member.getHierarchy();
            }
        }
        return scenario.getMember().getHierarchy();
    }

    private static final class OverlayCalc extends AbstractNestedUnknownCalc {

        private final ScenarioBase scenario;

        private OverlayCalc(ScenarioBase scenario) {
            super(ScalarType.INSTANCE);
            this.scenario = scenario;
        }

        @Override
        public Object evaluateInternal(Evaluator evaluator) {
            // the scenario hierarchy of the cube being read: the stand stands in each cube through that cube's own
            // hierarchy, at its own place in the cube
            Member withoutTheStand = standHierarchy(evaluator, scenario).getDefaultMember();
            int savepoint = evaluator.savepoint();
            try {
                evaluator.setContext(withoutTheStand);
                Object asStored = evaluator.evaluateCurrent();
                double value = asStored instanceof Number number ? number.doubleValue() : 0d;
                int changes = 0;
                for (WritebackCell written : scenario.getWritebackCells()) {
                    switch (written.getRelationTo(evaluator.getMembers())) {
                    case ABOVE -> {
                        value = spread(written, value, evaluator);
                        ++changes;
                    }
                    case EQUAL -> {
                        value = written.getNewValue();
                        ++changes;
                    }
                    case BELOW -> {
                        value += written.getOffset();
                        ++changes;
                    }
                    case NONE -> {
                        // a write somewhere else in the cube does not reach this cell
                    }
                    }
                }
                return changes == 0 ? asStored : value;
            } finally {
                evaluator.restore(savepoint);
            }
        }

        /**
         * This cell stands below a written one, so it takes its share. How large the share is, is what the
         * rule says: spread evenly, every cell holds the same; spread the difference, every cell moves by
         * the same amount.
         */
        private static double spread(WritebackCell written, double value, Evaluator evaluator) {
            double covered = atomicCellCount(evaluator);
            if (covered == 0d) {
                // the count can read zero while the source has not answered yet; one at least does not
                // divide by zero, and the cell is asked again once the answer is there
                covered = 1d;
            }
            double share = covered / written.getAtomicCellCount();
            AllocationPolicy policy = written.getAllocationPolicy();
            return switch (policy) {
            case EQUAL_ALLOCATION -> written.getNewValue() * share;
            case EQUAL_INCREMENT -> value + written.getOffset() * share;
            default -> throw new IllegalStateException(
                    "A cell below a written one cannot take its share by the rule " + policy);
            };
        }
    }
}
