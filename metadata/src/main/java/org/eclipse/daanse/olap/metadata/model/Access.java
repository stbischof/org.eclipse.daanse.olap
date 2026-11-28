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
package org.eclipse.daanse.olap.metadata.model;

/** The access levels a role grants. */
public final class Access {

    private Access() {
    }

    /** On a catalog. */
    public enum Catalog {
        ALL, ALL_DIMENSIONS, CUSTOM, NONE
    }

    /** On a cube or a dimension. */
    public enum Cube {
        ALL, CUSTOM, NONE
    }

    /** On a hierarchy; {@code CUSTOM} goes on with member grants. */
    public enum Hierarchy {
        ALL, CUSTOM, NONE
    }

    /** On a member and everything below it; {@code CUSTOM} goes on with grants further down. */
    public enum Member {
        ALL, CUSTOM, NONE
    }

    /**
     * On a database schema, a table or a column, for what a role may read through the relational rowsets and SQL;
     * {@code CUSTOM} goes on with grants further down. A column has no {@code CUSTOM}.
     */
    public enum Database {
        ALL, CUSTOM, NONE
    }

    /** What an aggregate shows when some of its members are hidden. */
    public enum Rollup {
        FULL, PARTIAL, HIDDEN
    }
}
