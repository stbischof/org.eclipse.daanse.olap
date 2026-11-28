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
import java.util.Optional;

/** A catalog: its cubes and what they share. Identity. */
public final class CatalogDef {

    private final String name;
    private final List<DimensionDef> sharedDimensions;
    private final List<CubeDef> cubes;
    private final List<NamedSetDef> namedSets;
    private final List<RoleDef> roles;
    private final String defaultRole;
    private final List<ParameterDef> parameters;
    private final List<FunctionDef> functions;
    private final ElementInfo info;
    private final List<org.eclipse.daanse.olap.metadata.relational.DatabaseSchemaDef> schemas;

    public CatalogDef(String name, List<DimensionDef> sharedDimensions, List<CubeDef> cubes,
            List<NamedSetDef> namedSets, List<RoleDef> roles,
            String defaultRole, List<ParameterDef> parameters, List<FunctionDef> functions, ElementInfo info) {
        this(name, sharedDimensions, cubes, namedSets, roles, defaultRole, parameters, functions, info, List.of());
    }

    public CatalogDef(String name, List<DimensionDef> sharedDimensions, List<CubeDef> cubes,
            List<NamedSetDef> namedSets, List<RoleDef> roles, String defaultRole, List<ParameterDef> parameters,
            List<FunctionDef> functions, ElementInfo info, List<org.eclipse.daanse.olap.metadata.relational.DatabaseSchemaDef> schemas) {
        this.schemas = List.copyOf(schemas);
        this.name = name;
        this.sharedDimensions = List.copyOf(sharedDimensions);
        this.namedSets = List.copyOf(namedSets);
        this.cubes = List.copyOf(cubes);
        this.roles = List.copyOf(roles);
        this.defaultRole = defaultRole;
        this.parameters = List.copyOf(parameters);
        this.functions = List.copyOf(functions);
        this.info = info;
    }

    /** The database schemas the catalog's tables lie in; empty for a catalog that reads no tables. */
    public List<org.eclipse.daanse.olap.metadata.relational.DatabaseSchemaDef> schemas() {
        return schemas;
    }

    public String name() {
        return name;
    }

    /**
     * The dimensions declared on the catalog, for cubes to use by name.
     *
     * <p>Two cubes share a dimension by pointing at the same {@link DimensionDef}, not by being
     * listed here, so this list is the declaration site: it names a dimension once, gets its name
     * checked for being unique, and gives a source its dimensions without walking every cube.
     */
    public List<DimensionDef> sharedDimensions() {
        return sharedDimensions;
    }

    /**
     * The sets the catalog knows by name, above every cube. A query on any cube may name them;
     * a set of the same name on a cube comes first.
     */
    public List<NamedSetDef> namedSets() {
        return namedSets;
    }

    public List<CubeDef> cubes() {
        return cubes;
    }

    public Optional<CubeDef> cube(String cubeName) {
        return cubes.stream().filter(c -> c.name().equals(cubeName)).findFirst();
    }

    public List<RoleDef> roles() {
        return roles;
    }

    /** The name of the role of a connection that names none; {@code null} for full access. */
    public String defaultRole() {
        return defaultRole;
    }

    public List<ParameterDef> parameters() {
        return parameters;
    }

    /** The MDX functions this catalog brings along. */
    public List<FunctionDef> functions() {
        return functions;
    }

    public ElementInfo info() {
        return info;
    }

    @Override
    public String toString() {
        return "catalog " + name;
    }
}
