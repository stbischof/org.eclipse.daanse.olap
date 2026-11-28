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

/**
 * A parameter of the catalog, read in MDX with {@code ParamRef("name")}.
 *
 * @param kind         the kind of value; {@code null} for a member parameter
 * @param defaultValue MDX text of the default value
 * @param modifiable   whether a connection may set it
 * @param hierarchy    for a member parameter the hierarchy its members come from, as Mondrian's
 *                     {@code Parameter("p", [Dim], ...)} names it; {@code null} for a value parameter
 */
public record ParameterDef(String name, ValueKind kind, String defaultValue, boolean modifiable,
        ElementInfo info, String hierarchy) {

    /** A value parameter. */
    public ParameterDef(String name, ValueKind kind, String defaultValue, boolean modifiable, ElementInfo info) {
        this(name, kind, defaultValue, modifiable, info, null);
    }
}
