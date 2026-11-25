/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api.element;

public interface KPI {
    String getName();

    String getDisplayFolder();

    String getCurrentTimeMember();

    String getTrend();

    String getWeight();

    String getTrendGraphic();

    String getStatusGraphic();

    KPI getParentKpi();

    String getValue();

    String getGoal();

    String getStatus();

    String getDescription();

    /**
     * The text a client shows for this indicator, which is its name unless the catalog says otherwise.
     */
    default String getCaption() {
        return getName();
    }
}
