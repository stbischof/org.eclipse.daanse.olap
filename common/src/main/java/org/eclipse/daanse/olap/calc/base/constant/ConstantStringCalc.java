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

import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.NullType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.calc.base.AbstractConstantCalc;

public class ConstantStringCalc extends AbstractConstantCalc<String> implements StringCalc {

	public ConstantStringCalc(StringType type, String value) {
		super(value, type);
	}

    public ConstantStringCalc(NullType type, String value) {
        super(value, type);
    }

}
