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

import java.util.List;

/** What a role grants, from the catalog down to members. All names are MDX text. */
public final class Grants {

    private Grants() {
    }

    public record CubeGrant(String cube, Access.Cube access, List<DimensionGrant> dimensions,
            List<HierarchyGrant> hierarchies) {

        public CubeGrant {
            dimensions = List.copyOf(dimensions);
            hierarchies = List.copyOf(hierarchies);
        }
    }

    public record DimensionGrant(String dimension, Access.Cube access) {
    }

    /**
     * @param topLevel    the highest level the role sees; {@code null} for the top
     * @param bottomLevel the lowest level the role sees; {@code null} for the bottom
     */
    public record HierarchyGrant(String hierarchy, Access.Hierarchy access, String topLevel, String bottomLevel,
            Access.Rollup rollup, List<MemberGrant> members) {

        public HierarchyGrant {
            members = List.copyOf(members);
        }
    }

    public record MemberGrant(String member, Access.Member access) {
    }

    /** What a role may read of a database schema, and below it of its tables. */
    public record DatabaseSchemaGrant(org.eclipse.daanse.olap.metadata.relational.DatabaseSchemaDef schema, Access.Database access,
            List<TableGrant> tables) {

        public DatabaseSchemaGrant {
            tables = List.copyOf(tables);
        }
    }

    /** What a role may read of a table, and below it of its columns. */
    public record TableGrant(org.eclipse.daanse.olap.metadata.relational.TableDef table, Access.Database access, List<ColumnGrant> columns) {

        public TableGrant {
            columns = List.copyOf(columns);
        }
    }

    /** Whether a role may read a column; {@code CUSTOM} is no answer for a column. */
    public record ColumnGrant(org.eclipse.daanse.olap.metadata.relational.PhysicalColumnDef column, Access.Database access) {

        public ColumnGrant {
            if (access == Access.Database.CUSTOM) {
                throw new IllegalArgumentException("column " + column + ": a column is read or not, CUSTOM says nothing");
            }
        }
    }
}
