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
/**
 * The description of a catalog. Two kinds of types: the named nodes other parts point at (catalog,
 * cube, dimension, hierarchy, level, measure, role) are classes with identity and no
 * {@code equals}, so that two usages of one dimension are the same object; everything else is a
 * record. All of it is immutable and built by
 * {@link org.eclipse.daanse.olap.metadata.Olap#catalog(String)}.
 */
@org.osgi.annotation.bundle.Export
@org.osgi.annotation.versioning.Version("0.0.1")
package org.eclipse.daanse.olap.metadata.model;
