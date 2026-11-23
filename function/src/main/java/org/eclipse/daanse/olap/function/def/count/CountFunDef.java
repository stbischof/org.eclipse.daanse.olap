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
package org.eclipse.daanse.olap.function.def.count;

import org.eclipse.daanse.olap.api.calc.compiler.CompileRequest;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.AbstractAggregateFunDef;

public class CountFunDef  extends AbstractAggregateFunDef {

    static final String TIMING_NAME = CountFunDef.class.getSimpleName();

    public CountFunDef( FunctionMetaData functionMetaData ) {
      super( functionMetaData );
    }

    @Override
  public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
      final Calc<?> calc = compiler.compile(call.getArg( 0 ), CompileRequest.as(null, ResultStyle.ITERABLE_ANY));
      final boolean includeEmpty =
          call.getArgCount() < 2 || ( (Literal<?>) call.getArg( 1 ) ).getValue().equals( "INCLUDEEMPTY" );
      return new CountCalc( call.getType(), calc, includeEmpty );
    }
  }
