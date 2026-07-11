/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.api.result;

import java.util.Objects;

/**
 * A real cell value, carrying the already-boxed value object (often a
 * {@link Double}, {@link java.math.BigDecimal}, {@link Integer} or
 * {@link String}), see {@link CellValue}.
 */
public record ObjectValue(Object value) implements CellValue {

    public ObjectValue {
        Objects.requireNonNull(value, "value; use NullValue.INSTANCE for MDX NULL");
    }
}
