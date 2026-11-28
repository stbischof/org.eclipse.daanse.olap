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

import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.findings.Place;
import org.eclipse.daanse.olap.metadata.model.DimensionDef;
import org.eclipse.daanse.olap.metadata.model.DimensionKind;
import org.eclipse.daanse.olap.metadata.model.HierarchyDef;

public final class DimensionBuilder extends ElementBuilder<DimensionBuilder> {

    private DimensionKind kind = DimensionKind.STANDARD;
    private final List<Consumer<List<HierarchyDef>>> hierarchies = new ArrayList<>();
    private final List<Finding> problems;
    private final Place path;

    DimensionBuilder(String name, List<Finding> problems, Place path) {
        super(name);
        this.problems = problems;
        this.path = path.then("dimension", name);
    }

    /** The time functions of MDX apply. */
    public DimensionBuilder time() {
        this.kind = DimensionKind.TIME;
        return this;
    }

    /** The one hierarchy of the dimension, named like it. */
    public DimensionBuilder hierarchy(Consumer<HierarchyBuilder> hierarchy) {
        return hierarchy(name, hierarchy);
    }

    public DimensionBuilder hierarchy(String hierarchyName, Consumer<HierarchyBuilder> hierarchy) {
        hierarchies.add(built -> {
            HierarchyBuilder builder = new HierarchyBuilder(hierarchyName);
            hierarchy.accept(builder);
            built.add(builder.build(problems, path));
        });
        return this;
    }

    DimensionDef build() {
        List<HierarchyDef> built = new ArrayList<>();
        hierarchies.forEach(h -> h.accept(built));
        return new DimensionDef(name, kind, built, info());
    }
}
