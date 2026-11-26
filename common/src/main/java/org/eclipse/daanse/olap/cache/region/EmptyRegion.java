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

import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Member;

/** The region that reaches no cell. */
public class EmptyRegion implements CellRegionShape {

    @Override
    public List<Dimension> getDimensionality() {
        return List.of();
    }

    @Override
    public boolean covers( List<? extends Member> coordinate ) {
        return false;
    }

    @Override
    public void accept( CellRegionVisitor visitor ) {
        visitor.visit( this );
    }

    @Override
    public String toString() {
        return "Empty()";
    }

    @Override
    public boolean touches( List<? extends Member> coordinate ) {
        return false;
    }
}
