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

package org.eclipse.daanse.olap.api.result;

import java.util.List;

/**
 * The rows behind a cell, read forward once. How a provider finds them is its own business: a
 * relational provider runs SQL, a provider that holds its facts hands them out directly.
 */
public interface DrillThroughRows extends AutoCloseable {

    /** The column names, in the order {@link #getValue(int)} addresses them. */
    List<String> getColumnNames();

    /** Moves to the next row; {@code false} when there is none. */
    boolean next();

    /** The value of the current row in the column with the given zero-based index. */
    Object getValue(int columnIndex);

    /** Releases what the rows hold; idempotent. */
    @Override
    void close();
}
