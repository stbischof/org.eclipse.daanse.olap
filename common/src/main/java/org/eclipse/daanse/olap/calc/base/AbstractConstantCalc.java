/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.calc.base;

import java.util.List;

import org.eclipse.daanse.olap.api.calc.ConstantCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;

public abstract class AbstractConstantCalc<T> extends AbstractCalc<T> implements ConstantCalc<T> {

	private T value;

	public AbstractConstantCalc(T value, Type type) {
		super(type);
		this.value = value;
	}

	@Override
	public T evaluateInternal(Evaluator evaluator) {
		return value;
	}

	@Override
	public boolean dependsOn(Hierarchy hierarchy) {
		return false;
	}

	@Override
	public ResultStyle getResultStyle() {
		return value == null ? ResultStyle.VALUE : ResultStyle.VALUE_NOT_NULL;
	}

	@Override
	List<CalculationProfile> getChildProfiles() {
		return List.of();
	}

}
