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
 * A measure a virtual cube takes over, pointing at the measure itself rather than at its name. A hidden usage is not
 * a measure a client sees: it is one a formula taken over from the cube may name, and it came along for that alone.
 *
 * @param name the name in the virtual cube; usually the name of the measure
 */
public record MeasureUsageDef(String name, PhysicalCubeDef cube, MeasureDef measure, boolean hidden) {

    public MeasureUsageDef(String name, PhysicalCubeDef cube, MeasureDef measure) {
        this(name, cube, measure, false);
    }

}
