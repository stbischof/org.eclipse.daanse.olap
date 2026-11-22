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
 * An execution has taken the empty positions off the axes it runs NON EMPTY; the end of {@link NonEmptyStartEvent}.
 *
 * @param axisCount the axes it took them off
 */
public record NonEmptyEndEvent(ExecutionEventCommon executionEventCommon, int axisCount) implements ExecutionEvent {

}
