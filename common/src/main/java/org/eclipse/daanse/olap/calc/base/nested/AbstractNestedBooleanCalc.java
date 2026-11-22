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
package org.eclipse.daanse.olap.calc.base.nested;

import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.AbstractNestedCalc;

/**
 * Abstract implementation of the
 * {@link org.eclipse.daanse.olap.api.calc.BooleanCalc} interface.
 * 
 * Handles nested child and profiling
 *
 */
public abstract class AbstractNestedBooleanCalc extends AbstractNestedCalc<Boolean>
		implements BooleanCalc {
	/**
	 * {@inheritDoc}
	 *
	 */
	protected AbstractNestedBooleanCalc(Type type, Calc<?>... calcs) {
		super(type, calcs);
	}

}
