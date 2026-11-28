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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.findings.FindingException;
import org.eclipse.daanse.olap.findings.Place;
import org.eclipse.daanse.olap.metadata.DescriptionRules;
import org.eclipse.daanse.olap.metadata.Olap;
import org.eclipse.daanse.olap.metadata.model.CatalogDef;
import org.eclipse.daanse.olap.metadata.model.CubeDef;
import org.eclipse.daanse.olap.metadata.model.DimensionDef;
import org.eclipse.daanse.olap.metadata.model.ElementInfo;
import org.eclipse.daanse.olap.metadata.model.FunctionDef;
import org.eclipse.daanse.olap.metadata.model.NamedSetDef;
import org.eclipse.daanse.olap.metadata.model.ParameterDef;
import org.eclipse.daanse.olap.metadata.model.PhysicalCubeDef;
import org.eclipse.daanse.olap.metadata.model.RoleDef;
import org.eclipse.daanse.olap.metadata.model.ValueKind;

/**
 * Builds a catalog. The order of the calls does not matter: a cube may use a shared dimension
 * declared after it, a virtual cube a cube declared after it. {@link #build()} resolves the
 * names and reports everything that is wrong at once.
 */
public final class CatalogBuilder extends ElementBuilder<CatalogBuilder> {

    private record PendingUnion(String name, List<String> roles) {
    }

    private final List<Finding> problems = new ArrayList<>();
    private final Place path;
    private final List<DimensionBuilder> dimensions = new ArrayList<>();
    private final List<AbstractCubeBuilder<?>> cubes = new ArrayList<>();
    private final List<RoleBuilder> roles = new ArrayList<>();
    private final List<RoleDef> readyRoles = new ArrayList<>();
    private final List<PendingUnion> unions = new ArrayList<>();
    private final List<NamedSetDef> namedSets = new ArrayList<>();
    private final List<org.eclipse.daanse.olap.metadata.relational.DatabaseSchemaDef> schemas = new ArrayList<>();

    /**
     * A database schema the catalog's tables lie in; build it with
     * {@code DatabaseSchemaDef.build(name, s -> s.table(...))}.
     */
    public CatalogBuilder schema(org.eclipse.daanse.olap.metadata.relational.DatabaseSchemaDef schema) {
        schemas.add(schema);
        return this;
    }
    private final List<ParameterDef> parameters = new ArrayList<>();
    private final List<FunctionDef> functions = new ArrayList<>();
    private String defaultRole;

    public CatalogBuilder(String name) {
        super(name);
        this.path = Place.of("catalog", name);
    }

    /** A set every cube of the catalog knows by name. */
    public CatalogBuilder namedSet(String setName, String formula) {
        return namedSet(setName, formula, null);
    }

    /** A set every cube of the catalog knows by name, in a folder a client may show it in. */
    public CatalogBuilder namedSet(String setName, String formula, String displayFolder) {
        namedSets.add(new NamedSetDef(setName, formula, AbstractCubeBuilder.folder(displayFolder), ElementInfo.DEFAULT));
        return this;
    }

    /** A dimension cubes use by name. */
    public CatalogBuilder dimension(String dimensionName, Consumer<DimensionBuilder> dimension) {
        DimensionBuilder builder = new DimensionBuilder(dimensionName, problems, path);
        dimension.accept(builder);
        dimensions.add(builder);
        return this;
    }

    public CatalogBuilder cube(String cubeName, Consumer<CubeBuilder> cube) {
        CubeBuilder builder = new CubeBuilder(cubeName, problems, path);
        cube.accept(builder);
        cubes.add(builder);
        return this;
    }

    public CatalogBuilder virtualCube(String cubeName, Consumer<VirtualCubeBuilder> cube) {
        VirtualCubeBuilder builder = new VirtualCubeBuilder(cubeName, problems, path);
        cube.accept(builder);
        cubes.add(builder);
        return this;
    }

    public CatalogBuilder role(String roleName, Consumer<RoleBuilder> role) {
        RoleBuilder builder = new RoleBuilder(roleName);
        role.accept(builder);
        roles.add(builder);
        return this;
    }

    /**
     * A role made elsewhere -- by a connector that translates the roles of another catalog format -- taken as it is,
     * with the roles a union of it combines.
     */
    public CatalogBuilder role(RoleDef role) {
        readyRoles.add(role);
        return this;
    }

    /** A role that sees what any of the named roles sees. */
    public CatalogBuilder unionRole(String roleName, String... roleNames) {
        unions.add(new PendingUnion(roleName, List.of(roleNames)));
        return this;
    }

    /** The role of a connection that names none; without it such a connection sees everything. */
    public CatalogBuilder defaultRole(String roleName) {
        this.defaultRole = roleName;
        return this;
    }

    /** A parameter made elsewhere -- by a connector -- taken as it is, with its description. */
    public CatalogBuilder parameter(ParameterDef parameter) {
        parameters.add(parameter);
        return this;
    }

    /** A value parameter. */
    public CatalogBuilder parameter(String parameterName, ValueKind kind, String defaultValue, boolean modifiable) {
        parameters.add(new ParameterDef(parameterName, kind, defaultValue, modifiable, ElementInfo.DEFAULT));
        return this;
    }

    /**
     * A parameter whose value is a member of this hierarchy. Its default is MDX worked out
     * in the cube of each query: a cube without the hierarchy refuses it as not found.
     *
     * @param hierarchy    the hierarchy, as {@code [Product].[Product]}
     * @param defaultValue MDX text of the default member
     */
    public CatalogBuilder memberParameter(String parameterName, String hierarchy, String defaultValue,
            boolean modifiable) {
        parameters.add(new ParameterDef(parameterName, null, defaultValue, modifiable, ElementInfo.DEFAULT, hierarchy));
        return this;
    }

    /**
     * An MDX function written in Java, over scalar arguments.
     *
     * @param body gets the evaluated arguments, numbers as {@link Double}, an empty cell as {@code null}
     */
    public CatalogBuilder function(String functionName, ValueKind returns, List<ValueKind> parameterKinds,
            java.util.function.Function<Object[], Object> body) {
        functions.add(new FunctionDef(functionName, returns, parameterKinds, body, null));
        return this;
    }

    /** A numeric function of one number; an empty argument gives an empty cell. */
    public CatalogBuilder function(String functionName, java.util.function.DoubleUnaryOperator body) {
        return function(functionName, ValueKind.NUMERIC, List.of(ValueKind.NUMERIC),
                a -> a[0] == null ? null : body.applyAsDouble(((Number) a[0]).doubleValue()));
    }

    /** A numeric function of two numbers; an empty argument gives an empty cell. */
    public CatalogBuilder function(String functionName, java.util.function.DoubleBinaryOperator body) {
        return function(functionName, ValueKind.NUMERIC, List.of(ValueKind.NUMERIC, ValueKind.NUMERIC),
                a -> a[0] == null || a[1] == null ? null
                        : body.applyAsDouble(((Number) a[0]).doubleValue(), ((Number) a[1]).doubleValue()));
    }

    /**
     * The catalog.
     *
     * @throws FindingException when the description is not valid, with every finding
     */
    public CatalogDef build() {
        Map<String, DimensionDef> shared = new LinkedHashMap<>();
        for (DimensionBuilder dimension : dimensions) {
            shared.putIfAbsent(dimension.name, dimension.build());
        }
        List<DimensionDef> sharedList = dimensions.stream().map(d -> shared.get(d.name)).distinct().toList();
        if (sharedList.size() != dimensions.size()) {
            // the message names the dimension; the first declaration is the one that counts
            java.util.Set<String> seen = new java.util.HashSet<>();
            java.util.Set<String> twice = new java.util.LinkedHashSet<>();
            dimensions.forEach(d -> {
                if (!seen.add(d.name)) {
                    twice.add(d.name);
                }
            });
            problems.add(BuildFindings.of(DescriptionRules.M099, path, "declares the shared dimension "
                    + String.join(", ", twice) + " more than once; the first declaration counts"));
        }

        Map<String, PhysicalCubeDef> physical = new LinkedHashMap<>();
        Map<AbstractCubeBuilder<?>, CubeDef> built = new LinkedHashMap<>();
        for (AbstractCubeBuilder<?> cube : cubes) {
            if (cube instanceof CubeBuilder physicalBuilder) {
                PhysicalCubeDef def = physicalBuilder.build(shared::get);
                built.put(cube, def);
                physical.putIfAbsent(def.name(), def);
            }
        }
        for (AbstractCubeBuilder<?> cube : cubes) {
            if (cube instanceof VirtualCubeBuilder virtualBuilder) {
                // a cube is named in any case, as MDX names it
                built.put(cube, virtualBuilder.build(shared::get, name -> physical.entrySet().stream()
                        .filter(e -> e.getKey().equalsIgnoreCase(name)).map(java.util.Map.Entry::getValue).findFirst().orElse(null)));
            }
        }
        List<CubeDef> cubeList = cubes.stream().map(built::get).toList();

        Map<String, RoleDef> roleByName = new LinkedHashMap<>();
        List<RoleDef> roleList = new ArrayList<>();
        for (RoleBuilder role : roles) {
            RoleDef def = new RoleDef(role.name, role.catalogAccess, role.cubeGrants, List.of(), role.info(),
                    role.databaseGrants);
            roleList.add(def);
            roleByName.putIfAbsent(def.name(), def);
        }
        for (RoleDef ready : readyRoles) {
            roleList.add(ready);
            roleByName.putIfAbsent(ready.name(), ready);
        }
        // A union may name a union declared after it: they are resolved in rounds, each building the unions
        // whose parts are known; what is left names a role nobody declares, or a cycle.
        Map<PendingUnion, RoleDef> resolved = new java.util.HashMap<>();
        boolean progress = true;
        while (progress) {
            progress = false;
            for (PendingUnion union : unions) {
                if (resolved.containsKey(union)
                        || !union.roles().stream().allMatch(roleByName::containsKey)) {
                    continue;
                }
                List<RoleDef> parts = union.roles().stream().map(roleByName::get).toList();
                RoleDef def = new RoleDef(union.name(),
                        org.eclipse.daanse.olap.metadata.model.Access.Catalog.NONE, List.of(), parts,
                        ElementInfo.DEFAULT);
                resolved.put(union, def);
                roleByName.putIfAbsent(def.name(), def);
                progress = true;
            }
        }
        for (PendingUnion union : unions) {
            RoleDef def = resolved.get(union);
            if (def == null) {
                List<RoleDef> parts = new ArrayList<>();
                for (String part : union.roles()) {
                    RoleDef role = roleByName.get(part);
                    if (role == null) {
                        problems.add(BuildFindings.of(DescriptionRules.M100, path.then("role", union.name()),
                                "combines role " + part + ", which the catalog does not declare"));
                    } else {
                        parts.add(role);
                    }
                }
                def = new RoleDef(union.name(), org.eclipse.daanse.olap.metadata.model.Access.Catalog.NONE,
                        List.of(), parts, ElementInfo.DEFAULT);
                roleByName.putIfAbsent(def.name(), def);
            }
            roleList.add(def);
        }

        CatalogDef catalog = new CatalogDef(name, sharedList, cubeList, List.copyOf(namedSets), roleList,
                defaultRole, parameters, functions, info(), schemas);
        List<Finding> all = new ArrayList<>(problems);
        all.addAll(Olap.validate(catalog));
        if (!all.isEmpty()) {
            throw new FindingException(all);
        }
        return catalog;
    }
}
