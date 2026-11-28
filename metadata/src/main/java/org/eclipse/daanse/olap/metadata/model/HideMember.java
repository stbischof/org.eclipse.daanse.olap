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
 * When a member of a level is none of a client's business, which is how a hierarchy with gaps is told
 * apart from one without: a country without states keeps its towns directly below it, and the state
 * that stands between them in the description is not shown.
 */
public enum HideMember {
    /** Every member of the level is shown. */
    NEVER,
    /** A member whose name is empty stands for a step the data does not take. */
    IF_BLANK_NAME,
    /** A member that repeats the name of its parent stands for a step the data does not take. */
    IF_PARENTS_NAME
}
