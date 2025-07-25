/*
 *
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2020 Hitachi Vantara and others
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

package org.eclipse.daanse.olap.fun.sort;

import java.util.Comparator;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.element.Member;

/**
 * Compares tuples, which are represented as lists of {@link Member}s.
 */
abstract class TupleComparator
  implements Comparator<List<Member>> {
  final int arity;

  TupleComparator( int arity ) {
    this.arity = arity;
  }

  /**
   * Extension to {@link TupleComparator} which compares tuples by evaluating an expression.
   */
  abstract static class TupleExpComparator extends TupleComparator {
    Evaluator evaluator;
    final Calc calc;

    TupleExpComparator( Evaluator evaluator, Calc calc, int arity ) {
      super( arity );
      this.evaluator = evaluator;
      this.calc = calc;
    }
  }
}
