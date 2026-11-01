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
package org.eclipse.daanse.olap.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.sql.DataSource;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.relational.RelationalContext;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.cache.CacheControl;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.db.DatabaseSchema;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.sql.guard.api.SqlGuard;
import org.eclipse.daanse.sql.guard.api.SqlGuardFactory;
import org.eclipse.daanse.sql.guard.api.elements.DatabaseCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConnectionBaseParseSqlTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RelationalContext<?> context;
    @Mock
    private Catalog catalog;
    @Mock
    private CatalogReader reader;
    @Mock
    private DatabaseSchema mainSchema;
    @Mock
    private SqlGuardFactory guardFactory;
    @Mock
    private SqlGuard guard;
    @Mock
    private DataSource dataSource;
    @Mock
    private java.sql.Connection jdbc;

    /** The least ConnectionBase that can run parseStatement. */
    private final class TestConnection extends ConnectionBase {
        @Override protected Logger getLogger() { return LoggerFactory.getLogger(TestConnection.class); }
        @Override public Context<?> getContext() { return context; }
        @Override public Catalog getCatalog() { return catalog; }
        @Override public CatalogReader getCatalogReader() { return reader; }
        // every other Connection method: throw UnsupportedOperationException

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Result execute(Query query) {
            throw new UnsupportedOperationException();
		}
		@Override
		public Statement createStatement() {
            throw new UnsupportedOperationException();
		}
		@Override
		public Locale getLocale() {
            throw new UnsupportedOperationException();
		}
		@Override
		public Expression parseExpression(String s) {
            throw new UnsupportedOperationException();
		}
		@Override
		public QueryComponent parseStatement(String mdx) {
            throw new UnsupportedOperationException();
		}
		@Override
		public void setRole(Role role) {
            throw new UnsupportedOperationException();
		}
		@Override
		public Role getRole() {
            throw new UnsupportedOperationException();
		}
		@Override
		public CacheControl getCacheControl(PrintWriter pw) {
            throw new UnsupportedOperationException();
		}
		@Override
		public Scenario getScenario() {
            throw new UnsupportedOperationException();
		}
		@Override
		public Scenario createScenario() {
            throw new UnsupportedOperationException();
		}
		@Override
		public void setScenario(Scenario scenario) {
            throw new UnsupportedOperationException();
		}
		@Override
		public long getId() {
            throw new UnsupportedOperationException();
		}
		@Override
		public Statement getInternalStatement() {
            throw new UnsupportedOperationException();
		}
		@Override
		public Result execute(Execution execution) {
            throw new UnsupportedOperationException();
		}
    }

    @Test
    void theGuardIsToldTheCatalogAndTheConnectionsDefaultSchema() throws Exception {
        when(catalog.getName()).thenReturn("FoodMart");
        when(mainSchema.getName()).thenReturn("main");
        when(reader.getDatabaseSchemas()).thenAnswer(i -> List.of(mainSchema));
        when(context.getSqlGuardFactory()).thenReturn(Optional.of(guardFactory));
        when(context.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(jdbc);
        when(jdbc.getSchema()).thenReturn("main");
        when(guardFactory.create(anyString(), anyString(), any(), anyList(), any())).thenReturn(guard);
        when(guard.guard("select * from customer")).thenReturn("SELECT \"customer\".\"id\" FROM \"main\".\"customer\"");

        QueryComponent query = new TestConnection().parseStatement(null, "select * from customer",
                mock(FunctionService.class), false);

        ArgumentCaptor<String> catalogName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> schemaName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DatabaseCatalog> guarded = ArgumentCaptor.forClass(DatabaseCatalog.class);
        verify(guardFactory).create(catalogName.capture(), schemaName.capture(), guarded.capture(), anyList(), any());
        assertThat(catalogName.getValue()).isEqualTo("FoodMart");
        assertThat(schemaName.getValue()).isEqualTo("main");
        assertThat(guarded.getValue().getName()).isEqualTo("FoodMart");
        assertThat(query).isNotNull();
    }

    @Test
    void aDriverWithoutSchemasFallsBackToTheSingleSchemaOfTheCatalog() throws Exception {
        when(catalog.getName()).thenReturn("FoodMart");
        when(mainSchema.getName()).thenReturn("public");
        when(reader.getDatabaseSchemas()).thenAnswer(i -> List.of(mainSchema));
        when(context.getSqlGuardFactory()).thenReturn(Optional.of(guardFactory));
        when(context.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(jdbc);
        when(jdbc.getSchema()).thenReturn(null);
        when(guardFactory.create(anyString(), anyString(), any(), anyList(), any())).thenReturn(guard);
        when(guard.guard(anyString())).thenReturn("SELECT 1");

        new TestConnection().parseStatement(null, "select 1 from t", mock(FunctionService.class), false);

        verify(guardFactory).create(eq("FoodMart"), eq("public"), any(), anyList(), any());
    }

    @Test
    void theDefaultSchemaIsReadOnce() throws Exception {
        when(catalog.getName()).thenReturn("FoodMart");
        when(reader.getDatabaseSchemas()).thenAnswer(i -> List.of(mainSchema));
        when(context.getSqlGuardFactory()).thenReturn(Optional.of(guardFactory));
        when(context.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(jdbc);
        when(jdbc.getSchema()).thenReturn("main");
        when(guardFactory.create(anyString(), anyString(), any(), anyList(), any())).thenReturn(guard);
        when(guard.guard(anyString())).thenReturn("SELECT 1");

        TestConnection connection = new TestConnection();
        connection.parseStatement(null, "select 1 from t", mock(FunctionService.class), false);
        connection.parseStatement(null, "select 2 from t", mock(FunctionService.class), false);

        verify(dataSource).getConnection();
    }
}