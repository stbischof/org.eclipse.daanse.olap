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

import java.util.Optional;

/**
 * How a cube's facts reach a dimension it uses: the fact column that holds the key of a member, and, where the
 * facts stop above the leaves, the level they reach. The binding of a dimension usage to the fact table; a usage
 * whose hierarchies read the fact rows themselves needs no foreign key.
 *
 * @param foreignKey  the fact column holding the member key; {@code null} when the dimension's columns are fact
 *                    columns
 * @param level       the level the facts join to, where it is not the lowest; {@code null} for the lowest
 * @param usagePrefix a prefix put before the dimension's column aliases, where one dimension is used twice in one
 *                    fact table; {@code null} for none
 */
public record UsageJoin(ColumnDef foreignKey, String level, String usagePrefix) {

    public static UsageJoin by(ColumnDef foreignKey) {
        return new UsageJoin(foreignKey, null, null);
    }

    public Optional<ColumnDef> foreignKeyColumn() {
        return Optional.ofNullable(foreignKey);
    }
}
