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
 */
package org.eclipse.daanse.olap.metadata.model;

/**
 * One fact as a derived measure reads it: its values by name, measures and other fields alike.
 */
public interface FactRow {

    /** The value the fact carries under this name; {@code null} when it carries none. */
    Object value(String name);

    /** The value as a number; {@code null} when the fact carries none or carries no number under this name. */
    default Number number(String name) {
        return value(name) instanceof Number n ? n : null;
    }
}
