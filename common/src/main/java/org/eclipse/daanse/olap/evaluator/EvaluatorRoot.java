/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2021 Hitachi Vantara..  All rights reserved.
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
package org.eclipse.daanse.olap.evaluator;

import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.CubeLevel;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Measure;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.StoredMeasure;
import org.eclipse.daanse.olap.api.element.VirtualCube;
import org.eclipse.daanse.olap.api.result.CellReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ParameterSlot;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.common.SolveOrderMode;
import org.eclipse.daanse.olap.common.Util;

/**
 * Context at the root of a tree of evaluators.
 *
 *
 * Contains the context that does not change as evaluation context is pushed/popped.
 *
 * @author jhyde
 * @since Nov 11, 2008
 */
public abstract class EvaluatorRoot {
  final Map<Object, Object> expResultCache = new HashMap<>();
  final Map<Object, Object> tmpExpResultCache = new HashMap<>();
  protected final Cube cube;
  protected final Connection connection;
  protected final CatalogReader schemaReader;
  final Map<CompiledExpKey, Calc> compiledExps = new HashMap<>();
  public final Statement statement;
  protected final Query query;
  private final LocalDateTime queryStartTime;

  public int expResultCacheHitCount;
  public int expResultCacheMissCount;

  /**
   * Default members of each hierarchy, from the schema reader's perspective. Finding the default member is moderately
   * expensive, but happens very often.
   */
  public final CalculableMember[] defaultMembers;
  final int[] nonAllPositions;
  int nonAllPositionCount;

  SolveOrderMode solveOrderMode;

  final Set<Expression> activeNativeExpansions = new HashSet<>();

  /**
   * The size of the command stack at which we will next check for recursion.
   */
  int recursionCheckCommandCount;
  public final Execution execution;

  /**
   * Creates a EvaluatorRoot.
   *
   * @param statement
   *          statement
   * @deprecated
   */
  @Deprecated
public EvaluatorRoot( Statement statement ) {
    this( statement, null );
  }

  public EvaluatorRoot( Execution execution ) {
    this( execution.getDaanseStatement(), execution );
  }

  private EvaluatorRoot( Statement statement, Execution execution ) {
    this.execution = execution;
    this.statement = statement;
    this.query = statement.getQuery();
    this.cube = (Cube) query.getCube();
    this.connection = statement.getDaanseConnection();
    this.solveOrderMode =
        Util.lookup( SolveOrderMode.class, connection.getContext()
                .getConfig().solveOrderMode()
                .toUpperCase(),
            SolveOrderMode.ABSOLUTE );
    this.schemaReader = query.getCatalogReader( true );
    this.queryStartTime = LocalDateTime.now();
    List<CalculableMember> list = new ArrayList<>();
    nonAllPositions = new int[cube.getHierarchies().size()];
    nonAllPositionCount = 0;
    for ( Hierarchy hierarchy : cube.getHierarchies() ) {
      CalculableMember defaultMember = (CalculableMember) schemaReader.getHierarchyDefaultMember( hierarchy );
      assert defaultMember != null;

      // A writeback scenario replaces the default member of its own hierarchy. Which
      // hierarchy that is, and what member stands for the scenario, only the provider
      // knows, so it is asked rather than detected here.
      CalculableMember scenarioMember = scenarioMemberFor( hierarchy );
      if ( scenarioMember != null ) {
        defaultMember = scenarioMember;
      }

      // The relational provider rewrites the unique name of a default member here, to
      // reflect how the hierarchy is joined into the cube. That is a naming decision of
      // the provider, so it is made by the provider.
      nameDefaultMember( cube, hierarchy, defaultMember );

      list.add( defaultMember );
      if ( !defaultMember.isAll() ) {
        nonAllPositions[nonAllPositionCount] = hierarchy.getOrdinalInCube();
        nonAllPositionCount++;
      }
    }
    this.defaultMembers = list.toArray( new CalculableMember[list.size()] );

    this.recursionCheckCommandCount = ( defaultMembers.length << 4 );
  }

  /**
   * Implements a cheap-and-cheerful mapping from expressions to compiled expressions.
   *
   *
   * TODO: Save compiled expressions somewhere better.
   *
   * @param exp
   *          Expression
   * @param scalar
   *          Whether expression is scalar
   * @param resultStyle
   *          Preferred result style; if null, use query's default result style; ignored if expression is scalar
   * @return compiled expression
   */
  public final Calc getCompiled( Expression exp, boolean scalar, ResultStyle resultStyle ) {
    CompiledExpKey key = new CompiledExpKey( exp, scalar, resultStyle );
    Calc calc = compiledExps.get( key );
    if ( calc == null ) {
      calc = statement.getQuery().compileExpression( exp, scalar, resultStyle );
      compiledExps.put( key, calc );
    }
    return calc;
  }

  /**
   * Just a simple key of Exp/scalar/resultStyle, used for keeping compiled expressions. Previous to the introduction of
   * this class, the key was a list constructed as Arrays.asList(exp, scalar, resultStyle) and having poorer performance
   * on equals, hashCode, and construction.
   */
  private static class CompiledExpKey {
    private final Expression exp;
    private final boolean scalar;
    private final ResultStyle resultStyle;
    private int hashCode = Integer.MIN_VALUE;

    private CompiledExpKey( Expression exp, boolean scalar, ResultStyle resultStyle ) {
      this.exp = exp;
      this.scalar = scalar;
      this.resultStyle = resultStyle;
    }

    @Override
	public boolean equals( Object other ) {
      if ( this == other ) {
        return true;
      }
      if ( !( other instanceof CompiledExpKey otherKey ) ) {
        return false;
      }
      return this.scalar == otherKey.scalar && this.resultStyle == otherKey.resultStyle && this.exp.equals(
          otherKey.exp );
    }

    @Override
	public int hashCode() {
      if ( hashCode != Integer.MIN_VALUE ) {
        return hashCode;
      } else {
        int hash = 0;
        hash = Util.hash( hash, scalar );
        hash = Util.hash( hash, resultStyle );
        this.hashCode = Util.hash( hash, exp );
      }
      return this.hashCode;
    }
  }

  /**
   * Evaluates a named set.
   *
   *
   * The default implementation throws {@link UnsupportedOperationException}.
   *
   * @param namedSet
   *          Named set
   * @param create
   *          Whether to create named set evaluator if not found
   */
  protected Evaluator.NamedSetEvaluator evaluateNamedSet( NamedSet namedSet, boolean create ) {
    throw new UnsupportedOperationException();
  }

  /**
   * Evaluates a named set represented by an expression.
   *
   *
   * The default implementation throws {@link UnsupportedOperationException}.
   *
   * @param exp
   *          Expression
   * @param create
   *          Whether to create named set evaluator if not found
   */
  protected Evaluator.SetEvaluator evaluateSet( Expression exp, boolean create ) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the value of a parameter, evaluating its default expression if necessary.
   *
   *
   * The default implementation throws {@link UnsupportedOperationException}.
   */
  public Object getParameterValue( ParameterSlot slot ) {
    throw new UnsupportedOperationException();
  }

  /**
   * Puts result in cache.
   *
   * @param key
   *          key
   * @param result
   *          value to be cached
   * @param isValidResult
   *          indicate if this result is valid
   */
  public final void putCacheResult( Object key, Object result, boolean isValidResult ) {
    if ( isValidResult ) {
      expResultCache.put( key, result );
    } else {
      tmpExpResultCache.put( key, result );
    }
  }

  /**
   * Gets result from cache.
   *
   * @param key
   *          cache key
   * @return cached expression
   */
  public final Object getCacheResult( Object key ) {
    Object result = expResultCache.get( key );
    if ( result == null ) {
      result = tmpExpResultCache.get( key );
    }
    if ( result == null ) {
      expResultCacheMissCount++;
    } else {
      expResultCacheHitCount++;
    }
    return result;
  }

  /**
   * Clears the expression result cache.
   *
   * @param clearValidResult
   *          whether to clear valid expression results
   */
  public final void clearResultCache( boolean clearValidResult ) {
    if ( clearValidResult ) {
      expResultCache.clear();
    }
    tmpExpResultCache.clear();
  }

  /**
   * Get query start time.
   *
   * @return the query start time
   */
  public LocalDateTime getQueryStartTime() {
    return queryStartTime;
  }

  // ---- what only a provider can answer ------------------------------------

  /**
   * The member that stands for the active writeback scenario on {@code hierarchy}, or
   * {@code null} when this hierarchy carries no scenario. A provider without writeback
   * always answers {@code null}.
   */
  protected CalculableMember scenarioMemberFor( Hierarchy hierarchy ) {
    return null;
  }

  /**
   * Lets the provider settle the unique name of a hierarchy's default member, which may
   * depend on how that hierarchy is joined into the cube. Does nothing by default.
   */
  protected void nameDefaultMember( Cube cube, Hierarchy hierarchy, CalculableMember defaultMember ) {
    // nothing to settle unless a provider says otherwise
  }


  // ---- the seam to the result being built ---------------------------------

  /**
   * The evaluator the slicer axis established, against which set expressions outside the
   * current cell are evaluated.
   */
  public abstract EvaluatorImpl slicerEvaluator();

  /**
   * Evaluates a compiled expression, in the slicer context and optionally under a further
   * context evaluator. The result under construction owns this, because it is what decides
   * whether a value is final or has to be computed again after a load.
   */
  public abstract Object evaluateExpression( org.eclipse.daanse.olap.api.calc.Calc<?> calc,
      EvaluatorImpl slicerEvaluator, org.eclipse.daanse.olap.api.evaluator.Evaluator contextEvaluator );

}
