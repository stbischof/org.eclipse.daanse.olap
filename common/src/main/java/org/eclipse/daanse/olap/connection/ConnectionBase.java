/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.connection;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.parser.api.MdxParser;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.db.DatabaseSchema;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.ExpressionProvider;
import org.eclipse.daanse.olap.api.query.QueryProvider;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.dmv.parser.api.DmvParserProvider;
import org.eclipse.daanse.olap.common.SqlQueryImpl;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.exceptions.FailedToParseQueryException;
import org.eclipse.daanse.olap.guard.GuardDatabaseCatalog;
import org.eclipse.daanse.olap.query.base.ExpressionProviderImpl;
import org.eclipse.daanse.olap.query.base.QueryProviderImpl;
import org.eclipse.daanse.olap.query.base.StatementRouter;
import org.eclipse.daanse.olap.query.component.DmvQueryImpl;
import org.eclipse.daanse.sql.guard.api.SqlGuard;
import org.eclipse.daanse.sql.guard.api.SqlGuardFactory;
import org.eclipse.daanse.sql.guard.api.exception.GuardException;
import org.eclipse.daanse.sql.guard.api.exception.UnparsableStatementGuardException;
import org.slf4j.Logger;
import org.eclipse.daanse.olap.api.relational.RelationalContext;


/**
 * ConnectionBase implements some of the methods in
 * {@link Connection}.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public abstract class ConnectionBase implements Connection {

    QueryProvider queryProvider = new QueryProviderImpl();

    ExpressionProvider expressionProvider = new ExpressionProviderImpl();

    private Optional<SqlGuard> oSqlGuard = Optional.empty();

	private String currentSchemaName;

    protected ConnectionBase() {
//        getContext().getSqlGuardFactory();
    }

    protected abstract Logger getLogger();


    @Override
	public Query parseQuery(String query) {
        return (Query) parseStatement(query);
    }

    /**
     * Parses a query, with specified function table and the mode for strict
     * validation(if true then invalid members are not ignored).
     *
     * This method is only used in testing and by clients that need to
     * support customized parser behavior. That is why this method is not part
     * of the Connection interface.
     *
     * 
     *
     * @param statement Evaluation context
     * @param queryToParse MDX query that requires special parsing
     * @param funTable Customized function table to use in parsing
     * @param strictValidation If true, do not ignore invalid members
     * @return Query the corresponding Query object if parsing is successful
     * @throws OlapRuntimeException if parsing fails
     */
    public QueryComponent parseStatement(
        Statement statement,
        String queryToParse,
        FunctionService funTable,
        boolean strictValidation)
    {
        if (getLogger().isDebugEnabled()) {
            String s = new StringBuilder().append(Util.NL).append(queryToParse.replaceAll("[\n\r]", "_")).toString();
            getLogger().debug(s);
        }

        // The kind is decided before any parser runs, not by which parser happens to fail:
        // decision-by-exception answered a broken DMV with the SQL guard's complaint and the
        // real parse error was gone.
        StatementRouter.Kind kind = StatementRouter.classify(queryToParse);
        if (kind == StatementRouter.Kind.SQL) {
            return parseSql(statement, queryToParse, funTable, strictValidation);
        }
        if (kind == StatementRouter.Kind.DMV) {
            return parseDmv(queryToParse);
        }

        MdxStatement mdxStatement;
        try {
            MdxParser parser = getContext().getMdxParserProvider().newParser(queryToParse,
                    funTable.getPropertyWords());
            mdxStatement = parser.parseMdxStatement();
        } catch (Exception mdxPE) {
            throw new FailedToParseQueryException(queryToParse, mdxPE);
        }
        // Conversion runs outside the catch: a conversion error is not a parse error and
        // carries its own message.
        return getQueryProvider().createQuery(statement, mdxStatement, strictValidation);
    }

    /**
     * A statement that reads as SQL goes to the guard first; only if the guard cannot even
     * parse it does the MDX parser get its turn - the classification is lexical and a rare
     * MDX statement can look like SQL, but a SQL error must read as one.
     */
    private QueryComponent parseSql(
        Statement statement,
        String queryToParse,
        FunctionService funTable,
        boolean strictValidation)
    {
        // Only a relational provider can be asked SQL.
        if (!(getContext() instanceof RelationalContext<?> relational)) {
            return parseAsMdxAfterAll(statement, queryToParse, funTable, strictValidation, null);
        }
        Optional<SqlGuardFactory> oSqlGuardFactory = relational.getSqlGuardFactory();
        if (oSqlGuardFactory.isEmpty()) {
            return parseAsMdxAfterAll(statement, queryToParse, funTable, strictValidation, null);
        }
        List<DatabaseSchema> ds = (List<DatabaseSchema>) this.getCatalogReader().getDatabaseSchemas();
        String catalogName = getCatalog().getName();
        String schemaName = currentSchemaName(relational, ds);
        org.eclipse.daanse.sql.guard.api.elements.DatabaseCatalog dc = new GuardDatabaseCatalog(catalogName, ds);
        //TODO need resolve function list from other place
        SqlGuard guard = oSqlGuardFactory.get().create(catalogName, schemaName, dc,
                List.of("sum", "avg", "min", "max", "count", "concat"), relational.getDialect());
        // TODO add white list functions
        try {
            String sanetizedSql = guard.guard(queryToParse);
            return new SqlQueryImpl(sanetizedSql, relational.getDataSource());
        } catch (UnparsableStatementGuardException uex) {
            return parseAsMdxAfterAll(statement, queryToParse, funTable, strictValidation, uex);
        } catch (GuardException guardEx) {
            throw new FailedToParseQueryException(queryToParse, guardEx);
        }
    }

    /**
     * The schema an unqualified table name means: the one the database connection is in
     * ({@code main} on DuckDB, {@code public} on PostgreSQL), the same source RolapCatalog
     * uses to name an unnamed mapping schema. When the driver does not say and the
     * catalog has exactly one schema, that one; otherwise the empty string, which resolves
     * nothing and lets the guard report the name as unknown.
     */
    private String currentSchemaName(RelationalContext<?> relational, List<DatabaseSchema> schemas) {
        if (currentSchemaName == null) {
            String fromDriver = null;
            DataSource dataSource = relational.getDataSource();
            if (dataSource != null) {
                try (java.sql.Connection connection = dataSource.getConnection()) {
                    fromDriver = connection.getSchema();
                } catch (SQLException e) {
                    getLogger().debug("default schema of catalog {} not readable", getCatalog().getName(), e);
                }
            }
            if (fromDriver == null || fromDriver.isBlank()) {
                fromDriver = schemas.size() == 1 && schemas.get(0).getName() != null ? schemas.get(0).getName() : "";
            }
            currentSchemaName = fromDriver;
        }
        return currentSchemaName;
    }
    
	private QueryComponent parseAsMdxAfterAll(
        Statement statement,
        String queryToParse,
        FunctionService funTable,
        boolean strictValidation,
        Exception sqlFailure)
    {
        try {
            MdxParser parser = getContext().getMdxParserProvider().newParser(queryToParse,
                    funTable.getPropertyWords());
            MdxStatement mdxStatement = parser.parseMdxStatement();
            return getQueryProvider().createQuery(statement, mdxStatement, strictValidation);
        } catch (Exception mdxPE) {
            throw new FailedToParseQueryException(queryToParse,
                    sqlFailure == null ? mdxPE : sqlFailure);
        }
    }



    /**
     * A DMV is its own language with its own parser service. No parser installed means the
     * query is refused with that reason - not handed to parsers that would guess wrong.
     */
    private QueryComponent parseDmv(String queryToParse) {
        Optional<DmvParserProvider> provider = getContext().getDmvParserProvider();
        if (provider.isEmpty()) {
            throw new FailedToParseQueryException(queryToParse,
                    new IllegalStateException("no DMV parser is installed"));
        }
        try {
            return new DmvQueryImpl(provider.get().newParser(queryToParse).parseDmvStatement());
        } catch (Exception dmvPE) {
            throw new FailedToParseQueryException(queryToParse, dmvPE);
        }
    }

    public QueryProvider getQueryProvider() {
        return queryProvider;
    }

    public ExpressionProvider getExpressionProvider() {
        return expressionProvider;
    }

}
