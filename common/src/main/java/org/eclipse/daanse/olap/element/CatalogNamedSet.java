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
package org.eclipse.daanse.olap.element;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.query.Quoting;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.query.component.FormulaImpl;
import org.eclipse.daanse.olap.query.component.IdImpl;

/**
 * A set a catalog knows by name, above every cube.
 *
 * <p>The formula is parsed once, when the catalog is built, and left unresolved: the set has no
 * cube of its own, so the query that names it binds it in the cube it runs against. A formula
 * that cannot be parsed makes the catalog fail to load, which is where a reader of the
 * description expects to hear about it.
 */
public final class CatalogNamedSet {

    private CatalogNamedSet() {
    }

    /**
     * The set the formula stands for.
     *
     * @throws RuntimeException whatever the parser says about the formula; the caller names the
     *         set in its own message.
     */
    public static NamedSet of(Connection connection, String name, String formula) {
        Expression expression = connection.parseExpression(formula);
        return new FormulaImpl(new IdImpl(new IdImpl.NameSegmentImpl(name, Quoting.UNQUOTED)), expression)
                .getNamedSet();
    }
}
