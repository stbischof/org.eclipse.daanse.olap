/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.api.element;

/** A cube whose measures come from other cubes. */
public interface VirtualCube extends Cube {

    @Override
    boolean unrelatedDimensionsStayFor(Measure measure);

    @Override
    default boolean drawsFromOtherCubes() {
        return true;
    }
}
