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

/**
 * A cube a virtual cube draws from.
 *
 * @param ignoreUnrelatedDimensions whether a measure of this cube ignores a dimension of the
 *                                  virtual cube it does not vary by, instead of being empty there
 */
public record CubeUsageDef(PhysicalCubeDef cube, boolean ignoreUnrelatedDimensions) {
}
