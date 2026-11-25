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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.element;

import org.eclipse.daanse.olap.api.element.KPI;

public class KPIImpl implements KPI {

	private String name;

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String description;

	private String displayFolder;


	private String value;

	private String goal;

	private String status;

	private String trend;

	private String weight;

	private String trendGraphic;

	private String statusGraphic;

	private String currentTimeMember;

	private KPI parentKpi;

	@Override
	public String getDisplayFolder() {
		return displayFolder;
	}

	public void setDisplayFolder(String displayFolder) {
		this.displayFolder = displayFolder;
	}

	@Override

	public String getValue() {
		return value;
	}


	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String getGoal() {
		return goal;
	}

	public void setGoal(String goal) {
		this.goal = goal;
	}
	@Override

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	@Override

	public String getTrend() {
		return trend;
	}

	public void setTrend(String trend) {
		this.trend = trend;
	}
	@Override
	public String getWeight() {
		return weight;
	}

	public void setWeight(String weight) {
		this.weight = weight;
	}

	@Override

	public String getTrendGraphic() {
		return trendGraphic;
	}

	public void setTrendGraphic(String trendGraphic) {
		this.trendGraphic = trendGraphic;
	}

	@Override
	public String getStatusGraphic() {
		return statusGraphic;
	}

	public void setStatusGraphic(String statusGraphic) {
		this.statusGraphic = statusGraphic;
	}

	@Override
	public String getCurrentTimeMember() {
		return currentTimeMember;
	}

	
	public void setCurrentTimeMember(String currentTimeMember) {
		this.currentTimeMember = currentTimeMember;
	}
	
	@Override
	public KPI getParentKpi() {
		return parentKpi;
	}

	public void setParentKpi(KPI parentKpi) {
		this.parentKpi = parentKpi;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private String caption;

	/** {@inheritDoc} */
	@Override
	public String getCaption() {
		return caption == null ? getName() : caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}
}
