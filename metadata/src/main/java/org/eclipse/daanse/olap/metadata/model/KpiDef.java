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
 * A key performance indicator. Every field is MDX text: either the name of a member or an
 * expression, which an engine publishes as a hidden calculated measure, so that a query can
 * move it through time: {@code (KPIValue("Revenue"), [Time].PrevMember)}.
 */
public record KpiDef(String name, String value, String goal, String status, String trend, String weight,
        String currentTimeMember, String statusGraphic, String trendGraphic, String parentKpi,
        LocalizedText displayFolder, ElementInfo info) {
}
