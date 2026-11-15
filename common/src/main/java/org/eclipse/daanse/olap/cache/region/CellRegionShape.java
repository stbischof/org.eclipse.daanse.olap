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
     * Shows this shape to a visitor, and a shape made of others shows them too.
     *
     * @param visitor the visitor to show it to
     */
    void accept( CellRegionVisitor visitor );
}
