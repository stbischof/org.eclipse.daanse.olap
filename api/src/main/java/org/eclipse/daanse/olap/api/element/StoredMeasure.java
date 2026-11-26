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

import java.util.Optional;

import org.eclipse.daanse.olap.api.DataTypeJdbc;

public interface StoredMeasure extends Measure {

    String getAggregateFunction();

    /**
     * Returns the cube this measure belongs to.
     */
    Cube getCube();

    default Optional<DataTypeJdbc> getDataType() {
        return Optional.empty();
    }

    /** {@inheritDoc} A stored measure reads the facts of the cube it belongs to. */
    @Override
    default Optional<Cube> factCube() {
        return Optional.ofNullable(getCube());
    }
}
