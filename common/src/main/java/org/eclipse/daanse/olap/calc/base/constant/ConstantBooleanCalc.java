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
package org.eclipse.daanse.olap.calc.base.constant;

import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.calc.base.AbstractConstantCalc;

public class ConstantBooleanCalc extends AbstractConstantCalc<Boolean> implements BooleanCalc {

	public ConstantBooleanCalc( BooleanType type,Boolean value) {
		super(value, type);
	}

}
