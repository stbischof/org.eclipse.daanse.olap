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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.findings;

/** Where on the way from a source to an answer a finding was made. */
public enum Layer {
    /** The model file, before anything reads it. */
    MODEL,
    /** A connector that translates a source into the description of a catalog. */
    CONNECTOR,
    /** The description of a catalog. */
    DESCRIPTION,
    /** The engine binding the description to its data. */
    BINDING,
    /** The data the catalog reads. */
    DATA,
    /** A query. */
    QUERY
}
