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
/**
 * The relational half of the catalog description: where the facts and the members of a catalog lie when they
 * lie in tables -- schemas, tables and columns, the sources a cube or a hierarchy reads (a table, a join, a SQL
 * select, an inline table), aggregation tables, the writeback table, and the bindings that tie a cube, a
 * hierarchy, a level, a measure, a property or a usage to its columns.
 *
 * <p>An engine that reads tables needs all of it; one that gets its members and cells through a source of its own
 * ignores it, and every binding on an OLAP element is optional. Schema, table and physical column are identities
 * (two tables of one name in two schemas are two tables); sources and bindings are values, so a source re-aliased
 * is a new value sharing its parts, not a deep copy.
 */
@org.osgi.annotation.bundle.Export
@org.osgi.annotation.versioning.Version("0.0.1")
package org.eclipse.daanse.olap.metadata.relational;
