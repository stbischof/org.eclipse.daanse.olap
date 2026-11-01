/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.provider.memory;

import java.util.Locale;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.connection.ConnectionBase;
import org.eclipse.daanse.olap.execution.StatementImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection to a catalog this provider holds.
 *
 * <p>Parsing, validating, compiling and evaluating all work, because those are the engine's
 * own machinery in {@code common}. Executing a whole query into a {@code Result} does not
 * yet, and says so by name.
 */
public class MemoryConnection extends ConnectionBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryConnection.class);

    private final MemoryContext context;
    private static final java.util.concurrent.atomic.AtomicLong IDS =
            new java.util.concurrent.atomic.AtomicLong();

    private final long id = IDS.incrementAndGet();
    private Role role;
    private Statement internalStatement;

    MemoryConnection(MemoryContext context) {
        this.context = context;
        this.role = context.getCatalog().getDefaultRole();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Context<?> getContext() {
        return context;
    }

    @Override
    public Catalog getCatalog() {
        return context.getCatalog();
    }

    @Override
    public CatalogReader getCatalogReader() {
        return context.getCatalogReader();
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Statement createStatement() {
        return new StatementImpl(this);
    }

    @Override
    public org.eclipse.daanse.olap.api.query.component.QueryComponent parseStatement(String mdx) {
        return parseStatement(createStatement(), mdx, context.getFunctionService(), false);
    }


    @Override
    public void close() {
        // Nothing to release: this provider holds no connection to anything.
    }

    @Override
    public org.eclipse.daanse.olap.api.result.Scenario createScenario() {
        throw new UnsupportedOperationException(
                "MemoryConnection.createScenario is not part of this provider");
    }

    @Override
    public org.eclipse.daanse.olap.api.result.Result execute(
            org.eclipse.daanse.olap.api.query.component.Query query) {
        return execute(new org.eclipse.daanse.olap.execution.ExecutionImpl(
                query.getStatement(), java.util.Optional.empty()));
    }

    @Override
    /**
     * Runs a query and materialises its result.
     *
     * <p>One pass: this provider never defers a cell, so there is nothing to fetch in a
     * batch and nothing to evaluate a second time.
     */
    public org.eclipse.daanse.olap.api.result.Result execute(
            org.eclipse.daanse.olap.api.execution.Execution execution) {
        execution.start();
        try {
            org.eclipse.daanse.olap.evaluator.EvaluatorImpl evaluator =
                    (org.eclipse.daanse.olap.evaluator.EvaluatorImpl)
                            context.createEvaluator(execution);
            return new MemoryResult(execution, evaluator);
        } finally {
            execution.end();
        }
    }

    @Override
    public org.eclipse.daanse.olap.api.cache.CacheControl getCacheControl(java.io.PrintWriter a0) {
        throw new UnsupportedOperationException(
                "MemoryConnection.getCacheControl is not part of this provider");
    }

    /**
     * The statement the engine uses for its own lookups while it resolves a query. One per
     * connection, created on first use.
     */
    @Override
    public org.eclipse.daanse.olap.api.execution.Statement getInternalStatement() {
        if (internalStatement == null) {
            internalStatement = new StatementImpl(this);
        }
        return internalStatement;
    }

    @Override
    public org.eclipse.daanse.olap.api.result.Scenario getScenario() {
        throw new UnsupportedOperationException(
                "MemoryConnection.getScenario is not part of this provider");
    }

    @Override
    public org.eclipse.daanse.olap.api.query.component.Expression parseExpression(java.lang.String a0) {
        throw new UnsupportedOperationException(
                "MemoryConnection.parseExpression is not part of this provider");
    }

    @Override
    public void setScenario(org.eclipse.daanse.olap.api.result.Scenario a0) {
        throw new UnsupportedOperationException(
                "MemoryConnection.setScenario is not part of this provider");
    }
}
