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
 */
package org.eclipse.daanse.olap.metadata.relational;

import java.util.List;

import org.eclipse.daanse.olap.metadata.model.ValueKind;

/**
 * A value computed by SQL over the row, where a level, a measure or a property reads an expression instead of a
 * column -- {@code price * quantity}, say. The expression is given per dialect; the one whose dialect list names
 * the database in use is taken, else the one that names none.
 *
 * @param name what the SQL may call the value
 * @param type the kind of value, or {@code null} when the catalog does not say
 * @param sqls the expression, one per set of dialects
 */
public record SqlExpressionColumnDef(String name, ValueKind type, List<SqlStatement> sqls) implements ColumnDef {

    public SqlExpressionColumnDef {
        sqls = List.copyOf(sqls);
    }
}
