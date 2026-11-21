/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.query.base;

import static org.eclipse.daanse.olap.query.base.MdxToQueryConverter.convertColumns;
import static org.eclipse.daanse.olap.query.base.MdxToQueryConverter.convertCubeClause;
import static org.eclipse.daanse.olap.query.base.MdxToQueryConverter.convertFormulaList;
import static org.eclipse.daanse.olap.query.base.MdxToQueryConverter.convertName;
import static org.eclipse.daanse.olap.query.base.MdxToQueryConverter.convertParameterList;
import static org.eclipse.daanse.olap.query.base.MdxToQueryConverter.convertQueryAxis;
import static org.eclipse.daanse.olap.query.base.MdxToQueryConverter.convertQueryAxisList;
import static org.eclipse.daanse.olap.query.base.MdxToQueryConverter.getExpressionByCompoundId;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.CallStatement;
import org.eclipse.daanse.mdx.model.api.CreateMemberStatement;
import org.eclipse.daanse.mdx.model.api.CreateSetStatement;
import org.eclipse.daanse.mdx.model.api.CreateSubcubeStatement;
import org.eclipse.daanse.mdx.model.api.DrillthroughStatement;
import org.eclipse.daanse.mdx.model.api.DropMemberStatement;
import org.eclipse.daanse.mdx.model.api.DropSetStatement;
import org.eclipse.daanse.mdx.model.api.DropSubcubeStatement;
import org.eclipse.daanse.mdx.model.api.ExplainStatement;
import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.model.api.RefreshStatement;
import org.eclipse.daanse.mdx.model.api.ReturnItem;
import org.eclipse.daanse.mdx.model.api.SelectStatement;
import org.eclipse.daanse.mdx.model.api.TransactionKind;
import org.eclipse.daanse.mdx.model.api.TransactionStatement;
import org.eclipse.daanse.mdx.model.api.UpdateStatement;
import org.eclipse.daanse.mdx.model.api.select.Allocation;
import org.eclipse.daanse.olap.api.Command;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.query.QueryProvider;
import org.eclipse.daanse.olap.api.query.component.CellProperty;
import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.Explain;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.Refresh;
import org.eclipse.daanse.olap.api.query.component.Subcube;
import org.eclipse.daanse.olap.api.query.component.TransactionCommand;
import org.eclipse.daanse.olap.api.query.component.Update;
import org.eclipse.daanse.olap.api.query.component.UpdateClause;
import org.eclipse.daanse.olap.query.component.DrillThroughImpl;
import org.eclipse.daanse.olap.query.component.ExplainImpl;
import org.eclipse.daanse.olap.query.component.QueryImpl;
import org.eclipse.daanse.olap.query.component.RefreshImpl;
import org.eclipse.daanse.olap.query.component.TransactionCommandImpl;
import org.eclipse.daanse.olap.query.component.UpdateImpl;

public class QueryProviderImpl implements QueryProvider {

    @Override
    public QueryComponent createQuery(Statement statement, MdxStatement mdxStatement, boolean strictValidation) {

        return switch (mdxStatement) {
        case SelectStatement select -> createQuery(statement, select, strictValidation);
        case DrillthroughStatement drillthrougt -> createDrillThrough(statement, drillthrougt, strictValidation);
        case ExplainStatement explain -> createExplain(statement, explain, strictValidation);
        case RefreshStatement refresh -> createRefresh(refresh);
        case UpdateStatement update -> createUpdate(update);
        case TransactionStatement transaction -> createTransaction(transaction);
        // session calculated members and sets are not kept yet
        case CreateMemberStatement _, CreateSetStatement _, DropMemberStatement _, DropSetStatement _ ->
            throw new UnsupportedOperationException(
                "CREATE MEMBER, CREATE SET, DROP MEMBER and DROP SET statements are not supported");
        // a session subcube is not kept yet
        case CreateSubcubeStatement _, DropSubcubeStatement _ ->
            throw new UnsupportedOperationException("CREATE SUBCUBE and DROP SUBCUBE statements are not supported");
        // there are no stored procedures to call
        case CallStatement _ -> throw new UnsupportedOperationException("CALL statements are not supported");
        };
    }

    @Override
    public Refresh createRefresh(RefreshStatement refreshStatement) {
        return new RefreshImpl(convertName(refreshStatement.cubeName()));
    }

    @Override
    public TransactionCommand createTransaction(TransactionStatement transactionStatement) {
        return new TransactionCommandImpl(convertTransactionKind(transactionStatement.kind()));
    }

    private static Command convertTransactionKind(TransactionKind kind) {
        return switch (kind) {
        case BEGIN -> Command.BEGIN;
        case COMMIT -> Command.COMMIT;
        case ROLLBACK -> Command.ROLLBACK;
        };
    }

    @Override
    public Update createUpdate(UpdateStatement updateStatement) {
        return new UpdateImpl(convertName(updateStatement.cubeName()), convertUpdateClauses(updateStatement.updateClauses()));
    }

    private List<UpdateClause> convertUpdateClauses(List<? extends org.eclipse.daanse.mdx.model.api.select.UpdateClause> list) {
        if (list != null) {
            return list.stream().map(this::convertUpdateClause).toList();
        }
        return List.of();
    }

    private UpdateClause convertUpdateClause(org.eclipse.daanse.mdx.model.api.select.UpdateClause updateClause) {
        Expression tuple = MdxToQueryConverter.getExpression(updateClause.tupleExp());
        Expression value = MdxToQueryConverter.getExpression(updateClause.valueExp());
        Allocation allocation = updateClause.allocation();
        // The BY expression is optional, and no client that sends USE_EQUAL_ALLOCATION
        // sends one - taking it unconditionally made every ordinary UPDATE CUBE fail
        // to convert, after it had parsed.
        Expression weight = updateClause.weight().map(MdxToQueryConverter::getExpression).orElse(null);
        return new UpdateImpl.UpdateClauseImpl(tuple, value, allocation, weight);
    }

    @Override
    public Explain createExplain(Statement statement, ExplainStatement explainStatement, boolean strictValidation) {
        QueryComponent queryPart = createQuery(statement, explainStatement.mdxStatement(), strictValidation);
        return new ExplainImpl(queryPart);
    }

    @Override
    public DrillThrough createDrillThrough(Statement statement, DrillthroughStatement drillthroughStatement,
            boolean strictValidation) {
        Query query = createQuery(statement, drillthroughStatement.selectStatement(), strictValidation);
        List<Expression> returnList = new ArrayList<>();
        if (drillthroughStatement.returnItems() != null) {
            for (ReturnItem returnItem : drillthroughStatement.returnItems()) {
                // a column wrapped in Key, Name, Caption, ... picks which value of the attribute is returned; that
                // choice is not supported yet, so the column is refused rather than read as the bare one
                if (returnItem.function().isPresent()) {
                    throw new UnsupportedOperationException("DRILLTHROUGH RETURN with a column wrapped in "
                            + returnItem.function().get() + " is not supported");
                }
                returnList.add(getExpressionByCompoundId(returnItem.compoundId()));
            }
        }
        return new DrillThroughImpl(query, drillthroughStatement.maxRows().orElse(0),
                drillthroughStatement.firstRowSet().orElse(0), returnList);
    }

    @Override
    public Query createQuery(Statement statement, SelectStatement selectStatement, boolean strictValidation) {
        Subcube subcube = convertCubeClause(selectStatement.selectCubeClause());
        List<Formula> formulaList = convertFormulaList(selectStatement.selectWithClauses());
        List<QueryAxis> axesList = convertQueryAxisList(selectStatement.selectQueryClause());
        QueryAxis slicerAxis = convertQueryAxis(selectStatement.selectSlicerAxisClause());
        List<CellProperty> cellProps = convertParameterList(selectStatement.selectCellPropertyListClause());

        return createQuery( //
                statement, //
                formulaList.toArray(Formula[]::new), //
                axesList.toArray(QueryAxis[]::new), //
                subcube, //
                slicerAxis, //
                cellProps.toArray(CellProperty[]::new), //
                strictValidation);
    }

    @Override
    public Query createQuery(Statement statement, Formula[] formula, QueryAxis[] axes, Subcube subcube,
            QueryAxis slicerAxis, CellProperty[] cellProps, boolean strictValidation) {
        return new QueryImpl(statement, formula, axes, subcube, slicerAxis, cellProps, strictValidation);
    }

    }
