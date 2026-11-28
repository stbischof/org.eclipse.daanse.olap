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
package org.eclipse.daanse.olap.metadata.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.daanse.olap.metadata.model.Access;
import org.eclipse.daanse.olap.metadata.model.Grants;

/** Builds a role. It sees nothing of the catalog until a grant says so. */
public final class RoleBuilder extends ElementBuilder<RoleBuilder> {

    Access.Catalog catalogAccess = Access.Catalog.NONE;
    final List<Grants.CubeGrant> cubeGrants = new ArrayList<>();
    final List<Grants.DatabaseSchemaGrant> databaseGrants = new ArrayList<>();

    /** What the role may read of a database schema; tables and columns follow with {@code CUSTOM}. */
    public RoleBuilder databaseSchema(org.eclipse.daanse.olap.metadata.relational.DatabaseSchemaDef schema, Access.Database access,
            List<Grants.TableGrant> tables) {
        databaseGrants.add(new Grants.DatabaseSchemaGrant(schema, access, tables));
        return this;
    }

    RoleBuilder(String name) {
        super(name);
    }

    public RoleBuilder catalog(Access.Catalog access) {
        this.catalogAccess = access;
        return this;
    }

    public RoleBuilder cube(String cubeName, Access.Cube access) {
        return cube(cubeName, access, c -> { });
    }

    public RoleBuilder cube(String cubeName, Access.Cube access, Consumer<CubeGrantBuilder> grants) {
        CubeGrantBuilder builder = new CubeGrantBuilder();
        grants.accept(builder);
        cubeGrants.add(new Grants.CubeGrant(cubeName, access, builder.dimensions, builder.hierarchies));
        return this;
    }

    /** The grants within one cube. All names are MDX text. */
    public static final class CubeGrantBuilder {

        private final List<Grants.DimensionGrant> dimensions = new ArrayList<>();
        private final List<Grants.HierarchyGrant> hierarchies = new ArrayList<>();

        CubeGrantBuilder() {
        }

        public CubeGrantBuilder dimension(String dimension, Access.Cube access) {
            dimensions.add(new Grants.DimensionGrant(dimension, access));
            return this;
        }

        public CubeGrantBuilder hierarchy(String hierarchy, Access.Hierarchy access) {
            return hierarchy(hierarchy, access, h -> { });
        }

        public CubeGrantBuilder hierarchy(String hierarchy, Access.Hierarchy access,
                Consumer<HierarchyGrantBuilder> grants) {
            HierarchyGrantBuilder builder = new HierarchyGrantBuilder();
            grants.accept(builder);
            hierarchies.add(new Grants.HierarchyGrant(hierarchy, access, builder.topLevel, builder.bottomLevel,
                    builder.rollup, builder.members));
            return this;
        }
    }

    /** The grants within one hierarchy. */
    public static final class HierarchyGrantBuilder {

        private String topLevel;
        private String bottomLevel;
        private Access.Rollup rollup = Access.Rollup.FULL;
        private final List<Grants.MemberGrant> members = new ArrayList<>();

        HierarchyGrantBuilder() {
        }

        public HierarchyGrantBuilder topLevel(String level) {
            this.topLevel = level;
            return this;
        }

        public HierarchyGrantBuilder bottomLevel(String level) {
            this.bottomLevel = level;
            return this;
        }

        /** What an aggregate shows when some of its members are hidden. */
        public HierarchyGrantBuilder rollup(Access.Rollup rollup) {
            this.rollup = rollup;
            return this;
        }

        public HierarchyGrantBuilder member(String member, Access.Member access) {
            members.add(new Grants.MemberGrant(member, access));
            return this;
        }
    }
}
