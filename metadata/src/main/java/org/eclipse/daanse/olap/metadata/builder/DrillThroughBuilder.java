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

import org.eclipse.daanse.olap.metadata.model.ActionDef;

/**
 * Builds a drill-through action of a cube, which carries a caption and a description like every
 * other named element, as in
 * {@code c.drillThrough("Facts", a -> a.caption("The facts").asDefault().returns("[Measures].[Amount]"))}.
 */
public final class DrillThroughBuilder extends ElementBuilder<DrillThroughBuilder> {

    private final List<String> returns = new ArrayList<>();
    private boolean isDefault;

    DrillThroughBuilder(String name) {
        super(name);
    }

    /** A plain DRILLTHROUGH without a RETURN clause uses this action. */
    public DrillThroughBuilder asDefault() {
        this.isDefault = true;
        return this;
    }

    /** MDX text naming the levels, properties and measures the action returns. */
    public DrillThroughBuilder returns(String... mdx) {
        returns.addAll(List.of(mdx));
        return this;
    }

    ActionDef.DrillThrough build() {
        return new ActionDef.DrillThrough(name, isDefault, returns, info());
    }
}
