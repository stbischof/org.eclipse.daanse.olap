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
package org.eclipse.daanse.olap.result;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.result.AllocationPolicy;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.DrillThroughRows;
import org.eclipse.daanse.olap.api.result.ErrorValue;
import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.result.NullValue;
import org.eclipse.daanse.olap.api.result.ObjectValue;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.olap.common.ExecutionConfig;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.updateable.UpdateabilityResolver;
import org.slf4j.Logger;

/**
 * The cell of a {@link ResultImpl}, without anything a particular provider knows: value, format
 * and cell properties come from the evaluation. It has no rows behind it and takes no writes; a
 * provider that offers drill-through or writeback overrides those methods.
 */
public class ResultCell implements Cell {

    private static final int MDACTION_TYPE_DRILLTHROUGH = 0x100;

    protected final ResultImpl result;
    protected final int[] pos;
    protected final CellInfo ci;

    public ResultCell(ResultImpl result, int[] pos, CellInfo ci) {
        this.result = result;
        this.pos = pos;
        this.ci = ci;
    }

    @Override
    public List<Integer> getCoordinateList() {
        return new AbstractList<>() {
            @Override
            public Integer get(int index) {
                return pos[index];
            }

            @Override
            public int size() {
                return pos.length;
            }
        };
    }

    /** NULL cells yield {@code null}, error cells the {@link Throwable}, other cells the value. */
    @Override
    public Object getValue() {
        return switch (ci.value) {
        case null -> null;
        case NullValue v -> null;
        case NotLoaded n -> n;
        case ErrorValue e -> e.cause();
        case ObjectValue o -> o.value();
        };
    }

    @Override
    public String getCachedFormatString() {
        return ci.formatString;
    }

    @Override
    public String getFormattedValue() {
        return ci.getFormatValue();
    }

    @Override
    public boolean isNull() {
        return ci.value == null || ci.value instanceof NullValue;
    }

    @Override
    public boolean isError() {
        return ci.value instanceof ErrorValue;
    }

    @Override
    public boolean canDrillThrough() {
        if (!drillThroughAllowed()) {
            return false;
        }
        Member[] coordinates = drillThroughCoordinates();
        return !hasCalculatedCoordinate(coordinates) && rowsLieBehind(coordinates);
    }

    /** Whether this context lets a client look behind a cell at all. */
    protected boolean drillThroughAllowed() {
        return result.getQuery().getConnection().getContext().getConfig().enableDrillThrough();
    }

    /**
     * The members a drill-through would use. The base takes them as the cell stands; a catalog whose
     * formulas can name another member replaces such a coordinate by the member it names.
     */
    protected Member[] drillThroughCoordinates() {
        return result.getCellMembers(pos);
    }

    /**
     * The member a formula does no more than rename, or empty when it does more.
     * <p>
     * Two formulas name a member without changing what it stands for: the member itself, and
     * {@code Aggregate({m})} over a single member. A cell of such a member therefore stands for the same
     * facts as {@code m}, which is what lets a drill-through name its rows.
     * <p>
     * Asked by the name a function is registered under, not by its class: the classes live in the
     * function library, and the engine's cell must not reach for them. Exactly one function is
     * registered under {@code Aggregate} and one under {@code &#123;&#125;}, so the two tests say the same
     * thing.
     */
    protected static Optional<Member> memberBehindTrivialFormula(Member member) {
        return memberNamedBy(member.getExpression());
    }

    private static Optional<Member> memberNamedBy(Expression expression) {
        if (expression instanceof MemberExpression named) {
            return Optional.of(named.getMember());
        }
        if (expression instanceof ResolvedFunCall call && isNamed(call, "Aggregate") && call.getArgCount() == 1
                && call.getArg(0) instanceof ResolvedFunCall set && isNamed(set, "{}") && set.getArgCount() == 1
                && set.getArg(0) instanceof MemberExpression only) {
            return Optional.of(only.getMember());
        }
        return Optional.empty();
    }

    private static boolean isNamed(ResolvedFunCall call, String name) {
        return call.getFunDef().getFunctionMetaData().operationAtom().name().equals(name);
    }

    /** Whether anything lies behind these coordinates that a client could be shown. */
    protected boolean rowsLieBehind(Member[] coordinates) {
        return false;
    }

    /**
     * The role of the connection this cell was worked out for, empty when the cell no longer knows its
     * execution.
     */
    protected Optional<Role> connectionRole() {
        Execution execution = result.getExecution();
        if (execution == null || execution.getDaanseStatement() == null
                || execution.getDaanseStatement().getDaanseConnection() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(execution.getDaanseStatement().getDaanseConnection().getRole());
    }

    /**
     * Makes {@code coordinates} ready for a write and answers which scenario the write belongs to.
     * <p>
     * A scenario is named among the coordinates by a member of the hierarchy that says it holds them;
     * that coordinate is replaced by its All member, because a write reaches the cell as a whole and not
     * one scenario of it. A coordinate worked out from a formula names no fact, so a write to it is
     * refused by name rather than silently going nowhere.
     */
    protected Scenario prepareForWriteback(Member[] coordinates) {
        Scenario scenario = null;
        for (int i = 0; i < coordinates.length; i++) {
            Member member = coordinates[i];
            if (member.getHierarchy().isScenario()) {
                scenario = (Scenario) member.getPropertyValue(StandardProperty.SCENARIO.getName());
                coordinates[i] = member.getHierarchy().getAllMember();
            } else if (member.isCalculated()) {
                throw new IllegalArgumentException("Cannot write to cell: one of the coordinates ("
                        + member.getUniqueName() + ") is a calculated member");
            }
        }
        return scenario;
    }

    @Override
    public int getDrillThroughCount() {
        return -1;
    }

    @Override
    public DrillThroughRows drillThroughInternal(int maxRowCount, int firstRowOrdinal, List<OlapElement> fields,
            boolean extendedContext, Logger logger) {
        throw new UnsupportedOperationException("This provider offers no drill-through");
    }

    @Override
    public void setValue(Scenario scenario, Object newValue, AllocationPolicy allocationPolicy,
            Object... allocationArgs) {
        throw new UnsupportedOperationException("This provider takes no writeback");
    }

    /** The members this cell stands for, in the order the axes give them. */
    public List<Member> getMembersInCell() {
        Member[] members = result.getCellMembers(pos);
        return members == null ? List.of() : Collections.unmodifiableList(Arrays.asList(members));
    }

    /**
     * Whether a coordinate other than the measure is worked out from a formula. Such a cell has no
     * rows of its own, because a formula names no fact; the measure is exempt, because a formula over
     * measures of one cube still points at that cube's rows.
     */
    protected static boolean hasCalculatedCoordinate(Member[] coordinates) {
        for (int i = 1; i < coordinates.length; i++) {
            if (coordinates[i].isCalculated()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loosens the coordinates a cell stands for where a slicer of several positions names more than
     * one member of a hierarchy.
     * <p>
     * A cell of such a result is the total over those members, but the coordinate array keeps only the
     * last one of each hierarchy and would therefore say something narrower than the cell means. Where
     * that happens the coordinate becomes the hierarchy's All member, and what the slicer really
     * selects is expressed by the caller instead. A slicer of at most one position says nothing that
     * the coordinates do not already say, so nothing is loosened.
     *
     * @param coordinates the members of the cell, by the ordinal of their hierarchy in the cube;
     *                    modified in place
     * @param slicerAxis  the slicer of the result the cell belongs to
     */
    protected static void loosenForCompoundSlicer(Member[] coordinates, Axis slicerAxis) {
        List<Position> positions = slicerAxis.getPositions();
        if (positions.size() <= 1) {
            return;
        }
        for (Position position : positions) {
            for (Member member : position) {
                Hierarchy hierarchy = member.getHierarchy();
                int ordinal = hierarchy.getOrdinalInCube();
                if (!coordinates[ordinal].equals(member)) {
                    coordinates[ordinal] = hierarchy.getAllMember();
                }
            }
        }
    }

    @Override
    public Member getContextMember(Hierarchy hierarchy) {
        return result.getMember(pos, hierarchy);
    }

    @Override
    public Object getPropertyValue(String propertyName) {
        StandardProperty property = StandardProperty.lookup(propertyName, ExecutionConfig.current().caseSensitive());
        Object defaultValue = null;
        if (property == StandardProperty.CELL_ORDINAL) {
            return result.getCellOrdinal(pos);
        } else if (property == StandardProperty.VALUE) {
            return getValue();
        } else if (property == StandardProperty.FORMAT_STRING) {
            return formatString();
        } else if (property == StandardProperty.FORMATTED_VALUE) {
            return getFormattedValue();
        } else if (property == StandardProperty.FONT_FLAGS || property == StandardProperty.SOLVE_ORDER) {
            defaultValue = 0;
        } else if (property == StandardProperty.ACTION_TYPE) {
            return canDrillThrough() ? MDACTION_TYPE_DRILLTHROUGH : 0;
        } else if (property == StandardProperty.DRILLTHROUGH_COUNT) {
            return canDrillThrough() ? getDrillThroughCount() : -1;
        } else if (property == StandardProperty.BACK_COLOR || property == StandardProperty.FORE_COLOR) {
            return formatStringSection(property.getName());
        } else if (property == StandardProperty.UPDATEABLE) {
            Optional<Object> updateable = updateability();
            if (updateable.isPresent()) {
                return updateable.get();
            }
        }
        final Evaluator evaluator = result.getRootEvaluator();
        final int savepoint = evaluator.savepoint();
        try {
            result.populateEvaluator(evaluator, pos);
            return evaluator.getProperty(propertyName, defaultValue);
        } finally {
            evaluator.restore(savepoint);
        }
    }

    /** The format string of the cell, evaluated in the context of the cell when not yet known. */
    /**
     * What a client may do to this cell, as {@code UPDATEABLE} names it. Answered by whoever knows
     * what a write would reach; nothing means the question is left to the evaluator, which answers
     * what the query says.
     *
     * @return the answer, or nothing when this cell does not know one
     */
    protected Optional<Object> updateability() {
        return Optional.of(UpdateabilityResolver
                .resolve(getMembersInCell(), connectionRole(), result.getCube()).getUpdateable());
    }

    protected String formatString() {
        if (ci.formatString == null) {
            final Evaluator evaluator = result.getRootEvaluator();
            final int savepoint = evaluator.savepoint();
            try {
                result.populateEvaluator(evaluator, pos);
                ci.formatString = evaluator.getFormatString();
            } finally {
                evaluator.restore(savepoint);
            }
        }
        return ci.formatString;
    }

    /** The value of a {@code NAME=value} section of the format string, or {@code null}. */
    private String formatStringSection(String name) {
        String formatString = formatString();
        if (formatString == null) {
            return null;
        }
        for (String part : formatString.split(";")) {
            String[] pair = part.split("=");
            if (pair.length == 2 && pair[0].toUpperCase(Locale.ROOT).equals(name)) {
                return pair[1];
            }
        }
        return null;
    }

    /**
     * The cube whose rows lie behind these coordinates, empty when none does. The rule is the same for
     * every provider; what differs is whether anyone can deliver the rows, which {@link
     * #rowsLieBehind(Member[])} answers.
     */
    protected Optional<org.eclipse.daanse.olap.api.element.Cube> cubeBehind(Member[] coordinates) {
        org.eclipse.daanse.olap.api.element.Cube queried =
                result.getQuery() == null ? null : result.getQuery().getCube();
        return DrillThroughCubeChoice.cubeBehind(coordinates, queried, ambiguityMode());
    }

    /** What a drill-through answers where a virtual cube draws on more than one set of facts. */
    protected DrillThroughCubeChoice.Mode ambiguityMode() {
        return DrillThroughCubeChoice.Mode
                .named(result.getQuery().getConnection().getContext().getConfig()
                        .drillThroughOnAmbiguousVirtualCube());
    }
}
