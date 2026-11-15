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
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.catalog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.access.HierarchyAccess;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.OlapElement;

/**
 * What a catalog reader answers the same way whatever the catalog is made of.
 * <p>
 * These are the questions about what the role in force lets a client see. They are the same for every
 * kind of catalog, because a role is about the model and not about where the model comes from. Everything
 * a reader must answer for itself is refused by name by {@link AbstractRefusingCatalogReader} until it
 * does.
 */
public abstract class CatalogReaderBase extends AbstractRefusingCatalogReader {

    /**
     * Whether the role in force lets this element be seen. Without a role everything may be seen, which is
     * what a reader that was never given one means.
     */
    protected boolean mayBeSeen(OlapElement element) {
        Role role = getRole();
        return role == null || role.canAccess(element);
    }

    /**
     * Looks a name up the way a client wrote it, exactly. Which reader resolves it is the reader's own
     * business; that an unqualified lookup means an exact match is not, so it is answered once.
     */
    @Override
    public final OlapElement lookupCompound(OlapElement parent, List<Segment> names, boolean failIfNotFound,
            DataType category) {
        return lookupCompound(parent, names, failIfNotFound, category, MatchType.EXACT);
    }

    @Override
    public List<Dimension> getCubeDimensions(Cube cube) {
        List<Dimension> visible = new ArrayList<>();
        for (Dimension dimension : cube.getDimensions()) {
            if (mayBeSeen(dimension)) {
                visible.add(dimension);
            }
        }
        return visible;
    }

    @Override
    public List<Hierarchy> getDimensionHierarchies(Dimension dimension) {
        List<Hierarchy> visible = new ArrayList<>();
        for (Hierarchy hierarchy : dimension.getHierarchies()) {
            if (mayBeSeen(hierarchy)) {
                visible.add(hierarchy);
            }
        }
        return visible;
    }

    /**
     * The levels of the hierarchy the role may see: where a role was granted the hierarchy from one level
     * down to another, the ones above and below are not named at all - a client is told about what it may
     * ask for, not about what exists.
     */
    @Override
    public List<Level> getHierarchyLevels(Hierarchy hierarchy) {
        @SuppressWarnings("unchecked")
        List<Level> levels = (List<Level>) hierarchy.getLevels();
        Role role = getRole();
        HierarchyAccess access = role == null ? null : role.getAccessDetails(hierarchy);
        if (access == null) {
            return levels;
        }
        return levels.subList(access.getTopLevelDepth(), access.getBottomLevelDepth() + 1);
    }
}
