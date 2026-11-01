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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;


import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.dmv.parser.api.DmvParserProvider;
import org.eclipse.daanse.olap.api.aggregator.CustomAggregatorFactory;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.calc.base.compiler.BaseExpressionCompilerFactory;
import org.eclipse.daanse.olap.api.result.CellReader;
import org.eclipse.daanse.olap.api.result.CellValue;
import org.eclipse.daanse.olap.api.result.NullValue;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.eclipse.daanse.olap.evaluator.EvaluatorImpl;
import org.eclipse.daanse.olap.core.LoggingEventBus;

/**
 * A context over a catalog the provider holds in memory.
 *
 * <p>It answers the metadata half of {@code Context} fully: name, roles, function service,
 * parser, compiler factory. The evaluation half refuses by name until the evaluation stage
 * of this provider is in place, so a caller learns what is missing instead of meeting a
 * {@code null}.
 *
 * <p>There is no data source and no dialect, and that is not a gap: this provider answers
 * from values it holds, not from a database.
 */
public class MemoryContext extends AbstractBasicContext<Connection> {

    /** A cube whose every cell is empty, used when nobody supplied a reader. */
    private static final CellReader EMPTY_CELLS = new CellReader() {
        @Override
        public CellValue get(Evaluator evaluator) {
            return NullValue.INSTANCE;
        }

        @Override
        public int getMissCount() {
            return 0;
        }

        @Override
        public boolean isDirty() {
            return false;
        }
    };

    private final MemoryCatalog catalog;
    private final FunctionService functionService;
    private final MdxParserProvider parserProvider;
    private final ExpressionCompilerFactory compilerFactory = new BaseExpressionCompilerFactory();
    private final Semaphore queryLimit = new Semaphore(Integer.MAX_VALUE);
    private final CellReader cellReader;

    MemoryContext(MemoryCatalog catalog, FunctionService functionService,
            MdxParserProvider parserProvider, CellReader cellReader) {
        this.catalog = catalog;
        this.cellReader = cellReader == null ? EMPTY_CELLS : cellReader;
        this.functionService = functionService;
        this.parserProvider = parserProvider;
        this.eventBus = new LoggingEventBus();
        // The engine looks things up through the catalog while it resolves a query,
        // and for that the catalog needs a connection back into this context.
        catalog.internalConnection(new MemoryConnection(this));
        catalog.context(this);
    }

    /**
     * A context over {@code catalog}.
     *
     * @param functionService the registry the MDX functions come from
     * @param parserProvider  the MDX parser, or {@code null} while only metadata is read
     */
    public static MemoryContext over(MemoryCatalog catalog, FunctionService functionService,
            MdxParserProvider parserProvider) {
        return over(catalog, functionService, parserProvider, null);
    }

    /**
     * A context over {@code catalog} whose cells come from {@code cellReader}.
     *
     * @param cellReader where cell values come from, or {@code null} for a cube of empty
     *                   cells
     */
    public static MemoryContext over(MemoryCatalog catalog, FunctionService functionService,
            MdxParserProvider parserProvider, CellReader cellReader) {
        return new MemoryContext(catalog, functionService, parserProvider, cellReader);
    }

    /** A catalog reader over this context, with the catalog's default role. */
    public org.eclipse.daanse.olap.api.catalog.CatalogReader getCatalogReader() {
        return new MemoryCatalogReader(catalog, catalog.getDefaultRole(), this);
    }

    /** The catalog this context serves. */
    public MemoryCatalog getCatalog() {
        return catalog;
    }

    // ---- metadata -------------------------------------------------------------

    @Override
    public String getName() {
        return catalog.getName();
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.empty();
    }

    @Override
    public List<String> getAccessRoles() {
        return List.of();
    }

    @Override
    public FunctionService getFunctionService() {
        return functionService;
    }

    @Override
    public MdxParserProvider getMdxParserProvider() {
        if (parserProvider == null) {
            throw new UnsupportedOperationException(
                    "MemoryContext has no MDX parser: pass one to the builder to parse queries");
        }
        return parserProvider;
    }

    @Override
    public Optional<DmvParserProvider> getDmvParserProvider() {
        return Optional.empty();
    }

    @Override
    public ExpressionCompilerFactory getExpressionCompilerFactory() {
        return compilerFactory;
    }

    @Override
    public ExpressionCompiler createProfilingCompiler(ExpressionCompiler compiler) {
        return compiler;
    }

    @Override
    public ExpressionCompiler createDependencyTestingCompiler(ExpressionCompiler compiler) {
        return compiler;
    }

    @Override
    public Semaphore getQueryLimitSemaphore() {
        return queryLimit;
    }

    // ---- no database ----------------------------------------------------------

    @Override
    public org.eclipse.daanse.olap.api.agg.AggregationFactory getAggragationFactory() {
        throw new UnsupportedOperationException(
                "MemoryContext has no aggregation factory: aggregation over segments is a"
                        + " relational concern");
    }

    @Override
    public List<CustomAggregatorFactory> getCustomAggregators() {
        return List.of();
    }

    // ---- evaluation, stage two ------------------------------------------------

    @Override
    public Connection getConnectionWithDefaultRole() {
        MemoryConnection connection = new MemoryConnection(this);
        addConnection(connection);
        return connection;
    }

    @Override
    public Connection getConnection(ConnectionProps props) {
        return getConnectionWithDefaultRole();
    }

    @Override
    public Evaluator createEvaluator(Statement statement) {
        // An evaluator asked for outside a running query still gets an execution to belong
        // to. Functions read query timing and check for cancellation through it, and a root
        // without one turns an ordinary expression into a null pointer.
        return createEvaluator(new org.eclipse.daanse.olap.execution.ExecutionImpl(
                statement, java.util.Optional.empty()));
    }

    /**
     * An evaluator for a running execution.
     *
     * <p>The interface only offers the statement, but a function that asks for query
     * timing needs the execution behind it, so the provider takes this way in when it has
     * one.
     */
    public Evaluator createEvaluator(org.eclipse.daanse.olap.api.execution.Execution execution) {
        return evaluatorOver(new MemoryEvaluatorRoot(execution));
    }

    private Evaluator evaluatorOver(MemoryEvaluatorRoot root) {
        EvaluatorImpl evaluator = (EvaluatorImpl) EvaluatorImpl.create(root);
        root.slicerEvaluator(evaluator);
        evaluator.setCellReader(cellReader);
        return evaluator;
    }

    @Override
    public Evaluator createDummyEvaluator(Statement statement) {
        return createEvaluator(statement);
    }
}
