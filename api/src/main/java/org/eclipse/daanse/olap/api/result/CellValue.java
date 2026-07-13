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

/**
 * The state of a cell at the cell/cache boundary.
 *
 * <p>
 * Between segment storage and the public {@link Cell} API a cell is in
 * exactly one of four states. This sealed hierarchy makes the protocol
 * explicit and compiler-checked: an exhaustive {@code switch} over
 * {@code CellValue} cannot silently forget a state.
 *
 * <ul>
 * <li>{@link NullValue} — the cell evaluates to MDX NULL,</li>
 * <li>{@link NotLoaded} — the cell is not in the cache yet (batching
 * pass),</li>
 * <li>{@link ErrorValue} — evaluation failed; carries the cause,</li>
 * <li>{@link ObjectValue} — a real value (the already-boxed value
 * object).</li>
 * </ul>
 *
 * <p>
 * Deliberately no {@code NumericValue(double)} variant: that would allocate
 * per cell in the aggregation hot path (revisit with Valhalla value classes).
 */
public sealed interface CellValue permits NullValue, NotLoaded, ErrorValue, ObjectValue {

    /**
     * Unwraps to the legacy object convention of the public {@link Cell} API:
     * {@link NullValue} → Java {@code null}, {@link ErrorValue} → the
     * {@link Throwable}, {@link ObjectValue} → the value,
     * {@link NotLoaded} → the marker itself.
 */
    default Object toLegacyValue() {
        return switch (this) {
        case NullValue v -> null;
        case NotLoaded n -> n;
        case ErrorValue e -> e.cause();
        case ObjectValue o -> o.value();
        };
    }

    /**
     * Wraps a value in the legacy object convention: Java {@code null} →
     * {@link NullValue}, {@link NotLoaded} → itself, a {@link Throwable} →
     * {@link ErrorValue}, an existing {@code CellValue} → itself, anything
     * else → {@link ObjectValue}.
 */
    static CellValue fromLegacyValue(Object o) {
        if (o == null) {
            return NullValue.INSTANCE;
        }
        if (o instanceof CellValue cv) {
            return cv;
        }
        if (o instanceof Throwable t) {
            return new ErrorValue(t);
        }
        return new ObjectValue(o);
    }
}
