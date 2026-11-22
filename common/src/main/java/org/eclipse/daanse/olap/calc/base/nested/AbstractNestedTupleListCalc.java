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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.AbstractNestedCalc;

public abstract class AbstractNestedTupleListCalc extends AbstractNestedCalc<TupleList>
		implements TupleListCalc {
	private final boolean mutable;

	protected AbstractNestedTupleListCalc(Type type, Calc<?>... calcs) {
		this(type, true, calcs);
	}

	protected AbstractNestedTupleListCalc(Type type, boolean mutable, Calc<?>... calcs) {
		super(type, calcs);
		this.mutable = mutable;
		requiresType(SetType.class);
	}

	@Override
	public SetType getType() {
		return (SetType) super.getType();
	}

	@Override
	public ResultStyle getResultStyle() {
		return mutable ? ResultStyle.MUTABLE_LIST : ResultStyle.LIST;
	}

}
