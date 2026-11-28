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
package org.eclipse.daanse.olap.metadata.relational;

/**
 * A table the matching of aggregation tables must leave out: by its name, or by a pattern over names.
 *
 * @param name       a table name, or {@code null} when a pattern is given
 * @param pattern    a regular expression over table names, or {@code null} when a name is given
 * @param ignoreCase whether the name or pattern matches regardless of case
 */
public record AggregationExcludeDef(String name, String pattern, boolean ignoreCase) {

    public static AggregationExcludeDef named(String name) {
        return new AggregationExcludeDef(name, null, true);
    }
}
