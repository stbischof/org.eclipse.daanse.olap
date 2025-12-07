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

package org.eclipse.daanse.olap.calc.base.type.datetime;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDateTimeCalc;

public class UnknownToDateTimeCalc extends AbstractProfilingNestedDateTimeCalc {

	public UnknownToDateTimeCalc(Type type, Calc<?> calc) {
		super(type, calc);
	}

	@Override
	public LocalDateTime evaluateInternal(Evaluator evaluator) {
		Object o = getFirstChildCalc().evaluate(evaluator);
		if (o == null) {
			return null;
		} else if (o instanceof LocalDateTime ldt) {
			return ldt;
		} else if (o instanceof Date d) {
			// Support legacy Date conversion
			return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
		}

		throw evaluator.newEvalException(null, "expected LocalDateTime was " + o);
	}
}