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
package org.eclipse.daanse.olap.metadata.model;

import java.util.List;
import java.util.function.Function;

/**
 * An MDX function written in Java. It takes scalar arguments and gives a scalar:
 * {@code Margin([Measures].[Revenue], [Measures].[Cost])}.
 *
 * @param parameters the kind of every argument, which also fixes how many there are
 * @param body       gets the evaluated arguments, numbers as {@link Double}, an empty cell as
 *                   {@code null}, and answers the value; {@code null} for an empty cell
 */
public record FunctionDef(String name, ValueKind returns, List<ValueKind> parameters,
        Function<Object[], Object> body, String description) {

    public FunctionDef {
        parameters = List.copyOf(parameters);
    }
}
