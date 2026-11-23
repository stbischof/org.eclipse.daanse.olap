/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api.query.component;

import java.io.PrintWriter;

public sealed interface QueryComponent permits CalculatedFormula, CellProperty, DimensionExpression, DmvQuery,
        DrillThrough, Explain, Formula, HierarchyExpression, Id, LevelExpression, Literal, MemberProperty,
        NamedSetExpression, ParameterExpression, Query, QueryAxis, Refresh, ResolvedFunCall, Subcube,
        TransactionCommand, UnresolvedFunCall, Update, MemberExpression, UpdateClause, SqlQuery {

    /**
     * Returns an array of the object's children. Those which are not are ignored.
     */
    Object[] getChildren();

    /**
     * Writes a string representation of this parse tree node to the given writer.
     *
     * @param pw writer
     */
    void unparse(PrintWriter pw);

    /**
     * Returns the plan that Mondrian intends to use to execute this query.
     *
     * @param pw Print writer
     */
    void explain(PrintWriter pw);
}
