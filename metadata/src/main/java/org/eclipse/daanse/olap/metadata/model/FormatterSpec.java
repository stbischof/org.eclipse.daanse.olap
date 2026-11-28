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

import java.util.function.Function;

/** How a value becomes text when a format string is not enough. */
public sealed interface FormatterSpec {

    /** A formatter the runtime looks up by name. */
    record ByName(String name) implements FormatterSpec {
    }

    /** A formatter given as a function from the value to its text. */
    record ByInstance(Function<Object, String> formatter) implements FormatterSpec {
    }
}
