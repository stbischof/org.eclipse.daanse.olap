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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Member;

/**
 * The cells that lie in every one of several regions: the cartesian product. A crossing of crossings is
 * flattened, and no dimension may appear twice - that would name two things about one dimension.
 */
public class CrossjoinRegion implements CellRegionShape {

    private final List<CellRegionShape> components;
    private final List<Dimension> dimensions;

    /**
     * @param regions the regions to cross, each over dimensions the others do not name
     */
    public CrossjoinRegion( List<? extends CellRegionShape> regions ) {
        List<CellRegionShape> flat = new ArrayList<>();
        List<Dimension> dimensionality = new ArrayList<>();
        Set<Dimension> seen = new HashSet<>();
        for ( CellRegionShape region : regions ) {
            if ( region instanceof CrossjoinRegion crossing ) {
                flat.addAll( crossing.components );
            } else {
                flat.add( region );
            }
            for ( Dimension dimension : region.getDimensionality() ) {
                if ( !seen.add( dimension ) ) {
                    throw new IllegalArgumentException(
                            "A crossing names the dimension " + dimension.getName() + " twice" );
                }
                dimensionality.add( dimension );
            }
        }
        this.components = List.copyOf( flat );
        this.dimensions = List.copyOf( dimensionality );
    }

    /** The regions this crossing is made of, flattened. */
    public List<CellRegionShape> getComponents() {
        return components;
    }

    @Override
    public List<Dimension> getDimensionality() {
        return dimensions;
    }

    @Override
    public boolean covers( List<? extends Member> coordinate ) {
        for ( CellRegionShape component : components ) {
            if ( !component.covers( coordinate ) ) {
                return false;
            }
        }
        return !components.isEmpty();
    }

    @Override
    public void accept( CellRegionVisitor visitor ) {
        visitor.visit( this );
        for ( CellRegionShape component : components ) {
            component.accept( visitor );
        }
    }

    @Override
    public String toString() {
        return components.stream().map( String::valueOf )
                .collect( Collectors.joining( ", ", "Crossjoin(", ")" ) );
    }
}
