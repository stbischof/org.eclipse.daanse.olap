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

import java.util.Map;

/**
 * A member computed by an MDX formula.
 *
 * @param hierarchy    MDX text naming the hierarchy it belongs to, {@code [Measures]} for a measure
 * @param parent       MDX text naming its parent member; {@code null} for a root
 * @param formula      the MDX expression
 * @param formatString {@code null} for the default format
 * @param solveOrder   precedence among intersecting calculated members
 * @param properties   further member properties, as MDX expressions by name
 */
public record CalculatedMemberDef(String name, String hierarchy, String parent, String formula,
        String formatString, int solveOrder, Map<String, String> properties, LocalizedText displayFolder,
        ElementInfo info) {

    public CalculatedMemberDef {
        // in the order given: a property clause is written out in it
        properties = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(properties));
    }
}
