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
 * A dimension a measure group varies by, and how finely.
 *
 * @param usage       the name of the dimension usage in the cube
 * @param granularity the name of the level the facts of the group reach in it, or null for the lowest
 *                    level there is. Above the lowest level the facts are already summed up: a group whose
 *                    time granularity is the month says nothing about a single day, and a client that asks
 *                    for one is told so rather than shown a number that was never measured
 */
public record MeasureGroupDimensionDef(String usage, String granularity) {

    /** A dimension the group varies by as finely as the dimension goes. */
    public static MeasureGroupDimensionDef of(String usage) {
        return new MeasureGroupDimensionDef(usage, null);
    }
}
