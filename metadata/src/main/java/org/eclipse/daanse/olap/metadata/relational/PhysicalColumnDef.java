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

import org.eclipse.daanse.olap.metadata.model.ValueKind;

/**
 * A column of a table. An identity: what is read is keyed by the column object, and a level, a measure or a foreign
 * key names its column through the table that holds it -- which is why the column knows its table.
 */
public final class PhysicalColumnDef implements ColumnDef {

    private final String name;
    private final TableDef table;
    private final ValueKind type;
    private final Integer size;
    private final Integer scale;
    private final boolean nullable;
    private final String sqlType;
    private final Integer jdbcType;

    /**
     * @param size     the length or precision the database declares, or {@code null}
     * @param scale    the digits after the point for a numeric column, or {@code null}
     * @param nullable whether a row may leave the column empty
     */
    public PhysicalColumnDef(String name, TableDef table, ValueKind type, Integer size, Integer scale,
            boolean nullable) {
        this(name, table, type, size, scale, nullable, null);
    }

    public PhysicalColumnDef(String name, TableDef table, ValueKind type, Integer size, Integer scale,
            boolean nullable, String sqlType) {
        this(name, table, type, size, scale, nullable, sqlType, null);
    }

    public PhysicalColumnDef(String name, TableDef table, ValueKind type, Integer size, Integer scale,
            boolean nullable, String sqlType, Integer jdbcType) {
        this.sqlType = sqlType;
        this.jdbcType = jdbcType;
        this.name = name;
        this.table = table;
        this.type = type;
        this.size = size;
        this.scale = scale;
        this.nullable = nullable;
    }

    @Override
    public String name() {
        return name;
    }

    /** The table the column belongs to; {@code null} for a field of fact rows a source holds itself. */
    public TableDef table() {
        return table;
    }

    @Override
    public ValueKind type() {
        return type;
    }

    public Optional<Integer> size() {
        return Optional.ofNullable(size);
    }

    public Optional<Integer> scale() {
        return Optional.ofNullable(scale);
    }

    public boolean nullable() {
        return nullable;
    }

    /**
     * The type the database reports for the column through JDBC ({@link java.sql.Types}), where the catalog says. A
     * reader over SQL types its values by it, finer than {@link ValueKind} tells them apart (a SMALLINT from an
     * INTEGER).
     */
    public Optional<Integer> jdbcType() {
        return Optional.ofNullable(jdbcType);
    }

    /**
     * The type the database declares, as SQL names it ({@code VARCHAR}, {@code INTEGER}), where the catalog says; an
     * inline table hands it to the SQL it generates.
     */
    public Optional<String> sqlType() {
        return Optional.ofNullable(sqlType);
    }

    @Override
    public String toString() {
        return table == null ? name : table + "." + name;
    }
}
