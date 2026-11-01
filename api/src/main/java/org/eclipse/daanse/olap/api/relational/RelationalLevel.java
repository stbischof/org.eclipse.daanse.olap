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

import java.util.List;

import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.sql.SqlExpression;
import org.eclipse.daanse.sql.model.type.Datatype;

/** A level whose members are read from columns of a relational database. */
public interface RelationalLevel extends Level {

    /** The expressions the members of this level are ordered by; {@code null} when none is declared. */
    List<? extends SqlExpression> getOrdinalExps();

    /** The SQL type of the key column. */
    Datatype getDatatype();
}
