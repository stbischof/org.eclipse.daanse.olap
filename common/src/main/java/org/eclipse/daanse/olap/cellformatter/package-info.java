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
 *   Stefan Bischof (bipolis.org) - initial
 */
/**
 * Every cell formatter in one place, shared by every art. Two kinds, both named
 * {@code ...CellFormatter}:
 * <ul>
 * <li>formatting one value: {@link org.eclipse.daanse.olap.cellformatter.FormatStringCellFormatter} with
 * {@link org.eclipse.daanse.olap.cellformatter.LocaleCellFormatter} (format string under a locale) and
 * {@link org.eclipse.daanse.olap.cellformatter.UserDefinedCellFormatter} (a
 * {@link org.eclipse.daanse.olap.api.formatter.CellFormatter} of the catalog);</li>
 * <li>writing a whole cell set as text:
 * {@link org.eclipse.daanse.olap.cellformatter.RectangularTextCellFormatter} (a grid) and
 * {@link org.eclipse.daanse.olap.cellformatter.TraditionalTextCellFormatter} (axis by axis, then the cells).
 * They stay here although only tests write results as text today.</li>
 * </ul>
 */
@org.osgi.annotation.bundle.Export
@org.osgi.annotation.versioning.Version("0.0.1")
package org.eclipse.daanse.olap.cellformatter;
