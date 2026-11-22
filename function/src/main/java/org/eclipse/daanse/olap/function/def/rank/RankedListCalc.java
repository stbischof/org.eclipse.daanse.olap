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
package org.eclipse.daanse.olap.function.def.rank;

import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.calc.base.AbstractNestedCalc;

public class RankedListCalc extends AbstractNestedCalc {
    private final TupleListCalc tupleListCalc;
    private final boolean tuple;

    /**
     * Creates a RankedListCalc.
     *
     * @param tupleListCalc
     *          Compiled expression to compute the list
     * @param tuple
     *          Whether elements of the list are tuples (as opposed to members)
     */
    public RankedListCalc( TupleListCalc tupleListCalc, boolean tuple ) {
      super(  tupleListCalc.getType() , tupleListCalc );
      this.tupleListCalc = tupleListCalc;
      this.tuple = tuple;
    }

    @Override
    public Object evaluateInternal( Evaluator evaluator ) {
      // Construct an array containing the value of the expression
      // for each member.
      TupleList tupleList = tupleListCalc.evaluate( evaluator );
      assert tupleList != null;
      if ( tuple ) {
        return new RankedTupleList( tupleList );
      } else {
        return new RankedMemberList( tupleList.slice( 0 ) );
      }
    }
  }
