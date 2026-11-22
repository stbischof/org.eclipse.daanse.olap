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
package org.eclipse.daanse.olap.function.def.crossjoin;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.evaluator.NativeEvaluator;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.common.Util;

public abstract class BaseListCalc extends AbstractNestedTupleListCalc {
    ResolvedFunCall call;
    private int ctag;

  protected BaseListCalc( ResolvedFunCall call, Calc[] calcs, boolean mutable, int ctag ) {
    super( call.getType(), calcs, mutable );
    this.call=call;
    this.ctag = ctag;
  }

  @Override
  public TupleList evaluateInternal(Evaluator evaluator ) {
    // Use a native evaluator, if more efficient.
    // TODO: Figure this out at compile time.
    CatalogReader schemaReader = evaluator.getCatalogReader();
    NativeEvaluator nativeEvaluator =
        schemaReader.getNativeSetEvaluator( call.getFunDef(), call.getArgs(), evaluator, this );
    if ( nativeEvaluator != null ) {
      return (TupleList) nativeEvaluator.execute( ResultStyle.LIST );
    }

    Calc[] calcs = getChildCalcs();
    TupleListCalc listCalc1 = (TupleListCalc) calcs[0];
    TupleListCalc listCalc2 = (TupleListCalc) calcs[1];

    TupleList l1 = listCalc1.evaluate( evaluator );
    // check if size of first list already exceeds limit
    Util.checkCJResultLimit( l1.size() );
    TupleList l2 = listCalc2.evaluate( evaluator );
    // check if size of second list already exceeds limit
    Util.checkCJResultLimit( l2.size() );
    // check crossjoin
    Util.checkCJResultLimit( (long) l1.size() * l2.size() );

    l1 = CrossJoinFunDef.nonEmptyOptimizeList( evaluator, l1, call, ctag );
    if ( l1.isEmpty() ) {
      return TupleCollections.emptyList( l1.getArity() + l2.getArity() );
    }
    l2 = CrossJoinFunDef.nonEmptyOptimizeList( evaluator, l2, call, ctag );
    if ( l2.isEmpty() ) {
      return TupleCollections.emptyList( l1.getArity() + l2.getArity() );
    }

    return makeList( l1, l2 );
  }

  public abstract TupleList makeList( TupleList l1, TupleList l2 );
}
