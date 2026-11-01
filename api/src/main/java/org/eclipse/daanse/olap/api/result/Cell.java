/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * Contributors:
 *  SmartCity Jena - refactor, clean API
*
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.api.result;

import java.util.List;

import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.slf4j.Logger;

/**
 * A Cell is an item in the grid of a Result. It is returned by Result#getCell.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public interface Cell {
    /**
     * Returns the coordinates of this Cell in its Result.
     *
     * @return Coordinates of this Cell
 */
    List<Integer> getCoordinateList();

    /**
     * Returns the cell's raw value. This is useful for sending to further data
     * processing, such as plotting a chart.
     *
     * <p>
     * If the cell is NULL, the value is Java {@code null}. If the
     * cell contains an error, {@link #isError()} is true and this method
     * returns the underlying {@link Throwable}. Otherwise the type of this value
     * depends upon the type of measure: possible types include
     * java.math.BigDecimal, Double, Integer and String.
     *
     * <p>
     * {@code (return == null) == isNull()}
 */
    Object getValue();

    /**
     * Return the cached formatted string, that survives an aggregate cache clear.
 */
    String getCachedFormatString();

    /**
     * Returns the cell's value formatted according to the current format string,
     * and locale-specific settings such as currency symbol. The current format
     * string may itself be derived via an expression. For more information about
     * format strings, see mondrian.util.Format.
 */
    String getFormattedValue();

    /**
     * Returns whether the cell's value is null.
 */
    boolean isNull();

    /**
     * Returns whether the cell's calculation returned an error.
 */
    boolean isError();

    /**
     * Returns true if drill through is possible for this Cell. Returns false if the
     * Cell is based on a calculated measure.
     *
     * @return Whether can drill through on this cell
 */
    boolean canDrillThrough();

    /**
     * Returns the number of fact table rows which contributed to this Cell.
 */
    int getDrillThroughCount();

    /**
     * Returns the value of a property.
     *
     * @param propertyName Case-sensitive property name
     * @return Value of property
 */
    Object getPropertyValue(String propertyName);

    /**
     * Returns the context member for a particular dimension.
     *
     * The member is defined as follows (note that there is always a member):
     *
     * If the dimension appears on one of the visible axes, the context member is
     * simply the member on the current row or column.
     *
     * If the dimension appears in the slicer, the context member is the member of
     * that dimension in the slier.
     *
     * Otherwise, the context member is the default member of that dimension
     * (usually the 'all' member).
     *
     * @param hierarchy Hierarchy
     * @return current member of given hierarchy
 */
    Member getContextMember(Hierarchy hierarchy);

    /**
     * Helper method to implement org.olap4j.Cell#setValue.
     *
     * @param scenario         Scenario
     * @param newValue         New value
     * @param allocationPolicy Allocation policy
     * @param allocationArgs   Arguments for allocation policy
 */
    void setValue(Scenario scenario, Object newValue, AllocationPolicy allocationPolicy, Object... allocationArgs);

    /**
     * The rows behind this cell; the caller closes them.
     *
     * @param maxRowCount     maximum number of rows, {@code <= 0} when unlimited
     * @param firstRowOrdinal ordinal of the first row (1-based), {@code 0} to start at the beginning
     * @param fields          the fields to return; empty for the default
     * @param extendedContext whether the levels of non-constraining members are included
     * @param logger          where the provider logs what it runs; may be {@code null}
     */
    DrillThroughRows drillThroughInternal(int maxRowCount, int firstRowOrdinal, List<OlapElement> fields,
            boolean extendedContext, Logger logger);
}
