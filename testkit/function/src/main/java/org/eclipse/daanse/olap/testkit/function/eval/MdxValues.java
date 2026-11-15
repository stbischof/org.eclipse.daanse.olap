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
package org.eclipse.daanse.olap.testkit.function.eval;

import java.util.Optional;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.execution.ExecutionImpl;

/** Values from MDX, without assertions. Assertions live in MdxAssert (rolap test kit). */
public final class MdxValues {

    private MdxValues() {
    }

    public static String formattedValueOf(Connection connection, String cubeName, String expression) {
        return cellOf(connection, cubeName, expression, Optional.empty()).getFormattedValue();
    }

    /**
     * Like {@link #formattedValueOf(Connection, String, String)}, with an explicit
     * {@code FORMAT_STRING} on the calculated member.
     */
    public static String formattedValueOf(Connection connection, String cubeName, String expression,
            String formatString) {
        return cellOf(connection, cubeName, expression, Optional.of(formatString)).getFormattedValue();
    }

    public static Object valueOf(Connection connection, String cubeName, String expression) {
        return cellOf(connection, cubeName, expression, Optional.empty()).getValue();
    }

    public static Optional<Throwable> errorOf(Connection connection, String cubeName, String expression) {
        try {
            Cell cell = cellOf(connection, cubeName, expression, Optional.empty());
            return cell.isError() ? Optional.of((Throwable) cell.getValue()) : Optional.empty();
        } catch (Throwable thrown) {
            return Optional.of(thrown);
        }
    }

    /** One tuple per line, braced when compound — the same layout as MdxAssert's axis rendering. */
    public static String positionsOf(Connection connection, String cubeName, String setExpression) {
        Result result = execute(connection,
                "SELECT {" + setExpression + "} ON COLUMNS FROM " + quotedCube(cubeName));
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (Position position : result.getAxes()[0].getPositions()) {
            if (!first) {
                buf.append(System.lineSeparator());
            }
            first = false;
            boolean tuple = position.size() != 1;
            if (tuple) {
                buf.append('{');
            }
            for (int j = 0; j < position.size(); j++) {
                if (j > 0) {
                    buf.append(", ");
                }
                buf.append(position.get(j).getUniqueName());
            }
            if (tuple) {
                buf.append('}');
            }
        }
        return buf.toString();
    }

    private static Cell cellOf(Connection connection, String cubeName, String expression,
            Optional<String> formatString) {
        String mdx = "WITH MEMBER [Measures].[Foo] AS " + Util.singleQuoteString(expression)
                + formatString.map(f -> ", FORMAT_STRING = " + Util.singleQuoteString(f)).orElse("")
                + " SELECT {[Measures].[Foo]} ON COLUMNS FROM " + quotedCube(cubeName);
        return execute(connection, mdx).getCell(new int[] { 0 });
    }

    private static Result execute(Connection connection, String mdx) {
        var query = connection.parseQuery(mdx);
        var statement = query.getStatement();
        return statement.getDaanseConnection().execute(new ExecutionImpl(statement, Optional.empty()));
    }

    private static String quotedCube(String cubeName) {
        return cubeName.indexOf(' ') >= 0 ? Util.quoteMdxIdentifier(cubeName) : cubeName;
    }
}