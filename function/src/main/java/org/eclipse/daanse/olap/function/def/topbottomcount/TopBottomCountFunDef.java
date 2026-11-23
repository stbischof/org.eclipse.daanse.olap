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
package org.eclipse.daanse.olap.function.def.topbottomcount;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

public class TopBottomCountFunDef extends AbstractFunctionDefinition {
    boolean top;

    public TopBottomCountFunDef( FunctionMetaData functionMetaData , final boolean top ) {
      super( functionMetaData );
      this.top = top;

    }

    @Override
  public Calc<?> compileCall( final ResolvedFunCall call, ExpressionCompiler compiler ) {
      // Compile the member list expression. Ask for a mutable list, because
      // we're going to sort it later.
      final TupleListCalc tupleListCalc =
        compiler.compileMutableList(call.getArg( 0 ));
      final IntegerCalc integerCalc =
        compiler.compileInteger( call.getArg( 1 ) );
      final Calc<?> orderCalc =
        call.getArgCount() > 2
          ? compiler.compileExactScalar(call.getArg( 2 ))
          : null;
      return new TopBottomCountCalc(
              call.getType(), tupleListCalc, integerCalc, orderCalc, call, top) {

      };
    }
  }
