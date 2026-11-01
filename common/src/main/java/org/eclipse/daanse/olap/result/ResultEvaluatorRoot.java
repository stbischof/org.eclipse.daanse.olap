/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
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
package org.eclipse.daanse.olap.result;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ParameterSlot;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.evaluator.EvaluatorImpl;
import org.eclipse.daanse.olap.evaluator.EvaluatorRoot;
import org.eclipse.daanse.olap.evaluator.NamedSetEvaluatorImpl;
import org.eclipse.daanse.olap.evaluator.SetEvaluatorImpl;

/**
 * Extension to {@link EvaluatorRoot} which is capable of evaluating sets and named sets.
 *
 * 
 * A given set is only evaluated once each time a query is executed; the result is added to the
 * {@link #namedSetEvaluators} cache on first execution and re-used.
 *
 *
 * 
 * Named sets are always evaluated in the context of the slicer.
 *
 */
public class ResultEvaluatorRoot extends EvaluatorRoot {
  /**
   * Maps the names of sets to their values. Populated on demand.
 */
  private final Map<String, SetEvaluatorImpl> setEvaluators = new HashMap<>();
  private final Map<String, NamedSetEvaluatorImpl> namedSetEvaluators =
      new HashMap<>();

  public final ResultImpl result;
  private static final Object CycleSentinel = new Object();
  private static final Object NullSentinel = new Object();
    private final static String cycleDuringParameterEvaluation = "Cycle occurred while evaluating parameter ''{0}''";

    public ResultEvaluatorRoot( ResultImpl result ) {
    super( result.execution );
    this.result = result;
  }

  @Override
  public EvaluatorImpl slicerEvaluator() {
    return result.slicerEvaluator;
  }

  @Override
  public Object evaluateExpression( Calc<?> calc, EvaluatorImpl slicerEvaluator, Evaluator contextEvaluator ) {
    return result.evaluateExp( calc, slicerEvaluator, contextEvaluator );
  }

  @Override
	protected Evaluator.NamedSetEvaluator evaluateNamedSet( final NamedSet namedSet, boolean create ) {
    final String name = namedSet.getNameUniqueWithinQuery();
    if ( namedSet.isDynamic() && !create ) {
        NamedSetEvaluatorImpl value = new NamedSetEvaluatorImpl( this, namedSet );
        namedSetEvaluators.put( name, value );
        return value;

    } else {
      return namedSetEvaluators.computeIfAbsent(name, k -> new NamedSetEvaluatorImpl( this, namedSet ));
    }
  }

  @Override
	protected Evaluator.SetEvaluator evaluateSet( final Expression exp, boolean create ) {
    // Sanity check: This expression HAS to return a set.
    if ( !( exp.getType() instanceof SetType ) ) {
      throw Util.newInternal( "Trying to evaluate set but expression does not return a set" );
    }

    // Should be acceptable to use the string representation of the
    // expression as the name
    final String name = exp.toString();

    // pedro, 20120914 - I don't quite understand the !create, I was
    // kind'a expecting the opposite here. But I'll maintain the same
    // logic
    if ( !create ) {
        SetEvaluatorImpl value = new SetEvaluatorImpl( this, exp );
        setEvaluators.put( name, value );
        return value;
    } else {
        return setEvaluators.computeIfAbsent(name, k -> new SetEvaluatorImpl( this, exp ));
    }
  }

  @Override
	public Object getParameterValue( ParameterSlot slot ) {
    if ( slot.isParameterSet() ) {
      return slot.getParameterValue();
    }

    // Look in other places for the value. Which places we look depends
    // on the scope of the parameter.
    Parameter.Scope scope = slot.getParameter().getScope();
    switch ( scope ) {
      case System:
        // TODO: implement system params

        // fall through
      case Schema:
        // TODO: implement schema params

        // fall through
      case Connection:
        // if it's set in the session, return that value

        // fall through
      case Statement:
        break;

      default:
        throw Util.badValue( scope );
    }

    // Not set in any accessible scope. Evaluate the default value,
    // then cache it.
    Object liftedValue = slot.getCachedDefaultValue();
    Object value;
    if ( liftedValue != null ) {
      if ( liftedValue == CycleSentinel ) {
        throw new OlapRuntimeException(MessageFormat.format(cycleDuringParameterEvaluation, slot.getParameter().getName() ));
      }
      if ( liftedValue == NullSentinel ) {
        value = null;
      } else {
        value = liftedValue;
      }
      return value;
    }
    // Set value to a sentinel, so we can detect cyclic evaluation.
    slot.setCachedDefaultValue( CycleSentinel );
    value = result.evaluateExp( slot.getDefaultValueCalc(), result.slicerEvaluator, null );
    if ( value == null ) {
      liftedValue = NullSentinel;
    } else {
      liftedValue = value;
    }
    slot.setCachedDefaultValue( liftedValue );
    return value;
  }
}


