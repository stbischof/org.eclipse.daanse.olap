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
package org.eclipse.daanse.olap.function.def.sum;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;
import org.eclipse.daanse.olap.exceptions.ResultStyleException;
import org.eclipse.daanse.olap.function.core.AbstractAggregateFunDef;

public class SumFunDef extends AbstractAggregateFunDef {
        static final String TIMING_NAME = SumFunDef.class.getSimpleName();

        public SumFunDef( FunctionMetaData functionMetaData ) {
          super( functionMetaData );
        }

        @Override
      public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
          // What is the desired type to use to get the underlying values
          for ( ResultStyle r : compiler.getAcceptableResultStyles() ) {
            Calc<?> calc;
            switch ( r ) {
              case ITERABLE:
              case ANY:
                // Consumer wants ITERABLE or ANY to be used
                // return compileCallIterable(call, compiler);
                calc = compileCall( call, compiler, ResultStyle.ITERABLE );
                if ( calc != null ) {
                  return calc;
                }
                break;
              case MUTABLE_LIST:
                // Consumer wants MUTABLE_LIST
                calc = compileCall( call, compiler, ResultStyle.MUTABLE_LIST );
                if ( calc != null ) {
                  return calc;
                }
                break;
              case LIST:
                // Consumer wants LIST to be used
                // return compileCallList(call, compiler);
                calc = compileCall( call, compiler, ResultStyle.LIST );
                if ( calc != null ) {
                  return calc;
                }
                break;
            }
          }
          throw ResultStyleException.generate( ResultStyle.ITERABLE_LIST_MUTABLELIST_ANY, compiler
              .getAcceptableResultStyles() );
        }

        protected Calc<?> compileCall( final ResolvedFunCall call, ExpressionCompiler compiler, ResultStyle resultStyle ) {
          final Calc<?> ncalc = compiler.compileIterable(call.getArg( 0 ));
          if ( ncalc == null ) {
            return null;
          }
          final Calc<?> calc =
              call.getArgCount() > 1 ? compiler.compileExactScalar(call.getArg( 1 )) : new CurrentValueUnknownCalc( call.getType() );
          // we may have asked for one sort of Calc, but here's what we got.
          if ( ncalc instanceof TupleListCalc ) {
            return genListCalc( call, (TupleListCalc) ncalc, calc );
          } else {
            return genIterCalc( call, (TupleIteratorCalc) ncalc, calc );
          }
        }

        protected Calc<?> genIterCalc( final ResolvedFunCall call, final TupleIteratorCalc tupleIteratorCalc, final Calc calc ) {
          return new SumIterCalc( call.getType(), tupleIteratorCalc, calc);
        }

        protected Calc<?> genListCalc( final ResolvedFunCall call, final TupleListCalc tupleListCalc, final Calc calc ) {
          return new SumListCalc( call.getType(), tupleListCalc, calc );
        }
      }
