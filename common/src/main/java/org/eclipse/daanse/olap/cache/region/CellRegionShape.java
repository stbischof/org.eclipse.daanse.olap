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
package org.eclipse.daanse.olap.cache.region;

import java.util.List;

import org.eclipse.daanse.olap.api.cache.CacheControl;
import org.eclipse.daanse.olap.api.element.Member;

/**
 * A region of cells that says which cells lie in it. The shapes below build the algebra a client uses
 * through {@link CacheControl}: a region of members, a crossing, a union, and the empty region.
 */
public interface CellRegionShape extends CacheControl.CellRegion {

    /**
     * Whether the cell at this coordinate lies in the region.
     *
     * @param coordinate the members a cell is kept under, in any order
     * @return true when the region reaches that cell
     */
    boolean covers( List<? extends Member> coordinate );

    /**
     * Whether this region holds anything the cell at that coordinate was worked out from.
     *
     * <p>A different question from {@link #covers(List)}, and the one a forgetting has to ask. A cell sums
     * the facts below it, so a cell above the region depends on what the region names even though the region
     * does not reach it: forgetting a quarter leaves the year that holds it standing on numbers that are no
     * longer there.
     *
     * @param coordinate the members of a cell, its measure among them
     * @return true when the region and that cell meet, in either direction
     */
    boolean touches( List<? extends Member> coordinate );

    /**
     * Shows this shape to a visitor, and a shape made of others shows them too.
     *
     * @param visitor the visitor to show it to
     */
    void accept( CellRegionVisitor visitor );
}
