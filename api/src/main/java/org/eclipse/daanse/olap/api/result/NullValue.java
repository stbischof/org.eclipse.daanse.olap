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
 * The cell evaluates to MDX NULL. Allocation-free singleton, see
 * {@link CellValue}.
 */
public record NullValue() implements CellValue {

    public static final NullValue INSTANCE = new NullValue();

    @Override
    public String toString() {
        return "NULL";
    }
}
