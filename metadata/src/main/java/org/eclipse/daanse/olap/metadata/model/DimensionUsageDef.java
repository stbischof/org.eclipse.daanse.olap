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
 * The place a dimension takes in a cube. The name of the usage is the name of the dimension in
 * that cube: a Time dimension used as "Order Date" and "Ship Date" is two usages of one
 * {@link DimensionDef}.
 */
public record DimensionUsageDef(String name, DimensionDef dimension, ElementInfo info,
        org.eclipse.daanse.olap.metadata.relational.UsageJoin join) {

    /** A usage whose facts reach the dimension through a source of their own, not through a fact column. */
    public DimensionUsageDef(String name, DimensionDef dimension, ElementInfo info) {
        this(name, dimension, info, null);
    }
}
