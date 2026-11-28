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
 * What a level means to the time functions of MDX; {@link #REGULAR} outside a time dimension. {@link #TIME_UNDEFINED}
 * is a level of a time dimension that is no calendar unit (Mondrian TimeUndefined), so that a time dimension can
 * carry it without the time rule failing. The names are those of the engine's LevelType.
 */
public enum LevelKind {
    REGULAR, TIME_YEARS, TIME_HALF_YEARS, TIME_QUARTERS, TIME_MONTHS, TIME_WEEKS, TIME_DAYS, TIME_HOURS,
    TIME_MINUTES, TIME_SECONDS, TIME_UNDEFINED
}
