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
*/

package org.eclipse.daanse.olap.api.relational;

import org.eclipse.daanse.olap.api.result.Cell;

/** A cell whose rows come from a SQL database. */
public interface RelationalCell extends Cell {

    /**
     * Returns a SQL query that, when executed, returns drill through data for this
     * Cell.
     *
     * If the parameter {@code extendedContext} is true, then the query will include
     * all the levels (i.e. columns) of non-constraining members (i.e. members which
     * are at the "All" level).
     *
     * If the parameter {@code extendedContext} is false, the query will exclude the
     * levels (coulmns) of non-constraining members.
     *
     * The result is null if the cell is based upon a calculated member.
 */
    String getDrillThroughSQL(boolean extendedContext);
}
