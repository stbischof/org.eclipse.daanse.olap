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
package org.eclipse.daanse.olap.function.def.aggregate.avg;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionInterface;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.AbstractAggregateFunDef;

class AvgNumericFunDef extends AbstractAggregateFunDef {
	static final FunctionMetaData fmd = new FunctionMetaDataR(AvgResolver.operationAtom,
			"Returns the average value of a numeric expression evaluated over a set.", DataType.NUMERIC,
			new FunctionParameterR[] { FunctionParameterR.param(DataType.SET).describedAs("Set"),
					FunctionParameterR.param(DataType.NUMERIC).describedAs("Numeric Expression") }).interfaceName(FunctionInterface.STATISTICAL)
			.withTextKey("Set.Avg.Function").caption("Set.Avg Function");

	public AvgNumericFunDef() {
		super(fmd);
	}

	@Override
	public Calc<Double> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
		final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
		final Calc<?> calc = compiler.compileExactScalar(call.getArg(1));
		return new AvgCalc(call.getType(), tupleListCalc, calc, AvgNumericFunDef.class.getSimpleName());
	}
}
