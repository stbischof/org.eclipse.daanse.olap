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
package org.eclipse.daanse.olap.result;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.query.component.AxisOrdinal;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.result.CellSetAxis;
import org.eclipse.daanse.olap.api.result.CellSetMetaData;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.api.result.SubtotalVisibility;
import org.eclipse.daanse.olap.common.ExecuteDurationUtil;
import org.eclipse.daanse.olap.execution.ExecutionImpl;
import org.eclipse.daanse.olap.execution.PreparedStatementImpl;
import org.eclipse.daanse.olap.execution.StatementImpl;
import org.eclipse.daanse.olap.query.component.QueryAxisImpl;

public class CellSetImpl extends ExecutionImpl implements CellSet {

    private StatementImpl statement;
    private CellSetMetaData metaData;
    private List<CellSetAxis> axisList = new ArrayList<>();
    private CellSetAxis filterAxis;
    private Result result;
    protected boolean closed;
    private final Query query;

    public CellSetImpl(StatementImpl statement) {
        super(statement, ExecuteDurationUtil.executeDurationValue(statement.getConnection().getContext()));
        this.statement = statement;
        query = statement.getQuery();
        this.closed = false;
        if (statement instanceof PreparedStatementImpl ps) {
            this.metaData = ps.getCellSetMetaData();
        } else {
            this.metaData =
                new CellSetMetaDataImpl(
                    statement, query);
        }

    }

    @Override
    public CellSetMetaData getMetaData() {
        return this.metaData;
    }

    @Override
    public List<CellSetAxis> getAxes() {
        return axisList;
    }

    @Override
    public CellSetAxis getFilterAxis() {
        return filterAxis;
    }

    @Override
    public Cell getCell(List<Integer> coordinates) {
        int[] coords = new int[coordinates.size()];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = coordinates.get(i);
        }
        return getCellInternal(coords);
    }

    @Override
    public Statement getStatement() {
        return statement;
    }

    private Cell getCellInternal(int[] pos) {
        Cell cell;
        try {
            cell = result.getCell(pos);
        } catch (OlapRuntimeException e) {
            if (e.getMessage().indexOf("coordinates out of range") >= 0) {
                int[] dimensions = new int[getAxes().size()];
                for (int i = 0; i < axisList.size(); i++) {
                    dimensions[i] = axisList.get(i).getPositions().size();
                }
                throw new IndexOutOfBoundsException(
                    new StringBuilder("Cell coordinates (").append(getCoordsAsString(pos))
                        .append(") fall outside CellSet bounds (")
                        .append(getCoordsAsString(dimensions)).append(")").toString());
            } else if (e.getMessage().indexOf(
                "coordinates should have dimension") >= 0)
            {
                throw new IllegalArgumentException(
                    "Cell coordinates should have dimension "
                        + axisList.size());
            } else {
                throw e;
            }
        }
        return new CellImpl(result, pos, this, cell);
    }

    private static String getCoordsAsString(int[] pos) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < pos.length; i++) {
            int po = pos[i];
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(po);
        }
        return buf.toString();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        this.closed = true;
        if (this.result != null) {
            this.result.close();
        }
    }

    @Override
    public void execute() {
        result =
            statement.getConnection().execute(
                this);

        // initialize axes
        org.eclipse.daanse.olap.api.result.Axis[] axes = result.getAxes();
        QueryAxis[] queryAxes = result.getQuery().getAxes();
        assert axes.length == queryAxes.length;
        for (int i = 0; i < axes.length; i++) {
            Axis axis = axes[i];
            QueryAxis queryAxis = queryAxes[i];
            axisList.add(
                new CellSetAxisImpl(
                    this, queryAxis, axis));
        }

        // initialize filter axis
        QueryAxis queryAxis = result.getQuery().getSlicerAxis();
        final Axis axis = result.getSlicerAxis();
        if (queryAxis == null) {
            // Dummy slicer axis.
            queryAxis =
                new QueryAxisImpl(
                    false, null, AxisOrdinal.StandardAxisOrdinal.SLICER,
                    SubtotalVisibility.Undefined);
        }
        filterAxis =
            new CellSetAxisImpl(this, queryAxis, axis);
    }
}
