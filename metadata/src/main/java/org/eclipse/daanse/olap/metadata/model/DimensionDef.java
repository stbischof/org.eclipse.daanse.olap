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

/**
 * A dimension. Identity: a dimension shared between cubes, or used twice in one cube, is one
 * object named by several {@link DimensionUsageDef}s.
 */
public final class DimensionDef {

    private final String name;
    private final DimensionKind kind;
    private final List<HierarchyDef> hierarchies;
    private final ElementInfo info;

    public DimensionDef(String name, DimensionKind kind, List<HierarchyDef> hierarchies, ElementInfo info) {
        this.name = name;
        this.kind = kind;
        this.hierarchies = List.copyOf(hierarchies);
        this.info = info;
    }

    public String name() {
        return name;
    }

    public DimensionKind kind() {
        return kind;
    }

    /** The hierarchies; the first is the default one. */
    public List<HierarchyDef> hierarchies() {
        return hierarchies;
    }

    public ElementInfo info() {
        return info;
    }

    @Override
    public String toString() {
        return "dimension " + name;
    }
}
