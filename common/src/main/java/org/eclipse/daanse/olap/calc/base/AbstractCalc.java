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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.profile.CalcProfileR;

public abstract class AbstractCalc<T> implements Calc<T> {

	private Type type;

	/**
	 * Abstract Implementation of {@link Calc} that generated a
	 * {@link CalculationProfile} while calling
	 * evaluateWithProfile(Evaluator)
	 *
	 * @param type type 
	 */
	public AbstractCalc(Type type) {
		this.type = type;
	}

	/**
	 * The body, called directly: the calc measures nothing. A profiled statement compiles each calc into a
	 * {@code ProfilingCalc} hull that counts and times it (ProfilingCompiler), so a statement without profiling pays
	 * nothing per evaluation.
	 */
	@Override
	public final T evaluate(Evaluator evaluator) {
		return evaluateInternal(evaluator);
	}

	protected abstract T evaluateInternal(Evaluator evaluator);

	protected Map<String, Object> profilingProperties(Map<String, Object> properties) {
		return properties;
	}

	public CalculationProfile getCalculationProfile() {
		final List<CalculationProfile> childProfiles = getChildProfiles();
		Map<String, Object> profilingProperties = profilingProperties(new HashMap<String, Object>());
		return new CalcProfileR(this.getClass(), Optional.empty(), getType(), getResultStyle(), 0, 0, 0, 0,
				profilingProperties, childProfiles);
	}

	List<CalculationProfile> getChildProfiles() {
		return List.of();
	}

	@Override
	public Type getType() {
		return type;
	}
	
	 protected void requiresType(Class<? extends Type> typeClass) {
		Type type = getType();
		if (!typeClass.isInstance(type)) {
			throw new RuntimeException("Expecting Type " + typeClass + " but was " + type);
		}

	}

}
