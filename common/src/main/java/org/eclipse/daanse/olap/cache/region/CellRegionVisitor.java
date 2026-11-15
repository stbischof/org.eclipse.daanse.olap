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

import org.eclipse.daanse.olap.api.cache.CacheControl;

/**
 * Reads a region shape by shape. An art that brings a shape of its own sees it in {@link #visitOther}.
 */
public interface CellRegionVisitor {

    /** Called for a region of named members. */
    void visit( MemberRegion region );

    /** Called for the empty region. */
    void visit( EmptyRegion region );

    /** Called for a crossing, before its components. */
    void visit( CrossjoinRegion region );

    /** Called for a union, before its components. */
    void visit( UnionRegion region );

    /**
     * Called for a shape this package does not know, such as a relational range of members.
     *
     * @param region the shape
     */
    default void visitOther( CacheControl.CellRegion region ) {
        // A visitor that knows no shape of its own has nothing to do here.
    }
}
