/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.provider.memory;

import java.util.List;

import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.result.AllocationPolicy;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.olap.format.DefaultFormatter;

/**
 * One cell of a result: the value at a coordinate, and the members that address it.
 *
 * <p>There is nothing behind it to drill through to and nothing to write back into, so
 * those two ask are refused by name rather than answered with a lie.
 */
public class MemoryCell implements Cell {

    private static final DefaultFormatter FORMATTER = new DefaultFormatter();

    private final List<Integer> coordinates;
    private final List<Member> members;
    private final Object value;
    private final String formatString;

    MemoryCell(List<Integer> coordinates, List<Member> members, Object value, String formatString) {
        this.coordinates = List.copyOf(coordinates);
        this.members = List.copyOf(members);
        this.value = value;
        this.formatString = formatString;
    }

    @Override
    public List<Integer> getCoordinateList() {
        return coordinates;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    @Override
    public boolean isError() {
        return value instanceof Throwable;
    }

    @Override
    public String getCachedFormatString() {
        return formatString;
    }

    /**
     * The value as text.
     *
     * <p>Through the engine's own default formatter rather than {@code toString}. That
     * one strips trailing zeros, so a count of six reads as "6" and not as "6.0", which is
     * what every client shows and what a cell without a format string means.
     */
    @Override
    public String getFormattedValue() {
        if (value == null) {
            return "";
        }
        String formatted = FORMATTER.format(value);
        return formatted == null ? "" : formatted;
    }

    @Override
    public Member getContextMember(Hierarchy hierarchy) {
        for (Member m : members) {
            if (m.getHierarchy().equals(hierarchy)) {
                return m;
            }
        }
        return hierarchy.getDefaultMember();
    }

    @Override
    public Object getPropertyValue(String propertyName) {
        return switch (propertyName) {
            case "VALUE" -> value;
            case "FORMATTED_VALUE" -> getFormattedValue();
            case "FORMAT_STRING" -> formatString;
            default -> null;
        };
    }

    @Override
    public boolean canDrillThrough() {
        return false;
    }

    @Override
    public int getDrillThroughCount() {
        return -1;
    }

    @Override
    public void setValue(Scenario scenario, Object newValue, AllocationPolicy allocationPolicy,
            Object... allocationArgs) {
        throw new UnsupportedOperationException(
                "MemoryCell.setValue: this provider holds its values, it does not write them back");
    }
    @Override
    public org.eclipse.daanse.olap.api.result.DrillThroughRows drillThroughInternal(int maxRowCount, int firstRowOrdinal,
            List<org.eclipse.daanse.olap.api.element.OlapElement> fields, boolean extendedContext,
            org.slf4j.Logger logger) {
        throw new UnsupportedOperationException(
                "MemoryCell.drillThroughInternal: there are no rows behind this cell to reach");
    }

}
