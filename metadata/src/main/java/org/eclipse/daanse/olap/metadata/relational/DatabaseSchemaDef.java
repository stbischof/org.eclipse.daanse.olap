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
 * A schema of the database the catalog reads: a named set of tables. An identity, not a value: two schemas of one
 * name in two databases are two schemas, and a table knows the one it belongs to.
 *
 * <p>Built through the builder, which adds the tables before the schema is handed out; afterwards it does not change.
 */
public final class DatabaseSchemaDef {

    private final String name;
    private final List<TableDef> tables = new ArrayList<>();
    private boolean frozen;

    public DatabaseSchemaDef(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** The tables of the schema, in the order they were declared. */
    public List<TableDef> tables() {
        return Collections.unmodifiableList(tables);
    }

    /** The table of this name, matched as the database matches it: case does not matter. */
    public Optional<TableDef> table(String tableName) {
        return tables.stream().filter(t -> t.name().equalsIgnoreCase(tableName)).findFirst();
    }

    /** Adds a table while the schema is being built. */
    void add(TableDef table) {
        if (frozen) {
            throw new IllegalStateException("schema " + name + " is built; tables are added while building");
        }
        tables.add(table);
    }

    void freeze() {
        frozen = true;
    }

    /**
     * A schema built in one go: its tables and their columns, after which nothing is added any more.
     *
     * <pre>{@code
     * DatabaseSchemaDef shop = DatabaseSchemaDef.build("shop", s -> s
     *         .table("sales", t -> t.column("store_id", ValueKind.INTEGER).column("amount", ValueKind.NUMERIC))
     *         .table("store", t -> t.column("id", ValueKind.INTEGER).column("city", ValueKind.STRING)));
     * }</pre>
     */
    public static DatabaseSchemaDef build(String name, java.util.function.Consumer<Builder> tables) {
        DatabaseSchemaDef schema = new DatabaseSchemaDef(name);
        tables.accept(new Builder(schema));
        for (TableDef table : schema.tables) {
            table.freeze();
        }
        schema.freeze();
        return schema;
    }

    /** Adds the tables of a schema that is being built. */
    public static final class Builder {

        private final DatabaseSchemaDef schema;

        private Builder(DatabaseSchemaDef schema) {
            this.schema = schema;
        }

        public Builder table(String tableName, java.util.function.Consumer<TableBuilder> columns) {
            return add(tableName, TableDef.Kind.TABLE, null, columns);
        }

        public Builder view(String viewName, java.util.function.Consumer<TableBuilder> columns) {
            return add(viewName, TableDef.Kind.VIEW, null, columns);
        }

        public Builder add(String tableName, TableDef.Kind kind, String description,
                java.util.function.Consumer<TableBuilder> columns) {
            if (schema.table(tableName).isPresent()) {
                throw new IllegalArgumentException("schema " + schema.name + " already has a table " + tableName);
            }
            TableDef table = new TableDef(tableName, schema, kind, description);
            columns.accept(new TableBuilder(table));
            schema.add(table);
            return this;
        }
    }

    /** Adds the columns of a table that is being built. */
    public static final class TableBuilder {

        private final TableDef table;

        TableBuilder(TableDef table) {
            this.table = table;
        }

        public TableBuilder column(String columnName, org.eclipse.daanse.olap.metadata.model.ValueKind type) {
            return column(columnName, type, null, null, true);
        }

        public TableBuilder column(String columnName, org.eclipse.daanse.olap.metadata.model.ValueKind type,
                Integer size, Integer scale, boolean nullable) {
            return column(columnName, type, size, scale, nullable, null);
        }

        /** A column with the type the database declares for it, as SQL names it. */
        public TableBuilder column(String columnName, org.eclipse.daanse.olap.metadata.model.ValueKind type,
                Integer size, Integer scale, boolean nullable, String sqlType) {
            return column(columnName, type, size, scale, nullable, sqlType, null);
        }

        /** A column with the type the database declares for it, as SQL names it and as JDBC codes it. */
        public TableBuilder column(String columnName, org.eclipse.daanse.olap.metadata.model.ValueKind type,
                Integer size, Integer scale, boolean nullable, String sqlType, Integer jdbcType) {
            if (table.column(columnName).isPresent()) {
                throw new IllegalArgumentException("table " + table + " already has a column " + columnName);
            }
            table.add(new PhysicalColumnDef(columnName, table, type, size, scale, nullable, sqlType, jdbcType));
            return this;
        }
    }

    @Override
    public String toString() {
        return "schema " + name;
    }
}
