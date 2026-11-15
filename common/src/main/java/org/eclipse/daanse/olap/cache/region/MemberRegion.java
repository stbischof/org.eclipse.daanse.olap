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
 * The cells a list of members of one dimension reaches. With {@code descendants} the members below the
 * named ones are reached as well, which is what a client asking for a member and its descendants means.
 */
public class MemberRegion implements CellRegionShape {

    private final List<Member> members;
    private final boolean descendants;
    private final Dimension dimension;

    /**
     * @param members     the members, all of one dimension and at least one
     * @param descendants whether the members below the named ones belong to the region
     */
    public MemberRegion( List<Member> members, boolean descendants ) {
        if ( members.isEmpty() ) {
            throw new IllegalArgumentException( "A region of members names at least one member" );
        }
        this.members = List.copyOf( members );
        this.descendants = descendants;
        this.dimension = this.members.getFirst().getDimension();
    }

    /** The members this region names. */
    public List<Member> getMembers() {
        return members;
    }

    /** Whether what lies below the named members belongs to the region. */
    public boolean isDescendants() {
        return descendants;
    }

    @Override
    public List<Dimension> getDimensionality() {
        return List.of( dimension );
    }

    @Override
    public boolean covers( List<? extends Member> coordinate ) {
        for ( Member at : coordinate ) {
            for ( Member named : members ) {
                if ( at.equals( named ) || ( descendants && isBelow( at, named ) ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isBelow( Member candidate, Member ancestor ) {
        for ( Member parent = candidate.getParentMember(); parent != null; parent = parent.getParentMember() ) {
            if ( parent.equals( ancestor ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void accept( CellRegionVisitor visitor ) {
        visitor.visit( this );
    }

    @Override
    public String toString() {
        return members.stream().map( String::valueOf ).collect( Collectors.joining( ", ", "Member(", ")" ) );
    }
}
