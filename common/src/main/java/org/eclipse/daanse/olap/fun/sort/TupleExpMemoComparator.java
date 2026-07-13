/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2020 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - switch Cache to caffeine
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */


package org.eclipse.daanse.olap.fun.sort;


import java.util.Arrays;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.util.CancellationChecker;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Supports comparison of tuples, caching results such that .compare calls involving the same tuple do not result in
 * redundant evaluation.
 *
 * Optimized to evaluate only the "dependent" hierarchies in a tuple.  This can reduce the total eval required
 * considerably in many cases.  For example, sorting a 3-way crossjoin on a single hierarchy's CURRENTMEMBER should not
 * require evaluation of the complete cross product.
 */

abstract class TupleExpMemoComparator extends TupleComparator.TupleExpComparator {
  // Lazily created on first eval() so subclasses that never memoize
  // (HierarchicalTupleComparator evaluates directly) don't allocate an unused
  // cache. A comparator is used by a single sort on one thread, so no
  // synchronization is needed.
  private Cache<List<Member>, Object> valueCache;

  private int[] dependentHierarchiesIndices;
  private int count = 0;

  TupleExpMemoComparator( Evaluator e, Calc calc, int arity ) {
    super( e, calc, arity );
  }

  // Marker under which an MDX NULL result is memorized: Caffeine cannot
  // store null values, and re-evaluating NULL-valued tuples on every
  // comparison would defeat the memoization. The marker never leaves this
  // class; eval() unwraps it back to Java null.
  private static final Object NULL_RESULT = new Object();

  // applies the Calc to a tuple, memorizing results
  protected Object eval( List<Member> key ) {
    if ( valueCache == null ) {
      valueCache = Caffeine.newBuilder().maximumSize( 100000 ).build();
    }
    // Loader exceptions (e.g. CellRequestQuantumExceededException, which drives
    // the batch-reload protocol) propagate unwrapped: Caffeine does not wrap
    // them, unlike the former Guava cache.
    Object val = valueCache.get( key, this::evaluateCalc );
    return val == NULL_RESULT ? null : val;
  }

  private List<Member> dependentMembers( List<Member> tuple ) {
    getDependentHierarchiesIndices( tuple );
    return Arrays.stream( dependentHierarchiesIndices )
      .mapToObj( tuple::get )
      .toList();
  }

  private void getDependentHierarchiesIndices( List<Member> tuple ) {
    if ( dependentHierarchiesIndices == null ) {
      dependentHierarchiesIndices = new int[ tuple.size() ];
      int curPos = 0;
      for ( int i = 0; i < tuple.size(); i++ ) {
        if ( calc.dependsOn( tuple.get( i ).getHierarchy() ) ) {
          dependentHierarchiesIndices[ curPos++ ] = i;
        }
      }
      dependentHierarchiesIndices = Arrays.copyOf( dependentHierarchiesIndices, curPos );
    }
  }

  @Override
public int compare( List<Member> a1, List<Member> a2 ) {
    CancellationChecker.checkCancelOrTimeout( count++,
      evaluator.getQuery().getStatement().getCurrentExecution() );
    List<Member> a1Members = dependentMembers( a1 );
    List<Member> a2Members = dependentMembers( a2 );
    // before doing any eval, first check whether the lists are equal.
    // nonEqualCompare can be much more expensive.
    return Sorter.listEquals( a1Members, a2Members ) ? 0
      : nonEqualCompare( a1Members, a2Members );
  }

  /**
   * If a strict check of list equality fails, nonEqualCompare will be checked.
   */
  protected abstract int nonEqualCompare( List<Member> eval, List<Member> eval1 );

  private Object evaluateCalc( List<Member> tuple ) {
    evaluator.setContext( tuple );
    Object val = calc.evaluate( evaluator );
    return val == null ? NULL_RESULT : val;
  }

  static class BreakTupleComparator extends TupleExpMemoComparator {
    BreakTupleComparator( Evaluator e, Calc calc, int arity ) {
      super( e, calc, arity );
    }

    @Override protected int nonEqualCompare( List<Member> a1, List<Member> a2 ) {
      return Sorter.compareValues( eval( a1 ), eval( a2 ) );
    }
  }
}
