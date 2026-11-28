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
 * What an engine may cache for a cube. A catalog may say the same thing again as a
 * {@code daanse:cache.*} tag on the cube or on itself, and whoever says no wins.
 *
 * @param members whether members read from the member source are kept
 * @param cells   whether blocks of cells read from the cell source are kept
 */
public record CachePolicyDef(boolean members, boolean cells) {

    public static final CachePolicyDef DEFAULT = new CachePolicyDef(true, true);
    public static final CachePolicyDef OFF = new CachePolicyDef(false, false);
}
