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
 * Whether the data member of a parent in a parent-child hierarchy -- the facts written to the parent itself -- shows
 * among the parent's children (SSAS {@code MembersWithData}; Mondrian's {@code parentAsLeaf}).
 */
public enum MembersWithData {
    /** Reached through {@code .DataMember} only; the default of SSAS and Mondrian. */
    NON_LEAF_DATA_HIDDEN,
    /** Also the first child of its parent, named by the data member caption. */
    NON_LEAF_DATA_VISIBLE
}
