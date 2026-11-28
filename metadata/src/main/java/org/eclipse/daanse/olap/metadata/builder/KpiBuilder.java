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
package org.eclipse.daanse.olap.metadata.builder;

import org.eclipse.daanse.olap.metadata.model.KpiDef;

/** Builds a KPI. Every field is MDX text: the name of a member, or an expression. */
public final class KpiBuilder extends ElementBuilder<KpiBuilder> {

    private String value;
    private String goal;
    private String status;
    private String trend;
    private String weight;
    private String currentTimeMember;
    private String statusGraphic;
    private String trendGraphic;
    private String parentKpi;
    private org.eclipse.daanse.olap.metadata.model.LocalizedText displayFolder;

    KpiBuilder(String name) {
        super(name);
    }

    public KpiBuilder value(String mdx) {
        this.value = mdx;
        return this;
    }

    public KpiBuilder goal(String mdx) {
        this.goal = mdx;
        return this;
    }

    public KpiBuilder status(String mdx) {
        this.status = mdx;
        return this;
    }

    public KpiBuilder trend(String mdx) {
        this.trend = mdx;
        return this;
    }

    public KpiBuilder weight(String mdx) {
        this.weight = mdx;
        return this;
    }

    public KpiBuilder currentTimeMember(String mdx) {
        this.currentTimeMember = mdx;
        return this;
    }

    public KpiBuilder statusGraphic(String graphic) {
        this.statusGraphic = graphic;
        return this;
    }

    public KpiBuilder trendGraphic(String graphic) {
        this.trendGraphic = graphic;
        return this;
    }

    public KpiBuilder parent(String kpiName) {
        this.parentKpi = kpiName;
        return this;
    }

    /** The folder clients show the KPI in. */
    public KpiBuilder displayFolder(String folder) {
        this.displayFolder = folder == null ? null : org.eclipse.daanse.olap.metadata.model.LocalizedText.of(folder);
        return this;
    }

    KpiDef build() {
        return new KpiDef(name, value, goal, status, trend, weight, currentTimeMember, statusGraphic, trendGraphic,
                parentKpi, displayFolder, info());
    }
}
