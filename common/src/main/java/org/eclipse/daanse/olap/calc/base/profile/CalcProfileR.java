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
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.olap.calc.base.profile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;
import org.eclipse.daanse.olap.api.type.Type;

public record CalcProfileR(Class<?> clazz, Optional<String> name, Type type, ResultStyle resultStyle,
		long nanos, long elements, long elementsSquared,
		Map<String, Object> additionalValues, List<CalculationProfile> childProfiles) implements CalculationProfile {
}
