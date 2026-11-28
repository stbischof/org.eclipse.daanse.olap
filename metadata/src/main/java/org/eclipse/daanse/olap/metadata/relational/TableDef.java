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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A table or a view of the database, with its columns. An identity: what is read is keyed by the table, and a cube,
 * a hierarchy or a level names the table through the same object its columns belong to.
 *
 * <p>A table may stand without a schema -- a catalog that names tables as the connection finds them -- or inside
 * one, which is then part of how SQL names it.
 */
public final class TableDef {

    /** Whether the rows are stored or computed by the database. */
    public enum Kind {
        TABLE, VIEW
    }

    private final String name;
    private final DatabaseSchemaDef schema;
    private final Kind kind;
    private final String description;
    private final List<PhysicalColumnDef> columns = new ArrayList<>();
    private boolean frozen;

    /** @param description what the catalog says about the table, or {@code null} */
    public TableDef(String name, DatabaseSchemaDef schema, Kind kind, String description) {
        this.name = name;
        this.schema = schema;
        this.kind = kind == null ? Kind.TABLE : kind;
        this.description = description;
    }

    public String name() {
        return name;
    }

    /** The schema the table lies in; empty for a table named without one. */
    public Optional<DatabaseSchemaDef> schema() {
        return Optional.ofNullable(schema);
    }

    public Kind kind() {
        return kind;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /** The columns of the table, in the order they were declared. */
    public List<PhysicalColumnDef> columns() {
        return Collections.unmodifiableList(columns);
    }

    /** The column of this name, matched as the database matches it: case does not matter. */
    public Optional<PhysicalColumnDef> column(String columnName) {
        return columns.stream().filter(c -> c.name().equalsIgnoreCase(columnName)).findFirst();
    }

    /** Adds a column while the table is being built. */
    void add(PhysicalColumnDef column) {
        if (frozen) {
            throw new IllegalStateException("table " + name + " is built; columns are added while building");
        }
        columns.add(column);
    }

    void freeze() {
        frozen = true;
    }

    /** A table outside any schema, built in one go: the table a connection finds by its name alone. */
    public static TableDef build(String name, java.util.function.Consumer<DatabaseSchemaDef.TableBuilder> columns) {
        TableDef table = new TableDef(name, null, Kind.TABLE, null);
        columns.accept(new DatabaseSchemaDef.TableBuilder(table));
        table.freeze();
        return table;
    }

    /** The column of this name; the catalog names it, so a missing one is an error that says which. */
    public PhysicalColumnDef columnNamed(String columnName) {
        return column(columnName).orElseThrow(() -> new IllegalArgumentException(
                "table " + this + " has no column " + columnName + "; it has "
                        + columns.stream().map(PhysicalColumnDef::name).toList()));
    }

    @Override
    public String toString() {
        // a schema without a name (a catalog that names tables as the connection finds them) is left out
        return (schema == null || schema.name() == null || schema.name().isBlank() ? "" : schema.name() + ".") + name;
    }
}
