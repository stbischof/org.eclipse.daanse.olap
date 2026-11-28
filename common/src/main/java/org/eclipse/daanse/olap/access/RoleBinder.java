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
package org.eclipse.daanse.olap.access;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.access.AccessCatalog;
import org.eclipse.daanse.olap.api.access.AccessCube;
import org.eclipse.daanse.olap.api.access.AccessDimension;
import org.eclipse.daanse.olap.api.access.AccessHierarchy;
import org.eclipse.daanse.olap.api.access.AccessMember;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.access.RollupPolicy;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.metadata.model.Grants;
import org.eclipse.daanse.olap.metadata.model.RoleDef;

/**
 * Binds the roles of a catalog description to a runtime catalog, whichever art built it: grants name cubes,
 * dimensions, hierarchies, levels and members in MDX, so they are looked up in the catalog without access control
 * and granted on a {@link RoleImpl}; a union role is the union of its bound parts.
 *
 * <p>A role binds once per binder; a part shared by two unions is the same role object in both.
 */
public final class RoleBinder {

    private final Catalog catalog;
    private final boolean invalidMembersPassOver;
    private final Map<RoleDef, Role> bound = new IdentityHashMap<>();

    /**
     * @param invalidMembersPassOver whether a grant on a member the catalog does not have is left out instead of
     *                               bringing the role down (a context that lets invalid members pass)
     */
    public RoleBinder(Catalog catalog, boolean invalidMembersPassOver) {
        this.catalog = catalog;
        this.invalidMembersPassOver = invalidMembersPassOver;
    }

    /** The runtime role of a role of the description. */
    public Role bind(RoleDef roleDef) {
        Role known = bound.get(roleDef);
        if (known != null) {
            return known;
        }
        Role role = roleDef.unionOf().isEmpty() ? bindGrants(roleDef) : bindUnion(roleDef);
        bound.put(roleDef, role);
        return role;
    }

    /** A union role: the union of its parts, each bound once. */
    private Role bindUnion(RoleDef roleDef) {
        List<Role> parts = new ArrayList<>();
        for (RoleDef part : roleDef.unionOf()) {
            parts.add(bind(part));
        }
        return RoleImpl.union(parts);
    }

    private Role bindGrants(RoleDef roleDef) {
        RoleImpl role = new RoleImpl();
        role.grant(catalog, AccessCatalog.valueOf(roleDef.catalogAccess().name()));
        databaseGrants(role, roleDef);
        for (Grants.CubeGrant cubeGrant : roleDef.cubeGrants()) {
            bindCube(role, roleDef, cubeGrant);
        }
        role.makeImmutable();
        return role;
    }

    private void bindCube(RoleImpl role, RoleDef roleDef, Grants.CubeGrant cubeGrant) {
        Cube cube = catalog.lookupCube(cubeGrant.cube()).orElseThrow(
                () -> new IllegalStateException("Role " + roleDef.name() + " names the cube "
                        + cubeGrant.cube() + ", which the catalog does not have"));
        // the cube's own reader: a relational cube resolves its members only through it
        CatalogReader reader = cube.getCatalogReader(null).withoutAccessControl();
        role.grant(cube, AccessCube.valueOf(cubeGrant.access().name()));
        for (Grants.DimensionGrant grant : cubeGrant.dimensions()) {
            role.grant(lookup(reader, cube, grant.dimension(), Dimension.class, roleDef),
                    AccessDimension.valueOf(grant.access().name()));
        }
        for (Grants.HierarchyGrant grant : cubeGrant.hierarchies()) {
            bindHierarchy(role, roleDef, cube, reader, grant);
        }
    }

    /**
     * A grant on a hierarchy. Members are granted one by one only under custom access; when every member the grant
     * names is missing from the catalog, the role sees none of the hierarchy rather than all of it.
     */
    private void bindHierarchy(RoleImpl role, RoleDef roleDef, Cube cube, CatalogReader reader,
            Grants.HierarchyGrant grant) {
        Hierarchy hierarchy = lookup(reader, cube, grant.hierarchy(), Hierarchy.class, roleDef);
        AccessHierarchy access = AccessHierarchy.valueOf(grant.access().name());
        boolean custom = access == AccessHierarchy.CUSTOM;
        if (!custom && !grant.members().isEmpty()) {
            throw Util.newError("Role " + roleDef.name() + " grants members of " + hierarchy.getUniqueName()
                    + " one by one in cube " + cube.getName() + ", which needs custom access to the hierarchy, not "
                    + access.name().toLowerCase(Locale.ROOT));
        }
        RollupPolicy rollup = RollupPolicy.valueOf(grant.rollup().name());
        role.grant(hierarchy, access,
                custom && grant.topLevel() != null ? lookup(reader, cube, grant.topLevel(), Level.class, roleDef)
                        : null,
                custom && grant.bottomLevel() != null ? lookup(reader, cube, grant.bottomLevel(), Level.class, roleDef)
                        : null,
                rollup);
        if (grant.members().isEmpty()) {
            return;
        }
        List<Map.Entry<Member, AccessMember>> members = members(roleDef, cube, reader, hierarchy, grant);
        if (members.isEmpty()) {
            role.grant(hierarchy, AccessHierarchy.NONE, null, null, rollup);
            return;
        }
        for (Map.Entry<Member, AccessMember> member : members) {
            role.grant(member.getKey(), member.getValue());
        }
    }

    /** The members a hierarchy grant names, each with its access; a missing one passes over only where allowed. */
    private List<Map.Entry<Member, AccessMember>> members(RoleDef roleDef, Cube cube, CatalogReader reader,
            Hierarchy hierarchy, Grants.HierarchyGrant grant) {
        List<Map.Entry<Member, AccessMember>> members = new ArrayList<>();
        for (Grants.MemberGrant memberGrant : grant.members()) {
            // through the cube and under a locus: the cube resolves its own member (in a provider's virtual cube the
            // unique-name path gives the base cube's), a relational reader may go to the database
            OlapElement found = reader.withLocus().lookupCompound(cube, Util.parseIdentifier(memberGrant.member()),
                    false, DataType.MEMBER);
            if (!(found instanceof Member member)) {
                if (invalidMembersPassOver) {
                    continue;
                }
                throw new IllegalStateException("Role " + roleDef.name() + " grants the member " + memberGrant.member()
                        + " in cube " + cube.getName() + ", which the catalog does not have");
            }
            // by unique name: in a virtual cube of the provider the member keeps the base cube's hierarchy object
            if (!member.getHierarchy().getUniqueName().equals(hierarchy.getUniqueName())) {
                throw Util.newError("Role " + roleDef.name() + " grants " + member.getUniqueName() + " under "
                        + hierarchy.getUniqueName() + " in cube " + cube.getName() + ", but the member belongs to "
                        + member.getHierarchy().getUniqueName());
            }
            members.add(Map.entry(member, AccessMember.valueOf(memberGrant.access().name())));
        }
        return members;
    }

    /**
     * The grants on database schemas, tables and columns, bound to the schemas the catalog reports by name. An art
     * that reads no tables reports none and has nothing a grant on them could bind to; its role carries the OLAP
     * grants only.
     */
    private void databaseGrants(RoleImpl role, RoleDef roleDef) {
        List<? extends org.eclipse.daanse.olap.api.element.db.DatabaseSchema> schemas = catalog.getDatabaseSchemas();
        if (roleDef.databaseGrants().isEmpty() || schemas == null || schemas.isEmpty()) {
            return;
        }
        for (Grants.DatabaseSchemaGrant schemaGrant : roleDef.databaseGrants()) {
            String schemaName = schemaGrant.schema().name();
            var schema = schemas.stream().filter(s -> sameName(s.getName(), schemaName)).findFirst()
                    .orElseThrow(() -> Util.newError("Unknown databaseSchema '" + schemaName + "'"));
            role.grant(schema, org.eclipse.daanse.olap.api.access.db.AccessDatabaseSchema
                    .valueOf(schemaGrant.access().name()));
            for (Grants.TableGrant tableGrant : schemaGrant.tables()) {
                String tableName = tableGrant.table().name();
                var table = schema.getDbTables().stream().filter(t -> sameName(t.getName(), tableName)).findFirst()
                        .orElseThrow(() -> Util.newError("Unknown table '" + tableName + "'"));
                role.grant(table, org.eclipse.daanse.olap.api.access.db.AccessDatabaseTable
                        .valueOf(tableGrant.access().name()));
                for (Grants.ColumnGrant columnGrant : tableGrant.columns()) {
                    String columnName = columnGrant.column().name();
                    var column = table.getDbColumns().stream().filter(c -> sameName(c.getName(), columnName))
                            .findFirst().orElseThrow(() -> Util.newError("Unknown column '" + columnName + "'"));
                    role.grant(column, org.eclipse.daanse.olap.api.access.db.AccessDatabaseColumn
                            .valueOf(columnGrant.access().name()));
                }
            }
        }
    }

    /** Names as the database matches them; a schema without a name is the one without a name. */
    private static boolean sameName(String a, String b) {
        boolean blankA = a == null || a.isBlank();
        boolean blankB = b == null || b.isBlank();
        return blankA || blankB ? blankA == blankB : a.equalsIgnoreCase(b);
    }

    private static <T> T lookup(CatalogReader reader, Cube cube, String mdx, Class<T> kind, RoleDef roleDef) {
        OlapElement element = reader.lookupCompound(cube, Util.parseIdentifier(mdx), false, DataType.UNKNOWN);
        // [Geo] names the dimension first; a grant on a hierarchy means its default hierarchy
        if (element instanceof Dimension dimension && kind == Hierarchy.class) {
            element = dimension.getHierarchy();
        }
        if (!kind.isInstance(element)) {
            throw new IllegalStateException("Role " + roleDef.name() + " names " + mdx + " in cube " + cube.getName()
                    + ", which is no " + kind.getSimpleName().toLowerCase(Locale.ROOT) + " there");
        }
        return kind.cast(element);
    }
}
