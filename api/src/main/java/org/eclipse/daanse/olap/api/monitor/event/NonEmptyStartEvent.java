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
package org.eclipse.daanse.olap.api.monitor.event;

/**
 * An execution begins to take the empty positions off the axes it runs NON EMPTY: once per execution, over all of
 * them, after the cells are read and before the result is handed back. Analysis Services reports the step as
 * Calculate Non Empty Begin, once for a query, however many of its axes are NON EMPTY.
 */
public record NonEmptyStartEvent(ExecutionEventCommon executionEventCommon) implements ExecutionEvent {

}
