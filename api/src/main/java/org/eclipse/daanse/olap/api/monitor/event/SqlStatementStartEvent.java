/*
* Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api.monitor.event;

/**
 * The Record SqlStatementStartEvent.
 *
 * @param sqlStatementEventCommon SQL Statement Event Common
 * @param cellRequestCount Number of missed cells that led to this request
 */
public record SqlStatementStartEvent(SqlStatementEventCommon sqlStatementEventCommon, long cellRequestCount)
        implements SqlStatementEvent {

}
