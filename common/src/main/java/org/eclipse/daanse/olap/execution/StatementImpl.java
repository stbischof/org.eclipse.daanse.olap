/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.execution;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.DrillThroughAction;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.execution.ExecutionContext;
import org.eclipse.daanse.olap.api.execution.QueryCanceledException;
import org.eclipse.daanse.olap.api.execution.QueryTimeoutException;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.Explain;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.common.ExecuteDurationUtil;
import org.eclipse.daanse.olap.connection.ConnectionBase;
import org.eclipse.daanse.olap.execution.ExecutionImpl;
import org.eclipse.daanse.olap.result.CellImpl;
import org.eclipse.daanse.olap.result.CellSetImpl;

public class StatementImpl extends org.eclipse.daanse.olap.execution.AbstractStatement implements Statement {

    private Connection connection;
    private boolean closed;
    protected Context context;

    /**
     * Current cell set, or null if the statement is not executing anything.
     * Any method which modifies this member must synchronize
     * on the MondrianOlap4jStatement.
     */
    CellSetImpl openCellSet;

    public StatementImpl(Connection connection) {
        super(connection.getContext().getConfig().queryTimeout());
        assert connection != null;
        this.connection = connection;
        this.context = connection.getContext();
        this.closed = false;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public CellSet executeQuery(String mdx) {
        final Query query = parseQuery(mdx);
        return executeOlapQueryInternal(query);

    }

    @Override
    public CellSet executeQuery(Query query) {
        return executeOlapQueryInternal(query);

    }

    @Override
    public ResultSet executeQuery(
        String mdx,
        Optional<String> tabFields,
        int[] rowCountSlot
    )  {
        QueryComponent parseTree;
        try {
            parseTree =
                connection.parseStatement(mdx);
        } catch (OlapRuntimeException e) {
            throw new RuntimeException(
                "daanse gave exception while parsing query", e);
        }
        return executeQuery(parseTree, tabFields, rowCountSlot);
    }

    @Override
    public ResultSet executeQuery(
        QueryComponent queryComponent,
        Optional<String> tabFields,
        int[] rowCountSlot
    )  {
        if (queryComponent instanceof DrillThrough drillThrough) {
            final Query query = drillThrough.getQuery();
            query.setResultStyle(ResultStyle.LIST);
            //setQuery(query);
            CellSet cellSet = executeOlapQueryInternal(query);
            final List<Integer> coords = Collections.nCopies(
                cellSet.getAxes().size(), 0);
            final Cell cell =
                cellSet.getCell(coords);

            List<OlapElement> fields = drillThrough.getReturnList();
            if(fields.isEmpty()) {
                org.eclipse.daanse.olap.api.element.Cube rolapCube = cellSet.getMetaData().getCube();


                DrillThroughAction rolapDrillThroughAction =
                    rolapCube.getDefaultDrillThroughAction();
                if(rolapDrillThroughAction != null) {
                    fields = rolapDrillThroughAction.getOlapElements();
                }
            }

            ResultSet resultSet =
                ((CellImpl)cell).drillThroughInternal(
                    drillThrough.getMaxRowCount(),
                    drillThrough.getFirstRowOrdinal(),
                    fields,
                    true,
                    null,
                    rowCountSlot);
            if (resultSet == null) {
                throw new RuntimeException(
                    "Cannot do DrillThrough operation on the cell");
            }
            return resultSet;
        } else if (queryComponent instanceof Explain explain) {
            return null;

            //    connection,
            //    Collections.singletonList("PLAN"),
            //    Collections.singletonList(
            //        Collections.<Object>singletonList(plan)));
        } else {
            throw new RuntimeException(
                "Query does not have relational result. Use a DRILLTHROUGH query, or execute using the executeOlapQuery method.");
        }
    }

    private String explainInternal(QueryComponent query) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        query.explain(pw);
        pw.flush();
        return sw.toString();
    }

    private CellSet executeOlapQueryInternal(Query query) {
        // Close the previous open CellSet, if there is one.
        synchronized (this) {
            if (openCellSet != null) {
                final CellSetImpl cs = openCellSet;
                openCellSet = null;
                try {
                    cs.close();
                } catch (Exception e) {
                    throw new RuntimeException(
                         "Error while closing previous CellSet", e);
                }
            }

            query.setResultStyle(ResultStyle.LIST);
            //}
            this.query = query;
            openCellSet = new CellSetImpl(this);
        }
        // Release the monitor before executing, to give another thread the
        // opportunity to call cancel.
        try {
            openCellSet.execute();
        } catch (QueryCanceledException e) {
            throw new RuntimeException(
                "Query canceled", e);
        } catch (QueryTimeoutException e) {
            throw new RuntimeException(
                e.getMessage(), e);
        } catch (OlapRuntimeException e) {
            throw new RuntimeException(
                "daanse gave exception while executing query", e);
        }
        return openCellSet;
    }

    private Query parseQuery(String mdx) {
        try {
            // Create a new ExecutionImpl for parsing (similar to LocusImpl.execute(connection, ...))
            final Statement statement = connection.getInternalStatement();
            final ExecutionImpl execution = new ExecutionImpl(statement,
                ExecuteDurationUtil.executeDurationValue(connection.getContext()));

            return ExecutionContext.where(execution.asContext(), () -> {
                final Query query =
                    (Query) ((ConnectionBase)connection).parseStatement(
                        StatementImpl.this,
                        mdx,
                        context.getFunctionService(),
                        false);
                return query;
            });
        } catch (OlapRuntimeException e) {
            throw new RuntimeException(
                "daanse gave exception while parsing query", e);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            context.removeStatement(this);
            if (openCellSet != null) {
                CellSetImpl c = openCellSet;
                openCellSet = null;
                c.close();
            }
        }
    }

    @Override
    public Connection getDaanseConnection() {
        return connection;
    }
}
