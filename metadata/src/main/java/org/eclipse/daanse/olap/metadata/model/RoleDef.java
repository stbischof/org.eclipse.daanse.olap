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

/** A role. Identity: a union role points at the roles it combines. */
public final class RoleDef {

    private final String name;
    private final Access.Catalog catalogAccess;
    private final List<Grants.CubeGrant> cubeGrants;
    private final List<RoleDef> unionOf;
    private final ElementInfo info;
    private final List<Grants.DatabaseSchemaGrant> databaseGrants;

    public RoleDef(String name, Access.Catalog catalogAccess, List<Grants.CubeGrant> cubeGrants,
            List<RoleDef> unionOf, ElementInfo info) {
        this(name, catalogAccess, cubeGrants, unionOf, info, List.of());
    }

    public RoleDef(String name, Access.Catalog catalogAccess, List<Grants.CubeGrant> cubeGrants,
            List<RoleDef> unionOf, ElementInfo info, List<Grants.DatabaseSchemaGrant> databaseGrants) {
        this.databaseGrants = List.copyOf(databaseGrants);
        this.name = name;
        this.catalogAccess = catalogAccess;
        this.cubeGrants = List.copyOf(cubeGrants);
        this.unionOf = List.copyOf(unionOf);
        this.info = info;
    }

    public String name() {
        return name;
    }

    public Access.Catalog catalogAccess() {
        return catalogAccess;
    }

    /** What the role may read of the database schemas; empty for a role that says nothing about them. */
    public List<Grants.DatabaseSchemaGrant> databaseGrants() {
        return databaseGrants;
    }

    public List<Grants.CubeGrant> cubeGrants() {
        return cubeGrants;
    }

    /** The roles this one combines; empty for a role with grants of its own. */
    public List<RoleDef> unionOf() {
        return unionOf;
    }

    public ElementInfo info() {
        return info;
    }

    @Override
    public String toString() {
        return "role " + name;
    }
}
