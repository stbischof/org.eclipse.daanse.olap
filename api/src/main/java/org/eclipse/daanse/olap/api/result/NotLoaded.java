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
 * Marker for a cell whose value has not been loaded from the database yet.
 *
 * <p>
 * The batching cell reader returns this singleton on a cache miss while it
 * collects cell requests; the evaluation pass that produced it is marked
 * dirty and its results are discarded after the batch has been fetched.
 * {@code NotLoaded} is
 * deliberately not a {@link Number}: any code that tries to compute with it
 * fails fast instead of silently producing values from thin air.
 */
public record NotLoaded() implements CellValue {

    public static final NotLoaded INSTANCE = new NotLoaded();

    @Override
    public String toString() {
        return "NOT_LOADED";
    }
}
