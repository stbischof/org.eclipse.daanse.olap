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
 * Where a catalog is described: {@link org.eclipse.daanse.olap.metadata.Olap} starts the builders,
 * {@link org.eclipse.daanse.olap.metadata.DescriptionRules} are what a description is held to, and a
 * {@link org.eclipse.daanse.olap.metadata.CatalogDescriptionSupplier} hands it to a context.
 */
@org.osgi.annotation.bundle.Export
@org.osgi.annotation.versioning.Version("0.0.1")
package org.eclipse.daanse.olap.metadata;
