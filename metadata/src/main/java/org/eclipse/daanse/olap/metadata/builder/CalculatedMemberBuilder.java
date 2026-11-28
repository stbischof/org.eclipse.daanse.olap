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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.daanse.olap.metadata.model.CalculatedMemberDef;

/** Builds a calculated member; a calculated measure unless another hierarchy is named. */
public final class CalculatedMemberBuilder extends ElementBuilder<CalculatedMemberBuilder> {

    private final String formula;
    private String hierarchy = "[Measures]";
    private String parent;
    private String formatString;
    private int solveOrder;
    private org.eclipse.daanse.olap.metadata.model.LocalizedText displayFolder;
    private final Map<String, String> properties = new LinkedHashMap<>();

    CalculatedMemberBuilder(String name, String formula) {
        super(name);
        this.formula = formula;
    }

    /** MDX text naming the hierarchy the member belongs to. */
    public CalculatedMemberBuilder hierarchy(String mdx) {
        this.hierarchy = mdx;
        return this;
    }

    /** MDX text naming the parent member. */
    public CalculatedMemberBuilder parent(String mdx) {
        this.parent = mdx;
        return this;
    }

    public CalculatedMemberBuilder formatString(String formatString) {
        this.formatString = formatString;
        return this;
    }

    public CalculatedMemberBuilder solveOrder(int solveOrder) {
        this.solveOrder = solveOrder;
        return this;
    }

    /** The folder clients show the member in. */
    public CalculatedMemberBuilder displayFolder(String folder) {
        this.displayFolder = folder == null ? null : org.eclipse.daanse.olap.metadata.model.LocalizedText.of(folder);
        return this;
    }

    /** A further member property, as an MDX expression. */
    public CalculatedMemberBuilder property(String propertyName, String mdx) {
        properties.put(propertyName, mdx);
        return this;
    }

    CalculatedMemberDef build() {
        return new CalculatedMemberDef(name, hierarchy, parent, formula, formatString, solveOrder, properties,
                displayFolder, info());
    }
}
