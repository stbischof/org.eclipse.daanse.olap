/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2021 Hitachi Vantara and others
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.calc.tuple.TupleCursor;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

  /**
   * A NonEmptyResult filters a result by removing empty rows
   * on a particular axis.
   */
  public class NonEmptyResult extends ResultBase {

  private static final Logger LOGGER = LoggerFactory.getLogger( NonEmptyResult.class );

    public final Result underlying;
    private final int axis;
    private final Map<Integer, Integer> map;
    /**
     * workspace. Synchronized access only.
     */
    private final int[] pos;

    /**
     * Creates a NonEmptyResult.
     *
     * @param result    Result set
     * @param execution Execution context
     * @param axis      Which axis to make non-empty
     */
    public NonEmptyResult( Result result, Execution execution, int axis ) {
      super( execution, result.getAxes().clone() );

      this.underlying = result;
      this.axis = axis;
      this.map = new HashMap<>();
      int axisCount = underlying.getAxes().length;
      this.pos = new int[ axisCount ];
      this.slicerAxis = underlying.getSlicerAxis();
      TupleList tupleList =
        ( (AxisImpl) underlying.getAxes()[ axis ] ).getTupleList();

      final TupleList filteredTupleList;


		filteredTupleList = TupleCollections.createList(tupleList.getArity());
		int i = -1;
		TupleCursor tupleCursor = tupleList.tupleCursor();
		while (tupleCursor.forward()) {
			++i;
			if (!isEmpty(i, axis)) {
				map.put(filteredTupleList.size(), i);
				filteredTupleList.addCurrent(tupleCursor);
			}
		}

      this.axes[ axis ] = new AxisImpl( filteredTupleList );
    }

    @Override
	protected Logger getLogger() {
      return LOGGER;
    }

    /**
     * Returns true if all cells at a given offset on a given axis are
     * empty. For example, in a 2x2x2 dataset, isEmpty(1,0)
     * returns true if cells {(1,0,0), (1,0,1), (1,1,0),
     * (1,1,1)} are all empty. As you can see, we hold the 0th
     * coordinate fixed at 1, and vary all other coordinates over all
     * possible values.
     */
    private boolean isEmpty( int offset, int fixedAxis ) {
      int axisCount = getAxes().length;
      pos[ fixedAxis ] = offset;
      return isEmptyRecurse( fixedAxis, axisCount - 1 );
    }

    private boolean isEmptyRecurse( int fixedAxis, int axis ) {
      if ( axis < 0 ) {
        Cell cell = underlying.getCell( pos );
        return cell.isNull();
      } else if ( axis == fixedAxis ) {
        return isEmptyRecurse( fixedAxis, axis - 1 );
      } else {
        List<Position> positions = getAxes()[ axis ].getPositions();
        final int positionCount = positions.size();
        for ( int i = 0; i < positionCount; i++ ) {
          pos[ axis ] = i;
          if ( !isEmptyRecurse( fixedAxis, axis - 1 ) ) {
            return false;
          }
        }
        return true;
      }
    }

    // synchronized because we use 'pos'
    @Override
	public synchronized Cell getCell( int[] externalPos ) {
      try {
        System.arraycopy(
          externalPos, 0, this.pos, 0, externalPos.length );
        int offset = externalPos[ axis ];
        int mappedOffset = mapOffsetToUnderlying( offset );
        this.pos[ axis ] = mappedOffset;
        return underlying.getCell( this.pos );
      } catch ( NullPointerException npe ) {
        return underlying.getCell( externalPos );
      }
    }

    private int mapOffsetToUnderlying( int offset ) {
      return map.get( offset );
    }

    @Override
	public void close() {
      underlying.close();
    }

      @Override
      public Member[] getCellMembers(int[] coordinates) {
          return underlying.getCellMembers(coordinates);
      }
  }
