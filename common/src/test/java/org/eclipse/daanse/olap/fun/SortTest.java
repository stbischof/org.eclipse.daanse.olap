/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2020 Hitachi Vantara..  All rights reserved.
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
package org.eclipse.daanse.olap.fun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


/**
 * <code>SortTest</code> tests the collation order of positive and negative
 * infinity, and {@link Double#NaN}.
 *
 * @author jhyde
 * @since Sep 21, 2006
 */
class SortTest {

  /**
   * Access properties via this object and their values will be reset.
 */

  @AfterEach
  public void afterEach() {
    SystemWideProperties.instance().populateInitial();
  }

  @Test
  void testFoo() {
    // Check that each value compares according to its position in the total
    // order. For example, NaN compares greater than
    // Double.NEGATIVE_INFINITY, -34.5, -0.001, 0, 0.00000567, 1, 3.14;
    // equal to NaN; and less than Double.POSITIVE_INFINITY.
    //
    // Since (the null-semantics notes) the unboxed
    // order has no NULL slot; the former DOUBLE_NULL sentinel value
    // 0.000000012345 sorts as the plain tiny number it is.
    double[] values = {
      Double.NEGATIVE_INFINITY,
      -34.5,
      -0.001,
      0,
      0.000000012345, // former sentinel: now an ordinary value
      0.00000567,
      1,
      3.14,
      Double.NaN,
      Double.POSITIVE_INFINITY,
    };
    for ( int i = 0; i < values.length; i++ ) {
      for ( int j = 0; j < values.length; j++ ) {
        int expected = Integer.compare( i, j );
        assertEquals(
          expected,
          FunUtil.compareValues( values[ i ], values[ j ] ),
                "values[" + i + "]=" + values[ i ] + ", values[" + j
                        + "]=" + values[ j ]);
      }
    }
  }

}
