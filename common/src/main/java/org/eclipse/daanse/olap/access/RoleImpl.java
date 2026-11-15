/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2002-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 * 
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.daanse.olap.api.access.AccessCatalog;
import org.eclipse.daanse.olap.api.access.AccessCube;
import org.eclipse.daanse.olap.api.access.AccessDimension;
import org.eclipse.daanse.olap.api.access.AccessHierarchy;
import org.eclipse.daanse.olap.api.access.AccessMember;
import org.eclipse.daanse.olap.api.access.AllHierarchyAccess;
import org.eclipse.daanse.olap.api.access.HierarchyAccess;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.access.RollupPolicy;
import org.eclipse.daanse.olap.api.access.db.AccessDatabaseColumn;
import org.eclipse.daanse.olap.api.access.db.AccessDatabaseSchema;
import org.eclipse.daanse.olap.api.access.db.AccessDatabaseTable;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.element.db.DatabaseColumn;
import org.eclipse.daanse.olap.api.element.db.DatabaseSchema;
import org.eclipse.daanse.olap.api.element.db.DatabaseTable;
import org.eclipse.daanse.olap.common.ExecutionConfig;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.element.OlapElementBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.compatibility.Access;

/**
 * Default implementation of the {@link Role} interface.
 *
 * @author jhyde, lucboudreau
 * @since Oct 5, 2002
 */
public class RoleImpl implements Role {
    private boolean mutable = true;
    private final Map<Catalog, AccessCatalog> schemaGrants =
        new HashMap<>();
    private final Map<Cube, AccessCube> cubeGrants =
        new HashMap<>();
    private final Map<Dimension, AccessDimension> dimensionGrants =
        new HashMap<>();
    private final Map<Hierarchy, HierarchyAccessImpl> hierarchyGrants =
        new HashMap<>();
    private final Map<DatabaseSchema, AccessDatabaseSchema> databaseSchemaGrants =
            new HashMap<>();
    private final Map<DatabaseTable, AccessDatabaseTable> tableGrants =
            new HashMap<>();
    private final Map<DatabaseColumn, AccessDatabaseColumn> columnGrants =
            new HashMap<>();

    private static final Logger LOGGER =
        LoggerFactory.getLogger(RoleImpl.class);
    private final List<Object[]> hashCache = new ArrayList<>();
    private int hash = 0;

    /**
     * Returns a role which has access to everything.
     * @param schema A schema to bind this role to.
     * @return A role with root access to the schema.
     */
    public static Role createRootRole(Catalog schema) {
        RoleImpl role = new RoleImpl();
        role.grant(schema, AccessCatalog.ALL);
        role.makeImmutable();
        return role;
    }

    /**
     * Creates a role with no permissions.
     */
    public RoleImpl() {
    }

    protected RoleImpl(RoleImpl role) {
        mutable = role.mutable;
        schemaGrants.putAll(role.schemaGrants);
        cubeGrants.putAll(role.cubeGrants);
        dimensionGrants.putAll(role.dimensionGrants);
        hashCache.addAll(role.hashCache);
        for (Map.Entry<Hierarchy, HierarchyAccessImpl> entry
            : role.hierarchyGrants.entrySet())
        {
            hierarchyGrants.put(
                entry.getKey(),
                new HierarchyAccessImpl(entry.getValue()));
        }
    }

    @Override
	public int hashCode() {
        // Although this code isn't entirely thread safe, it is good enough.
        // The implementations of Role are not expected to be thread safe,
        // but are only to be immutable once isMutable() returns true.
        //
        // Role objects are only often hashed for tuple list caches and only
        // once per mondrian schema per mondrian instance. If heavier usage
        // is added, this should probably be refactored into something more
        // thread safe with a ReentrantReadWriteLock.
        if (hash == 0) {
            int tmpHash = 7;
            for (Object obj : hashCache) {
                tmpHash = Util.hash(tmpHash, obj);
            }
            hash = tmpHash;
        }
        return hash;
    }

    @Override
	public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RoleImpl r)) {
            return false;
        }
        return r.hashCache.equals(this.hashCache);
    }

    /**
     * Returns a copy of this Role which can be modified.
     */
    public RoleImpl makeMutableClone() {
        RoleImpl role = new RoleImpl(this);
        role.mutable = true;
        return role;
    }

    /**
     * Prevents any further modifications.
     *  !isMutable()
     */
    public void makeImmutable() {
        mutable = false;
    }

    /**
     * Returns whether modifications are possible.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Defines access to all cubes and dimensions in a schema.
     *
     * @param catalog Catalog whose access to grant/deny.
     * @param access An Access access code
     *
     *  schema != null
     *  access == Access.ALL || access == Access.NONE
     * || access == Access.ALL_DIMENSIONS
     *  isMutable()
     */
    public void grant(Catalog catalog, AccessCatalog access) {
        checkCatalog(catalog);
        assert isMutable();
        schemaGrants.put(catalog, access);
        hashCache.add(
            new Object[] {
                catalog.getId(),
                access.name()});
        hash = 0;
    }

    public void grant(DatabaseSchema databaseSchema, AccessDatabaseSchema access) {
        checkDatabaseSchema(databaseSchema);
        assert isMutable();
        databaseSchemaGrants.put(databaseSchema, access);
        hashCache.add(
            new Object[] {
                databaseSchema.getName(),
                access.name()});
        hash = 0;
    }


    public void grant(DatabaseTable table, AccessDatabaseTable access) {
        checkTable(table);
        assert isMutable();
        tableGrants.put(table, access);
        hashCache.add(
            new Object[] {
                table.getName(),
                access.name()});
        hash = 0;
    }

    public void grant(DatabaseColumn column, AccessDatabaseColumn access) {
        checkColumn(column);
        assert isMutable();
        columnGrants.put(column, access);
        hashCache.add(
            new Object[] {
                    column.getName(),
                access.name()});
        hash = 0;
    }

    @Override
	public AccessCatalog getAccess(Catalog schema) {
        checkCatalog(schema);
        final AccessCatalog schemaAccess = schemaGrants.get(schema);
        if (schemaAccess == null) {
            // No specific rules means full access.
            return AccessCatalog.CUSTOM;
        } else {
            return schemaAccess;
        }
    }

    /**
     * Converts a null Access object to {@link Access#NONE}.
     *
     * @param access Access object or null
     * @return Access object, never null
     */
    private static AccessCube toAccessCube(AccessCube access) {
        return access == null ? AccessCube.NONE : access;
    }

    private static AccessDimension toAccessDimension(AccessDimension access) {
        return access == null ? AccessDimension.NONE : access;
    }

    private static AccessMember toAccessMember(AccessMember access) {
        return access == null ? AccessMember.NONE : access;
    }

    /**
     * Defines access to a cube.
     *
     * @param cube Cube whose access to grant/deny.
     * @param access An Access access code
     *
     *  cube != null
     *  access == Access.ALL || access == Access.NONE
     *  isMutable()
     */
    public void grant(Cube cube, AccessCube access) {
        Util.assertPrecondition(cube != null, "cube != null");
        checkAccessCube(access);
        Util.assertPrecondition(isMutable(), "isMutable()");
        LOGGER.trace(
            "Grant {} on cube {}", access, cube.getName());
        cubeGrants.put(cube, access);
        // Set the catalogs's access to 'custom' if no rules already exist.
        final AccessCatalog catalogAccess =
            getAccess(cube.getCatalog());
        if (catalogAccess == AccessCatalog.NONE) {
            LOGGER.trace(
                "Cascading grant {} on schema {}",
                AccessCatalog.CUSTOM, cube.getCatalog().getName());
            grant(cube.getCatalog(), AccessCatalog.CUSTOM);
        }
        hashCache.add(
            new Object[] {
                cube.getClass().getName(),
                cube.getName(),
                access.name()});
        hash = 0;
    }

    @Override
	public AccessCube getAccess(Cube cube) {
        checkCube(cube);
        // Check for explicit rules.
        // Both 'custom' and 'all' are good enough
        AccessCube access = cubeGrants.get(cube);
        if (access != null) {
            LOGGER.trace(
                "Access level {} granted to cube {}", access, cube.getName());
            return access;
        }
        // Check for inheritance from the parent schema
        // 'All Dimensions' and 'custom' are not good enough
        AccessCatalog accessCatalog = schemaGrants.get(cube.getCatalog());
        if (accessCatalog == AccessCatalog.ALL) {
            LOGGER.trace(
                "Access level {} granted to cube {} because of the grant to schema {}",
                access,
                cube.getName(),
                cube.getCatalog().getName());
            return AccessCube.ALL;
        }
        // Deny access
        LOGGER.trace(
            "Access denied to cube {}", cube.getName());
        return AccessCube.NONE;
    }

    /**
     * Defines access to a dimension.
     *
     * @param dimension Dimension whose access to grant/deny.
     * @param access An Access instance
     *
     *  dimension != null
     *  access == Access.ALL || access == Access.CUSTOM
     * || access == Access.NONE
     *  isMutable()
     */
    public void grant(Dimension dimension, AccessDimension access) {
        checkDimension(dimension);
        checkAccessDimension(access);
        Util.assertPrecondition(isMutable(), "isMutable()");
        LOGGER.trace(
            "Grant {} on dimension {}", access,  dimension.getUniqueName());
        dimensionGrants.put(dimension, access);
        // Dimension grants do not cascade to the parent cube automatically.
        // We always figure out the inheritance at runtime since the place
        // where the dimension is used (either inside of a virtual cube,
        // a shared dimension or a cube) will influence on the decision.
        hashCache.add(
            new Object[] {
                dimension.getClass().getName(),
                dimension.getName(),
                access.name()});
        hash = 0;
    }

    @Override
	public AccessDimension getAccess(Dimension dimension) {
        checkDimension(dimension);
        // Check for explicit rules.
        AccessDimension access = getDimensionGrant(dimension);
        if (access == AccessDimension.CUSTOM) {
            // For legacy reasons, if there are no accessible hierarchies
            // and the dimension has an access level of custom, we deny.
            // TODO Remove for Mondrian 4.0
            boolean canAccess = false;
            for (Hierarchy hierarchy : dimension.getHierarchies()) {
                final HierarchyAccessImpl hierarchyAccess =
                    hierarchyGrants.get(hierarchy);
                if (hierarchyAccess != null
                    && hierarchyAccess.access != AccessHierarchy.NONE)
                {
                    canAccess = true;
                }
            }
            if (canAccess) {
                LOGGER.trace(
                    "Access level {} granted to dimension {} because of the grant to one of its hierarchy.",
                    access, dimension.getUniqueName());
                return AccessDimension.CUSTOM;
            } else {
                LOGGER.trace(
                    "Access denied to dimension {} because there are no hierarchies accessible.", dimension.getUniqueName());
                return AccessDimension.NONE;
            }
        } else if (access != null) {
            LOGGER.trace(
                "Access level {} granted to dimension {} because of explicit access rights.",
                access, dimension.getUniqueName());
            return access;
        }
        // Check if this dimension inherits the cube's access rights.
        // 'Custom' level is not good enough for inherited access.
        AccessCube accessCube = checkDimensionAccessByCubeInheritance(dimension);
        if (accessCube != AccessCube.NONE) {
            LOGGER.trace(
                "Access level {} granted to dimension {} because of the grant to its parent cube.",
                access, dimension.getUniqueName());
            return AccessUtil.getAccessDimension(accessCube);
        }
        // Check access at the schema level.
        // Levels of 'custom' and 'none' are not good enough.
        switch (getAccess(dimension.getCatalog())) {
        case ALL:
            LOGGER.trace(
                "Access level ALL granted to dimension {} because of the grant to schema {}",
                dimension.getUniqueName(), dimension.getCatalog().getName());
            return AccessDimension.ALL;
        case ALL_DIMENSIONS:
            // For all_dimensions to work, the cube access must be
            // at least 'custom' level
            return AccessDimension.NONE;
        default:
            LOGGER.trace(
                "Access denied to dimension {} because of the access level of schema {}",
                dimension.getUniqueName(),
                dimension.getCatalog().getName());
            return AccessDimension.NONE;
        }
    }

    private AccessDimension getDimensionGrant(final Dimension dimension) {
        if (dimension.isMeasures()) {
            for (Map.Entry<Dimension, AccessDimension> entry : dimensionGrants.entrySet()) {
                Dimension key = entry.getKey();
                if (key == dimension) {
                    return dimensionGrants.get(key);
                }
            }
            return null;
        } else {
            return dimensionGrants.get(dimension);
        }
    }

    /**
     * This method is used to check if the access rights over a dimension
     * that might be inherited from the parent cube.
     * It only checks for inherited access, and it presumes that there
     * are no dimension grants currently given to the dimension passed as an
     * argument.
     */
    private AccessCube checkDimensionAccessByCubeInheritance(Dimension dimension) {
        assert !dimensionGrants.containsKey(dimension)
               || dimension.isMeasures();
        for (Map.Entry<Cube, AccessCube> cubeGrant : cubeGrants.entrySet()) {
            final AccessCube access = toAccessCube(cubeGrant.getValue());
            // The 'none' and 'custom' access level are not good enough
            if (access == AccessCube.NONE || access == AccessCube.CUSTOM) {
                continue;
            }
            final List<? extends Dimension> dimensions = cubeGrant.getKey().getDimensions();
            for (Dimension dimension1 : dimensions) {
                // If the dimensions have the same identity,
                // we found an access rule.
                if (dimension == dimension1) {
                    return cubeGrant.getValue();
                }
                // If the passed dimension argument is of class
                // RolapCubeDimension, we must validate the cube
                // assignment and make sure the cubes are the same.
                // If not, skip to the next grant.
                if (dimension.equals(dimension1)
                    && dimension.getCube() != null
                    && dimension.getCube() instanceof OlapElementBase oeb 
                    && !oeb.equalsOlapElement(cubeGrant.getKey()))
                {
                    continue;
                }
                // Last thing is to allow for equality correspondences
                // to work with virtual cubes.
                if (cubeGrant.getKey() instanceof Cube keyed && keyed.drawsFromOtherCubes()
                    && dimension.equals(dimension1))
                {
                    return cubeGrant.getValue();
                }
            }
        }
        return AccessCube.NONE;
    }

    /**
     * Defines access to a hierarchy.
     *
     * @param hierarchy Hierarchy whose access to grant/deny.
     * @param access An Access access code
     * @param topLevel Top-most level which can be accessed, or null if the
     *     highest level. May only be specified if access is
     *    org.eclipse.daanse.olap.api.access.Access#CUSTOM.
     * @param bottomLevel Bottom-most level which can be accessed, or null if
     *     the lowest level. May only be specified if access is
     *    org.eclipse.daanse.olap.api.access.Access#CUSTOM.
     *
     * @param rollupPolicy Rollup policy
     *
     *  hierarchy != null
     *  Access.instance().isValid(access)
     *  (access == Access.CUSTOM)
     *      || (topLevel == null &amp;&amp; bottomLevel == null)
     *  topLevel == null || topLevel.getHierarchy() == hierarchy
     *  bottomLevel == null || bottomLevel.getHierarchy() == hierarchy
     *  isMutable()
     */
    public void grant(
        Hierarchy hierarchy,
        AccessHierarchy access,
        Level topLevel,
        Level bottomLevel,
        RollupPolicy rollupPolicy)
    {
        assert hierarchy != null;
        assert access != null;
        assert (access == AccessHierarchy.CUSTOM)
            || (topLevel == null && bottomLevel == null);
        assert topLevel == null || topLevel.getHierarchy() == hierarchy;
        assert bottomLevel == null || bottomLevel.getHierarchy() == hierarchy;
        assert isMutable();
        assert rollupPolicy != null;
        LOGGER.trace(
            "Granting access {} on hierarchy {}",
            access, hierarchy.getUniqueName());
        hierarchyGrants.put(
            hierarchy,
            new HierarchyAccessImpl(
                this, hierarchy, access, topLevel, bottomLevel, rollupPolicy));
        // Cascade the access right to 'custom' on the parent dim if necessary
        final AccessDimension dimAccess =
            toAccessDimension(dimensionGrants.get(hierarchy.getDimension()));
        if (dimAccess == AccessDimension.NONE) {
            LOGGER.trace(
                "Cascading grant CUSTOM on dimension {} because of the grant to hierarchy {}",
                hierarchy.getDimension().getUniqueName(), hierarchy.getUniqueName());
            grant(hierarchy.getDimension(), AccessDimension.CUSTOM);
        }
        hashCache.add(
            new Object[] {
                hierarchy.getClass().getName(),
                hierarchy.getName(),
                access.name()});
        hash = 0;
    }

    @Override
	public AccessHierarchy getAccess(Hierarchy hierarchy) {
        if (hierarchy == null) {
            throw new IllegalArgumentException("hierarchy should not be null");
        }
        HierarchyAccessImpl hierarchyAccess = hierarchyGrants.get(hierarchy);
        if (hierarchyAccess != null) {
            LOGGER.trace(
                "Access level {} granted to dimension {}", hierarchyAccess.access, hierarchy.getUniqueName());
            return hierarchyAccess.access;
        }
        // There was no explicit rule for this particular hierarchy.
        // Let's check the parent dimension.
        AccessDimension access = getAccess(hierarchy.getDimension());
        if (access == AccessDimension.ALL) {
            // Access levels of 'none' and 'custom' are not enough.
            LOGGER.trace(
                "Access level ALL  granted to hierarchy {} because of the grant to dimension {}",
                hierarchy.getUniqueName(),
                hierarchy.getDimension().getUniqueName());
            return AccessHierarchy.ALL;
        }
        // Access denied, since we know that the dimension check has
        // checked for its parents as well.
        LOGGER.trace(
            "Access denied to hierarchy {}", hierarchy.getUniqueName());
        return AccessHierarchy.NONE;
    }

    @Override
    public AccessDatabaseSchema getAccess(DatabaseSchema databaseSchema, Catalog catalog) {
        checkDatabaseSchema(databaseSchema);
        AccessDatabaseSchema access = databaseSchemaGrants.get(databaseSchema);
        if (access != null) {
            LOGGER.trace(
                "Access level {} granted to database schema {}", access, databaseSchema.getName());
            return access;
        }
        AccessCatalog accessCatalog = schemaGrants.get(catalog);
        if (accessCatalog == AccessCatalog.ALL) {
            LOGGER.trace(
                "Access level {} granted to database schema {} because of the grant to catalog {}",
                access,
                databaseSchema.getName(),
                catalog.getName());
            return AccessDatabaseSchema.ALL;
        }
        // Deny access
        LOGGER.trace(
            "Access denied to database schema {}", databaseSchema.getName());
        return AccessDatabaseSchema.NONE;
    }

    @Override
    public AccessDatabaseTable getAccess(DatabaseTable databaseTable, AccessDatabaseSchema accessDatabaseSchema) {
        checkDatabaseTable(databaseTable);
        AccessDatabaseTable access = tableGrants.get(databaseTable);
        if (access != null) {
            LOGGER.trace(
                "Access level {} granted to database table {}", access, databaseTable.getName());
            return access;
        }
        if (accessDatabaseSchema == AccessDatabaseSchema.ALL) {
            LOGGER.trace(
                "Access level {} granted to database table {} because of the grant to database schema",
                access,
                databaseTable.getName());
            return AccessDatabaseTable.ALL;
        }
        // Deny access
        LOGGER.trace(
            "Access denied to database table {}", databaseTable.getName());
        return AccessDatabaseTable.NONE;
    }

    @Override
    public AccessDatabaseColumn getAccess(DatabaseColumn databaseColumn, AccessDatabaseTable accessDatabaseTable) {
        checkDatabaseTable(databaseColumn);
        AccessDatabaseColumn access = columnGrants.get(databaseColumn);
        if (access != null) {
            LOGGER.trace(
                "Access level {} granted to database table {}", access, databaseColumn.getName());
            return access;
        }
        if (accessDatabaseTable == AccessDatabaseTable.ALL) {
            LOGGER.trace(
                "Access level {} granted to database column {} because of the grant to database table",
                access,
                databaseColumn.getName());
            return AccessDatabaseColumn.ALL;
        }
        // Deny access
        LOGGER.trace(
            "Access denied to database column {}", databaseColumn.getName());
        return AccessDatabaseColumn.NONE;
    }

    private void checkDatabaseTable(DatabaseColumn databaseColumn) {
        if (databaseColumn == null) {
            throw new IllegalArgumentException("database column should be not null");
        }
    }

    private void checkDatabaseTable(DatabaseTable databaseTable) {
        if (databaseTable == null) {
            throw new IllegalArgumentException("database table should be not null");
        }
    }

    @Override
	public HierarchyAccess getAccessDetails(Hierarchy hierarchy) {
        Util.assertPrecondition(hierarchy != null, "hierarchy != null");
        if (hierarchyGrants.containsKey(hierarchy)) {
            return hierarchyGrants.get(hierarchy);
        }
        final AccessHierarchy hierarchyAccess;
        final AccessCatalog schemaGrant =
            schemaGrants.get(hierarchy.getDimension().getCatalog());
        if (schemaGrant != null) {
            if (schemaGrant == AccessCatalog.ALL) {
                hierarchyAccess = AccessHierarchy.ALL;
            } else {
              // Let's check the parent dimension
              AccessDimension dimAccess = getAccess( hierarchy.getDimension() );
              hierarchyAccess = dimAccess == AccessDimension.ALL ? AccessHierarchy.ALL : AccessHierarchy.NONE;
            }
        } else {
            hierarchyAccess = AccessHierarchy.ALL;
        }
        return new HierarchyAccessImpl(
            this,
            hierarchy,
            hierarchyAccess,
            null,
            null,
            RollupPolicy.HIDDEN);
    }

    @Override
	public AccessMember getAccess(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("level should not be null");
        }
        HierarchyAccessImpl hierarchyAccess =
                hierarchyGrants.get(level.getHierarchy());
        if (hierarchyAccess != null
            && hierarchyAccess.access != AccessHierarchy.NONE
            && checkLevelIsOkWithRestrictions(hierarchyAccess, level)) {
                // We're good. Let it through.
                LOGGER.trace(
                    "Access level {} granted to level {} because of the grant to hierarchy {}",
                    hierarchyAccess.access,
                    level.getUniqueName(),
                    level.getHierarchy().getUniqueName());
                return AccessUtil.getAccessMember(hierarchyAccess.access);
        }
        // No information could be deducted from the parent hierarchy.
        // Let's use the parent dimension.
        AccessDimension access =
            getAccess(level.getDimension());
        LOGGER.trace(
            "Access level {} granted to level {} because of the grant to dimension {}",
            access,
            level.getUniqueName(),
            level.getDimension().getUniqueName());
        return AccessUtil.getAccessMember(access);
    }


    private static boolean checkLevelIsOkWithRestrictions(
        HierarchyAccessImpl hierarchyAccess,
        Level level)
    {
        // Check if this level is explicitly excluded by top/bottom
        // level restrictions.
        if (level.getDepth() < hierarchyAccess.topLevel.getDepth()) {
            return false;
        }
        return  level.getDepth() <= hierarchyAccess.bottomLevel.getDepth();
    }

    /**
     * Defines access to a member in a hierarchy.
     *
     * Notes:
     * The order of grants matters. If you grant/deny access to a
     *     member, previous grants/denials to its descendants are ignored.
     * Member grants do not supersde top/bottom levels set using
     *     grant(Hierarchy, Access, Level, Level, org.eclipse.daanse.olap.api.access.MappingRole.RollupPolicy).
     * If you have access to a member, then you can see its ancestors
     *     even those explicitly denied, up to the top level.
     * 
     *
     *  member != null
     *  isMutable()
     *  getAccess(member.getHierarchy()) == Access.CUSTOM
     */
    public void grant(Member member, AccessMember access) {
        Util.assertPrecondition(member != null, "member != null");
        assert isMutable();
        if (getAccess(member.getHierarchy()) != AccessHierarchy.CUSTOM) {
            throw new IllegalArgumentException("Access should not be CUSTOM");
        }
        HierarchyAccessImpl hierarchyAccess =
            hierarchyGrants.get(member.getHierarchy());
        assert hierarchyAccess != null;
        assert hierarchyAccess.access == AccessHierarchy.CUSTOM;
        hierarchyAccess.grant(this, member, access);
        hashCache.add(
            new Object[] {
                member.getClass().getName(),
                member.getName(),
                access.name()});
        hash = 0;
    }

    @Override
	public AccessMember getAccess(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("member should be not null");
        }
        // Always allow access to calculated members.
        if (member.isCalculatedInQuery()) {
            return AccessMember.ALL;
        }
        // Check if the parent hierarchy has any access
        // rules for this.
        final HierarchyAccessImpl hierarchyAccess =
            hierarchyGrants.get(member.getHierarchy());
        if (hierarchyAccess != null) {
            return hierarchyAccess.getAccess(member);
        }
        // Then let's check ask the parent level.
        AccessMember access = getAccess(member.getLevel());
        LOGGER.trace(
            "Access level {} granted to level {} because of the grant to level {}",
            access,
            member.getUniqueName(),
            member.getLevel().getUniqueName());
        return access;
    }

    @Override
	public AccessMember getAccess(NamedSet set) {
        Util.assertPrecondition(set != null, "set != null");
        // TODO Named sets cannot be secured at the moment.
        LOGGER.trace(
            "Access level ALL  granted to NamedSet {} because I said so.", set.getUniqueName());
        return AccessMember.ALL;
    }

    @Override
	public boolean canAccess(OlapElement olapElement) {
        Util.assertPrecondition(olapElement != null, "olapElement != null");
        return switch (olapElement) {
            case Member member -> getAccess(member) != AccessMember.NONE;
            case Level level -> getAccess(level) != AccessMember.NONE;
            case NamedSet namedSet -> getAccess(namedSet) != AccessMember.NONE;
            case Hierarchy hierarchy -> getAccess(hierarchy) != AccessHierarchy.NONE;
            case Cube cube -> getAccess(cube) != AccessCube.NONE;
            case Dimension dimension -> getAccess(dimension) != AccessDimension.NONE;
            default -> false;
        };
    }

    @Override
    public boolean canAccess(DatabaseSchema databaseSchema, Catalog catalog) {
        return getAccess(databaseSchema, catalog) != AccessDatabaseSchema.NONE;
    }

    /**
     * Creates an element which represents all access to a hierarchy.
     *
     * @param hierarchy Hierarchy
     * @return element representing all access to a given hierarchy
     */
    public static HierarchyAccess createAllAccess(Hierarchy hierarchy) {
        return new HierarchyAccessImpl(
        		RoleImpl.createRootRole(hierarchy.getDimension().getCatalog()),
            hierarchy, AccessHierarchy.ALL, null, null, RollupPolicy.FULL);
    }

    /**
     * Returns a role that is the union of the given roles.
     *
     * @param roleList List of roles
     * @return Union role
     */
    public static Role union(final List<Role> roleList) {
        if (roleList.isEmpty()) {
            throw new IllegalArgumentException("roleList should be not empty");
        }
        return new UnionRoleImpl(roleList);
    }

    private void checkAccessDimension(AccessDimension access) {
        if (access != AccessDimension.ALL &&
            access != AccessDimension.NONE
            && access != AccessDimension.CUSTOM) {
            throw new IllegalArgumentException("Access should be ALL or NONE or CUSTOM");
        }
    }

    private void checkAccessCube(AccessCube access) {
        if (access != AccessCube.ALL &&
            access != AccessCube.NONE
            && access != AccessCube.CUSTOM) {
            throw new IllegalArgumentException("Access should be ALL or NONE or CUSTOM");
        }
    }

    private void checkCatalog(Catalog schema) {
        if (schema == null) {
            throw new IllegalArgumentException("schema should be not null");
        }
    }

    private void checkDatabaseSchema(DatabaseSchema schema) {
        if (schema == null) {
            throw new IllegalArgumentException("database schema should be not null");
        }
    }

    private void checkTable(DatabaseTable table) {
        if (table == null) {
            throw new IllegalArgumentException("database table should be not null");
        }
    }

    private void checkColumn(DatabaseColumn column) {
        if (column == null) {
            throw new IllegalArgumentException("database column should be not null");
        }
    }

    private void checkCube(Cube cube) {
        if (cube == null) {
            throw new IllegalArgumentException("cube should be not null");
        }
    }
    private void checkDimension(Dimension dimension) {
        if (dimension == null) {
            throw new IllegalArgumentException("dimension should be not null");
        }
    }

    // ~ Inner classes --------------------------------------------------------

    /**
     * Represents the access that a role has to a particular hierarchy.
     */
    private static class HierarchyAccessImpl implements AllHierarchyAccess
    {
        private final Hierarchy hierarchy;
        private final Level topLevel;
        private final AccessHierarchy access;
        private final Level bottomLevel;
        private final Map<String, MemberAccess> memberGrants =
            new HashMap<>();
        private final RollupPolicy rollupPolicy;
        private final Role role;

        /**
         * Creates a HierarchyAccessImpl.
         * @param role A role this access belongs to.
         * @param hierarchy A hierarchy this object describes.
         * @param access The access granted to this role for this hierarchy.
         * @param topLevel The top level to restrict the role to, or null to
         * grant access up top the top level of the hierarchy parameter.
         * @param bottomLevel The bottom level to restrict the role to, or null
         * to grant access down to the bottom level of the hierarchy parameter.
         * @param rollupPolicy The rollup policy to apply.
         */
        HierarchyAccessImpl(
            Role role,
            Hierarchy hierarchy,
            AccessHierarchy access,
            Level topLevel,
            Level bottomLevel,
            RollupPolicy rollupPolicy)
        {
            assert role != null;
            assert hierarchy != null;
            assert access != null;
            assert rollupPolicy != null;
            this.role = role;
            this.hierarchy = hierarchy;
            this.access = access;
            this.rollupPolicy = rollupPolicy;
            this.topLevel = topLevel == null
                ? hierarchy.getLevels().getFirst()
                : topLevel;
            this.bottomLevel = bottomLevel == null
                ? hierarchy.getLevels().getLast()
                : bottomLevel;
        }

        private HierarchyAccessImpl(HierarchyAccessImpl hierarchyAccess) {
            this(hierarchyAccess.role, hierarchyAccess.hierarchy,
                hierarchyAccess.access, hierarchyAccess.topLevel,
                hierarchyAccess.bottomLevel, hierarchyAccess.rollupPolicy);
            memberGrants.putAll(hierarchyAccess.memberGrants);
        }

        /**
         * Grants access to a member.
         *
         * @param member Member
         * @param access Access
         */
        void grant(RoleImpl role, Member member, AccessMember access) {
            Util.assertTrue(member.getHierarchy() == hierarchy);

            // Remove any existing grants to descendants of "member"
            for (Iterator<MemberAccess> memberIter =
                memberGrants.values().iterator(); memberIter.hasNext();)
            {
                MemberAccess mAccess = memberIter.next();
                if (mAccess.member.isChildOrEqualTo(member)) {
                    LOGGER.trace(
                        "Member grant {} removed because a grant on {} overrides it.",
                        mAccess, member.getUniqueName());
                    memberIter.remove();
                }
            }

            LOGGER.trace(
                "Granting access {} on member {}",
                access, member.getUniqueName());
            memberGrants.put(
                member.getUniqueName(),
                new MemberAccess(member, access));

            if (access == AccessMember.NONE) {
                // Since we're denying access, the ancestor's
                // access level goes from NONE to CUSTOM
                // and from ALL to RESTRICTED.
                for (Member m = member.getParentMember();
                     m != null;
                     m = m.getParentMember())
                {
                    MemberAccess mAccess =
                        memberGrants.get(m.getUniqueName());
                    final AccessMember parentAccess =
                        mAccess == null ? null : mAccess.access;
                    // If no current access is allowed, upgrade to "custom"
                    // which means nothing unless explicitly allowed.
                    if (parentAccess == AccessMember.NONE
                        && checkLevelIsOkWithRestrictions(
                            this,
                            m.getLevel()))
                    {
                        LOGGER.trace(
                            "Cascading grant CUSTOM on member {} because of the grant to member {}",
                            m.getUniqueName(),
                            member.getUniqueName());
                        memberGrants.put(
                            m.getUniqueName(),
                            new MemberAccess(m, AccessMember.CUSTOM));
                    }
                    // If the current parent's access is not defined or
                    // 'all', we switch it to RESTRICTED, meaning
                    // that the user has access to everything unless
                    // explicitly denied.
                    if ((parentAccess == null
                            || parentAccess == AccessMember.ALL)
                        && checkLevelIsOkWithRestrictions(
                            this,
                            m.getLevel()))
                    {
                        LOGGER.trace(
                            "Cascading grant RESTRICTED on member {} because of the grant to member {}",
                            m.getUniqueName(), member.getUniqueName());
                        memberGrants.put(
                            m.getUniqueName(),
                            new MemberAccess(m, AccessMember.RESTRICTED));
                    }
                }
            } else {
                // Create 'custom' access for any ancestors of 'member' which
                // do not have explicit access but which have at least one
                // child visible.
                for (Member m = member.getParentMember();
                     m != null;
                     m = m.getParentMember())
                {
                    if (checkLevelIsOkWithRestrictions(
                            this,
                            m.getLevel()))
                    {
                        MemberAccess mAccess =
                            memberGrants.get(m.getUniqueName());
                        final AccessMember parentAccess =
                            toAccessMember(mAccess == null ? null : mAccess.access);
                        if (parentAccess == AccessMember.NONE) {
                            LOGGER.trace(
                                "Cascading grant CUSTOM on member {} because of the grant to member {}",
                                m.getUniqueName(), member.getUniqueName());
                            memberGrants.put(
                                m.getUniqueName(),
                                new MemberAccess(m, AccessMember.CUSTOM));
                        }
                    }
                }
                // Also set custom access for the parent hierarchy.
                final AccessHierarchy hierarchyAccess =
                    role.getAccess(member.getLevel().getHierarchy());
                if (hierarchyAccess == AccessHierarchy.NONE) {
                    LOGGER.trace(
                        "Cascading grant CUSTOM on hierarchy {} because of the grant to member {}",
                        hierarchy.getUniqueName(), member.getUniqueName());
                    // Upgrade to CUSTOM level.
                    role.grant(
                        hierarchy,
                        AccessHierarchy.CUSTOM,
                        topLevel,
                        bottomLevel,
                        rollupPolicy);
                }
            }
        }

        @Override
		public AccessHierarchy getAccess() {
            return access;
        }

        @Override
		public AccessMember getAccess(Member member) {
            if (this.access != AccessHierarchy.CUSTOM) {
                return AccessUtil.getAccessMember(this.access);
            }
            MemberAccess mAccess =
                memberGrants.get(member.getUniqueName());
            AccessMember accessInner = mAccess == null ? null : mAccess.access;
            // Check for an explicit deny.
            if (accessInner == AccessMember.NONE) {
                LOGGER.trace(
                    "Access level {} granted to member {} because it is explicitly denied.",
                    AccessMember.NONE, member.getUniqueName());
                return AccessMember.NONE;
            }
            // Check for explicit grant
            if (accessInner == AccessMember.ALL || accessInner == AccessMember.CUSTOM) {
                LOGGER.trace(
                    "Access level {} granted to member {}",
                    accessInner, member.getUniqueName());
                return accessInner;
            }
            // Restricted is ok. This means an explicit grant
            // followed by a deny of one of the children: so custom.
            if (accessInner == AccessMember.RESTRICTED) {
                LOGGER.trace(
                    "Access level {} granted to member {} because it was RESTRICTED. ",
                    AccessMember.CUSTOM, member.getUniqueName());
                return AccessMember.CUSTOM;
            }
            // Check if the member is out of the bounds
            // defined by topLevel and bottomLevel
            if (!checkLevelIsOkWithRestrictions(this, member.getLevel())) {
                LOGGER.trace(
                    "Access denied to member {} because its level {} is out of the permitted bounds of between {} and {}",
                    member.getUniqueName(), member.getLevel().getUniqueName(),
                    this.topLevel.getUniqueName(), this.bottomLevel.getUniqueName());
                return AccessMember.NONE;
            }
            // Nothing was explicitly defined for this member.
            // Check for grants on its parents
            for (Member m = member.getParentMember();
                m != null;
                m = m.getParentMember())
            {
                MemberAccess pAccess =
                    memberGrants.get(m.getUniqueName());
                final AccessMember parentAccess = pAccess == null
                    ? null
                    : pAccess.access;
                if (parentAccess == null) {
                    // No explicit rules for this parent
                    continue;
                }
                // Check for parent deny
                if (parentAccess == AccessMember.NONE
                    || parentAccess == AccessMember.CUSTOM)
                {
                    LOGGER.trace(
                        "Access denied to member {} because its parent {} is of access level {}",
                        member.getUniqueName(), m.getUniqueName(), parentAccess);
                    return AccessMember.NONE;
                }
                // Both RESTRICTED and ALL are OK for parents.
                LOGGER.trace(
                    "Access level ALL granted to member {} because its parent {} is of access level {}",
                    member.getUniqueName(), m.getUniqueName(), parentAccess);
                return AccessMember.ALL;
            }
            // Check for inherited access from ancestors.
            // "Custom" is not good enough. We are looking for "all" access.
            accessInner = role.getAccess(member.getLevel());
            if (accessInner == AccessMember.ALL) {
                LOGGER.trace(
                    "Access ALL granted to member {} because its level {} is of access level ALL",
                    member.getUniqueName(), member.getLevel().getUniqueName());
                return AccessMember.ALL;
            }
            // This member might be at a level allowed by the
            // topLevel/bottomLevel attributes. If there are no explicit
            // member grants defined at this level but the member fits
            // those bounds, we give access.
            if (memberGrants.size() == 0) {
                LOGGER.trace(
                    "Access level ALL granted to member {} because it lies between the permitted level bounds and there are no explicit member grants defined in hierarchy {}",
                    member.getUniqueName(), member.getHierarchy().getUniqueName());
                return AccessMember.ALL;
            }
            // No access
            LOGGER.trace(
                "Access denied to member {} because none of its parents allow access to it.",
                member.getUniqueName());
            return AccessMember.NONE;
        }

        @Override
		public final int getTopLevelDepth() {
            return topLevel.getDepth();
        }

        @Override
		public final int getBottomLevelDepth() {
            return bottomLevel.getDepth();
        }

        @Override
		public RollupPolicy getRollupPolicy() {
            return rollupPolicy;
        }

        /**
         * Tells whether a given member has some of its children being
         * restricted by the access controls of this role instance.
         */
        @Override
		public boolean hasInaccessibleDescendants(Member member) {
            for (MemberAccess accessInner : memberGrants.values()) {
                if ((AccessMember.NONE.equals(accessInner.access)
                    || AccessMember.CUSTOM.equals(accessInner.access))
                    && accessInner.isSubGrant(member)) {
                    // At least one of the limited member is
                    // part of the descendants of this member.
                    return true;
                }
            }
            // All descendants are accessible.
            return false;
        }
    }

    /**
     * A MemberAccess contains information about a grant applied
     * to a member for a given role. It is only an internal data
     * structure and should not be exposed via the API.
     */
    private static class MemberAccess {
        private final Member member;
        private final AccessMember access;
        // A bounded cache of "is this granted member a descendant of
        // parentMember?". The previous WeakHashMap keyed by getUniqueName()
        // was ineffective: the key strings are freshly built per call and,
        // with no other strong reference, were GC-eligible almost immediately,
        // so entries rarely survived to be reused. A size-capped strong map
        // keeps the cache effective while still bounding growth on
        // high-cardinality dimensions (the original intent). Insertion order
        // keeps get() non-mutating.
        private static final int MAX_PARENTS_CACHED = 1000;
        private final Map<String, Boolean> parentsCache =
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(
                    Map.Entry<String, Boolean> eldest)
                {
                    return size() > MAX_PARENTS_CACHED;
                }
            };
        public MemberAccess(
            Member member,
            AccessMember access)
        {
                this.member = member;
                this.access = access;
        }

        /**
         * Tells whether the member concerned by this grant object
         * is a children of a given member. The result of the computation
         * is cached for faster results, since this might get called
         * very often.
         */
        private boolean isSubGrant(Member parentMember) {
            if (parentsCache.containsKey(parentMember.getUniqueName())) {
                return parentsCache.get(parentMember.getUniqueName());
            }
            for (Member m = member; m != null; m = m.getParentMember()) {
                if (m.equals(parentMember)) {
                    // We have proved that this granted member is a
                    // descendant of 'member'. Cache it and return.
                    parentsCache.put(
                        parentMember.getUniqueName(), Boolean.TRUE);
                    return true;
                }
            }
            // Not a parent. Cache it and return.
            if (ExecutionConfig.current().enableRolapCubeMemberCache())
            {
                parentsCache.put(
                    parentMember.getUniqueName(), Boolean.FALSE);
            }
            return false;
        }

        @Override
		public String toString() {
            return
                new StringBuilder("MemberAccess{")
                    .append(member.getUniqueName())
                    .append(" : ")
                    .append(access.toString())
                    .append("}").toString();
        }
    }

    /**
     * Implementation of org.eclipse.daanse.olap.api.access.AccessHierarchy that
     * delegates all methods to an underlying hierarchy access.
     */
    public abstract static class DelegatingHierarchyAccess
        implements AllHierarchyAccess
    {
        protected final HierarchyAccess hierarchyAccess;

        /**
         * Creates a DelegatingHierarchyAccess.
         *
         * @param hierarchyAccess Underlying hierarchy access
         */
        protected DelegatingHierarchyAccess(HierarchyAccess hierarchyAccess) {
            assert hierarchyAccess != null;
            this.hierarchyAccess = hierarchyAccess;
        }

        @Override
		public AccessMember getAccess(Member member) {
            return hierarchyAccess.getAccess(member);
        }

        @Override
		public int getTopLevelDepth() {
            return hierarchyAccess.getTopLevelDepth();
        }

        @Override
		public int getBottomLevelDepth() {
            return hierarchyAccess.getBottomLevelDepth();
        }

        @Override
		public RollupPolicy getRollupPolicy() {
            return hierarchyAccess.getRollupPolicy();
        }

        @Override
		public boolean hasInaccessibleDescendants(Member member) {
            return hierarchyAccess.hasInaccessibleDescendants(member);
        }

        @Override
		public AccessHierarchy getAccess() {
            if (hierarchyAccess instanceof AllHierarchyAccess allHierarchyAccess) {
                return allHierarchyAccess.getAccess();
            }
            throw Util.newInternal(
                "Unsupported operation. Should implement AllHierarchyAccess.");
        }
    }

    /**
     * Implementation of {@link org.eclipse.daanse.olap.api.access.MappingRole.HierarchyAccess} that caches
     * the access of each member and level.
     *
     * This reduces the number of calls to the underlying HierarchyAccess,
     * which is particularly useful for a union role which is based on many
     * roles.
     *
     * Caching uses two {@link java.util.WeakHashMap} objects, so should
     * release resources if memory is scarce. However, it may use up memory and
     * cause segments etc. to be removed from the cache when GC is triggered.
     * For this reason, you should only use this wrapper for a HierarchyAccess
     * which would otherwise have poor performance; currently used for union
     * roles with 5 or more member roles.
     */
    static class CachingHierarchyAccess
        extends DelegatingHierarchyAccess
    {
        private final Map<Member, AccessMember> memberAccessMap =
            new WeakHashMap<>();
        private RollupPolicy rollupPolicy;
        private Map<Member, Boolean> inaccessibleDescendantsMap =
            new WeakHashMap<>();
        private Integer topLevelDepth;
        private Integer bottomLevelDepth;

        /**
         * Creates a CachingHierarchyAccess.
         *
         * @param hierarchyAccess Underlying hierarchy access
         */
        public CachingHierarchyAccess(HierarchyAccess hierarchyAccess) {
            super(hierarchyAccess);
        }

        @Override
        public AccessMember getAccess(Member member) {
            AccessMember access = memberAccessMap.get(member);
            if (access != null) {
                return access;
            }
            access = hierarchyAccess.getAccess(member);
            memberAccessMap.put(member, access);
            return access;
        }

        @Override
        public int getTopLevelDepth() {
            if (topLevelDepth == null) {
                topLevelDepth = hierarchyAccess.getTopLevelDepth();
            }
            return topLevelDepth;
        }

        @Override
        public int getBottomLevelDepth() {
            if (bottomLevelDepth == null) {
                bottomLevelDepth = hierarchyAccess.getBottomLevelDepth();
            }
            return bottomLevelDepth;
        }

        @Override
        public RollupPolicy getRollupPolicy() {
            if (rollupPolicy == null) {
                rollupPolicy = hierarchyAccess.getRollupPolicy();
            }
            return rollupPolicy;
        }

        @Override
        public boolean hasInaccessibleDescendants(Member member) {
            return inaccessibleDescendantsMap.computeIfAbsent(member, hierarchyAccess::hasInaccessibleDescendants);
        }
    }
}
