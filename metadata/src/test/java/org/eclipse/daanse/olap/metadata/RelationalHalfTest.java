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
package org.eclipse.daanse.olap.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.eclipse.daanse.olap.metadata.model.AggregatorDef;
import org.eclipse.daanse.olap.metadata.model.CatalogDef;
import org.eclipse.daanse.olap.metadata.model.ExplicitHierarchyDef;
import org.eclipse.daanse.olap.metadata.model.PhysicalCubeDef;
import org.eclipse.daanse.olap.metadata.model.ValueKind;
import org.eclipse.daanse.olap.metadata.relational.ColumnDef;
import org.eclipse.daanse.olap.metadata.relational.DatabaseSchemaDef;
import org.eclipse.daanse.olap.metadata.relational.InlineTableDef;
import org.eclipse.daanse.olap.metadata.relational.LevelColumns;
import org.eclipse.daanse.olap.metadata.relational.RelationalSource;
import org.eclipse.daanse.olap.metadata.relational.TableDef;
import org.eclipse.daanse.olap.metadata.relational.UsageJoin;
import org.junit.jupiter.api.Test;

/**
 * The relational half of the description: schema, tables and columns as identities built in one go, sources
 * as values that re-alias without copying, and the optional bindings an OLAP element carries.
 */
class RelationalHalfTest {

    private static final DatabaseSchemaDef SHOP = DatabaseSchemaDef.build("shop", s -> s
            .table("sales", t -> t.column("store_id", ValueKind.INTEGER).column("amount", ValueKind.NUMERIC))
            .table("store", t -> t.column("id", ValueKind.INTEGER).column("city", ValueKind.STRING)));

    @Test
    void aSchemaHoldsItsTablesAndATableItsColumnsAsIdentities() {
        TableDef sales = SHOP.table("SALES").orElseThrow();
        assertThat(sales.schema()).contains(SHOP);
        assertThat(sales.columnNamed("amount").table()).isSameAs(sales);
        assertThat(sales.columnNamed("Amount")).isSameAs(sales.columnNamed("amount"));
        assertThat(sales.toString()).isEqualTo("shop.sales");
        assertThatThrownBy(() -> sales.columnNamed("price")).hasMessageContaining("has no column price")
                .hasMessageContaining("store_id");
        assertThatThrownBy(() -> DatabaseSchemaDef.build("x", s -> s.table("t", t -> t.column("a", null))
                .table("T", t -> { }))).hasMessageContaining("already has a table T");
    }

    @Test
    void reAliasingASourceSharesItsTableAndAnUnchangedAliasIsTheSameValue() {
        TableDef sales = SHOP.table("sales").orElseThrow();
        RelationalSource.Table plain = RelationalSource.Table.of(sales);
        assertThat(plain.sqlAlias()).isEqualTo("sales");
        RelationalSource.Table aliased = plain.withAlias("s");
        assertThat(aliased.table()).isSameAs(sales);
        assertThat(aliased.sqlAlias()).isEqualTo("s");
        assertThat(aliased.withAlias("s")).isSameAs(aliased);
        RelationalSource.Join join = new RelationalSource.Join(
                new RelationalSource.Element(null, sales.columnNamed("store_id"), plain),
                new RelationalSource.Element("st", SHOP.table("store").orElseThrow().columnNamed("id"),
                        RelationalSource.Table.of(SHOP.table("store").orElseThrow())));
        assertThat(join.withAlias("anything")).isSameAs(join);
        assertThat(join.right().sqlAlias()).isEqualTo("st");
    }

    @Test
    void anInlineTableHoldsAValueForEveryColumnOfEachRow() {
        TableDef colours = TableDef.build("colour", t -> t.column("id", ValueKind.INTEGER).column("name", ValueKind.STRING));
        assertThat(new InlineTableDef(colours, List.of(List.of(1, "red"), java.util.Arrays.asList(2, null))).rows())
                .hasSize(2);
        assertThatThrownBy(() -> new InlineTableDef(colours, List.of(List.of(1))))
                .hasMessageContaining("holds 1 values for 2 columns");
    }

    @Test
    void theValidatorNamesAColumnOutsideTheTablesItIsReadFrom() {
        TableDef sales = SHOP.table("sales").orElseThrow();
        TableDef store = SHOP.table("store").orElseThrow();
        assertThatThrownBy(() -> Olap.catalog("Shop").cube("Sales", c -> c.facts(RelationalSource.Table.of(sales))
                .dimension("Store", d -> d.hierarchy(h -> h.level("City", l -> l.column(store.columnNamed("city")))))
                .join("Store", UsageJoin.by(store.columnNamed("id")))
                .measure("Amount", m -> m.column(store.columnNamed("city")))).build())
                .hasMessageContaining("cube Sales / dimension Store / hierarchy Store / level City: key column shop.store.city"
                        + " lies in none of the tables it is read from ([shop.sales]): Table 'store' not found")
                .hasMessageContaining("dimension Store: foreign key shop.store.id lies in none")
                .hasMessageContaining("measure Amount: column shop.store.city lies in none");
        assertThatThrownBy(() -> Olap.catalog("Shop").cube("Sales", c -> c
                .dimension("Store", d -> d.hierarchy(h -> h.level("City", l -> l.column(store.columnNamed("city")))))
                .join("Store", UsageJoin.by(sales.columnNamed("store_id"))).measure("Amount")).build())
                .hasMessageContaining("joins by a foreign key, but the cube names no fact table")
                .hasMessageContaining("reads columns, but neither its hierarchy nor the cube names a table");
        // a source of SQL text is not checked: its columns are what the SQL says -- for the facts and for a hierarchy
        Olap.catalog("Shop").cube("Sales", c -> c.facts(RelationalSource.Table.of(sales))
                .dimension("Store", d -> d.hierarchy(h -> h.rows(new RelationalSource.SqlSelect("v", List.of()), null)
                        .level("City", l -> l.column(store.columnNamed("city")))))
                .measure("Amount", m -> m.column(sales.columnNamed("amount")))).build();
        Olap.catalog("Shop").cube("Sales", c -> c.facts(new RelationalSource.SqlSelect("f", List.of()))
                .dimension("Store", d -> d.hierarchy(h -> h.level("City", l -> l.column(store.columnNamed("city")))))
                .measure("Amount", m -> m.column(sales.columnNamed("amount")))).build();
    }

    @Test
    void aggregationTablesAndTheWritebackTableHangOnTheFacts() {
        TableDef sales = SHOP.table("sales").orElseThrow();
        var noCount = new org.eclipse.daanse.olap.metadata.relational.AggregationTableDef.ByName(sales, null,
                new org.eclipse.daanse.olap.metadata.relational.AggregationTableDef.Columns(null, List.of(), List.of(),
                        List.of(), List.of(), List.of()));
        var writeback = new org.eclipse.daanse.olap.metadata.relational.WritebackTableDef("sales_wb", null,
                List.of(new org.eclipse.daanse.olap.metadata.relational.WritebackTableDef.Attribute("Shelf",
                        sales.columnNamed("store_id"))),
                List.of(new org.eclipse.daanse.olap.metadata.relational.WritebackTableDef.Measure("Price",
                        sales.columnNamed("amount"), true)));
        assertThatThrownBy(() -> Olap.catalog("Shop").cube("Sales", c -> c.facts(RelationalSource.Table.of(sales))
                .aggregationTable(noCount).writeback(writeback).measure("Amount")).build())
                .hasMessageContaining("aggregation table shop.sales: has no fact count column")
                .hasMessageContaining("writes Shelf, which the cube does not use")
                .hasMessageContaining("writes Price, which is no measure of the cube");
        assertThatThrownBy(() -> Olap.catalog("Shop").cube("Sales", c -> c.writeback(writeback).measure("Amount"))
                .build()).hasMessageContaining("names aggregation tables or a writeback table, but no facts");
        PhysicalCubeDef cube = (PhysicalCubeDef) Olap.catalog("Shop").cube("Sales", c -> c
                .facts(RelationalSource.Table.of(sales)).aggregationExclude(
                        new org.eclipse.daanse.olap.metadata.relational.AggregationExcludeDef(null, "agg_.*_old", true))
                .measure("Amount")).build().cubes().get(0);
        assertThat(cube.facts().orElseThrow().aggregationExcludes()).hasSize(1);
    }

    @Test
    void aRoleSaysWhatItMayReadOfSchemasTablesAndColumns() {
        TableDef sales = SHOP.table("sales").orElseThrow();
        CatalogDef catalog = Olap.catalog("Shop").schema(SHOP).cube("Sales", c -> c.measure("Amount"))
                .role("Clerk", r -> r.databaseSchema(SHOP, org.eclipse.daanse.olap.metadata.model.Access.Database.CUSTOM,
                        List.of(new org.eclipse.daanse.olap.metadata.model.Grants.TableGrant(sales,
                                org.eclipse.daanse.olap.metadata.model.Access.Database.CUSTOM,
                                List.of(new org.eclipse.daanse.olap.metadata.model.Grants.ColumnGrant(
                                        sales.columnNamed("amount"),
                                        org.eclipse.daanse.olap.metadata.model.Access.Database.NONE))))))
                .build();
        var grant = catalog.roles().get(0).databaseGrants().get(0);
        assertThat(grant.schema()).isSameAs(SHOP);
        assertThat(grant.tables().get(0).columns().get(0).column()).isSameAs(sales.columnNamed("amount"));
        assertThatThrownBy(() -> new org.eclipse.daanse.olap.metadata.model.Grants.ColumnGrant(sales.columnNamed("amount"),
                org.eclipse.daanse.olap.metadata.model.Access.Database.CUSTOM)).hasMessageContaining("CUSTOM says nothing");
    }

    @Test
    void theOlapElementsCarryTheirBindingsAndAnArtWithoutTablesCarriesNone() {
        TableDef sales = SHOP.table("sales").orElseThrow();
        TableDef store = SHOP.table("store").orElseThrow();
        CatalogDef catalog = Olap.catalog("Shop").schema(SHOP)
                .cube("Sales", c -> c.facts(RelationalSource.Table.of(sales))
                        .dimension("Store", d -> d.hierarchy(h -> h.rows(RelationalSource.Table.of(store),
                                store.columnNamed("id")).level("City", l -> l.columns(LevelColumns.of(
                                        store.columnNamed("city"), null, null, List.of(), null)))))
                        .join("Store", UsageJoin.by(sales.columnNamed("store_id")))
                        .measure("Amount", m -> m.column(sales.columnNamed("amount"))
                                .aggregator(AggregatorDef.Standard.SUM)))
                .build();
        assertThat(catalog.schemas()).containsExactly(SHOP);
        PhysicalCubeDef cube = (PhysicalCubeDef) catalog.cubes().get(0);
        assertThat(cube.facts().orElseThrow().source()).isEqualTo(RelationalSource.Table.of(sales));
        assertThat(cube.dimensions().get(0).join().foreignKeyColumn()).contains(sales.columnNamed("store_id"));
        ExplicitHierarchyDef geo = (ExplicitHierarchyDef) cube.dimensions().get(0).dimension().hierarchies().get(0);
        assertThat(geo.rows().orElseThrow().primaryKey()).isSameAs(store.columnNamed("id"));
        assertThat(geo.levels().get(0).columns().orElseThrow().firstKey()).isSameAs(store.columnNamed("city"));
        assertThat(cube.measures().get(0).column()).contains(sales.columnNamed("amount"));

        CatalogDef programmatic = Olap.catalog("Shop").cube("Sales", c -> c
                .dimension("Store", d -> d.hierarchy(h -> h.level("City"))).measure("Amount")).build();
        PhysicalCubeDef plain = (PhysicalCubeDef) programmatic.cubes().get(0);
        assertThat(plain.facts()).isEmpty();
        assertThat(plain.measures().get(0).column()).isEmpty();
        assertThat(plain.dimensions().get(0).join()).isNull();
        assertThat(programmatic.schemas()).isEmpty();
        assertThat(ColumnDef.field("Seq").table()).isNull();
    }
}
