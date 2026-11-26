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
import java.util.stream.Collectors;

import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Member;

/**
 * The cells that lie in any one of several regions. All of them are about the same dimensions, because
 * a union of regions of different shape would name cells no cache can address.
 */
public class UnionRegion implements CellRegionShape {

    private final List<CellRegionShape> regions;

    /**
     * @param regions the regions to unite, at least one and all of the same dimensionality
     */
    public UnionRegion( List<? extends CellRegionShape> regions ) {
        if ( regions.isEmpty() ) {
            throw new IllegalArgumentException( "A union names at least one region" );
        }
        List<Dimension> first = regions.getFirst().getDimensionality();
        for ( CellRegionShape region : regions ) {
            if ( !region.getDimensionality().equals( first ) ) {
                throw new IllegalArgumentException( "A union unites regions of the same dimensions" );
            }
        }
        this.regions = List.copyOf( regions );
    }

    /** The regions this union is made of. */
    public List<CellRegionShape> getRegions() {
        return regions;
    }

    @Override
    public List<Dimension> getDimensionality() {
        return regions.getFirst().getDimensionality();
    }

    @Override
    public boolean covers( List<? extends Member> coordinate ) {
        for ( CellRegionShape region : regions ) {
            if ( region.covers( coordinate ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void accept( CellRegionVisitor visitor ) {
        visitor.visit( this );
        for ( CellRegionShape region : regions ) {
            region.accept( visitor );
        }
    }

    @Override
    public String toString() {
        return regions.stream().map( String::valueOf ).collect( Collectors.joining( ", ", "Union(", ")" ) );
    }

    @Override
    public boolean touches( List<? extends Member> coordinate ) {
        for ( CellRegionShape region : regions ) {
            if ( region.touches( coordinate ) ) {
                return true;
            }
        }
        return false;
    }
}
