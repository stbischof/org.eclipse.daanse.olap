/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2021 Hitachi Vantara and others
 * Copyright (C) 2021 Sergei Semenkov
 * All Rights Reserved.
 *
 * jhyde, 10 August, 2001
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
import org.eclipse.daanse.olap.api.result.CellReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import org.eclipse.daanse.olap.calc.base.NullSemantics;
import org.eclipse.daanse.olap.api.cache.ExpCacheDescriptor;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ParameterSlot;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.execution.QueryTiming;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.result.CellValue;
import org.eclipse.daanse.olap.api.result.ErrorValue;
import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.result.NullValue;
import org.eclipse.daanse.olap.api.result.ObjectValue;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.DelegatingTupleList;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.util.format.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * EvaluatorImpl evaluates expressions in a dimensional environment.
 *
 * 
 * The context contains a member (which may be the default member) for every dimension in the current cube. Certain
 * operations, such as evaluating a calculated member or a tuple, change the current context.
 *
 * 
 * There are two ways of preserving context.
 *
 * 
 * First, the {@link #push} method creates a verbatim copy of the evaluator. Use that copy for computations, and any
 * changes of state will be made only to the copy.
 *
 * 
 * Second, the {@link #savepoint()} method tells the evaluator to create a checkpoint of its state, and returns an
 * {@code int} value that can later be passed to {@link #restore(int)}.
 *
 * 
 * The {@code savepoint} method is recommended for most purposes, because the initial checkpoint is extremely cheap.
 * Each call that modifies state (such as {@link org.eclipse.daanse.olap.api.evaluator.Evaluator#setContext(org.eclipse.daanse.olap.api.element.Member)}) creates, at
 * a modest cost, an entry on an internal command stack.
 *
 * 
 * One occasion that you would use {@code push} is when creating an iterator, and the iterator needs its own evaluator
 * context, even if the code that created the iterator later reverts the context. In this case, the iterator's
 * constructor should call {@code push}.
 *
 * Developers note
 *
 * 
 * Many of the methods in this class are performance-critical. Where possible they are declared 'final' so that the JVM
 * can optimize calls to these methods. If future functionality requires it, the 'final' modifier can be removed and
 * these methods can be overridden.
 *
 * @author jhyde
 * @since 10 August, 2001
 */
public class EvaluatorImpl implements Evaluator {
  private static final Logger LOGGER = LoggerFactory.getLogger( EvaluatorImpl.class );

  /**
   * Dummy value to represent null results in the expression cache.
 */
  private static final Object nullResult = new Object();
    public static final String NULL_MEMBER_IN = "null member in ";

  private final CalculableMember[] currentMembers;
  private final EvaluatorImpl parent;
  protected CellReader cellReader;
  private final int ancestorCommandCount;

  private Member expandingMember;
  private boolean firstExpanding;
  private boolean nonEmpty;
  public final EvaluatorRoot root;

  private int iterationLength;
  private boolean evalAxes;

  private final Calculation[] calculations;
  private int calculationCount;

  /**
   * List of lists of tuples or members, rarely used, but overrides the ordinary dimensional context if set when a cell
   * value comes to be evaluated.
 */
  protected final List<List<List<Member>>> aggregationLists;

  /** What a provider wants to remember about the compound slicer. Opaque here. */
  private Object slicerPredicateInfo;

  private final List<Member> slicerMembers;
  private final Map<Hierarchy, Set<Member>> slicerMembersByHierarchy;

  // slicer tuples and extra info
  private TupleList slicerTuples;
  private boolean disjointSlicerTuple;
  private boolean multiLevelSlicerTuple;

  private boolean nativeEnabled;
  private Member[] nonAllMembers;
  private int commandCount;
  private Object[] commands;

  /**
   * Set of expressions actively being expanded. Prevents infinite cycle of expansions.
   *
   * @return Mutable set of expressions being expanded
 */
  public Set<Expression> getActiveNativeExpansions() {
    return root.activeNativeExpansions;
  }

  public Object getSlicerPredicateInfo() {
    return slicerPredicateInfo;
  }

  /**
   * Keeps what an implementation worked out about the current compound slicer, handed back by
   * {@link #getSlicerPredicateInfo()}. Opaque here; only its author reads it.
   */
  public void setSlicerPredicateInfo(Object slicerPredicateInfo) {
    this.slicerPredicateInfo = slicerPredicateInfo;
  }

  /**
   * States of the finite state machine for determining the max solve order for the "scoped" behavior.
 */
  private enum ScopedMaxSolveOrderFinderState {
    START, AGG_SCOPE, CUBE_SCOPE, QUERY_SCOPE
  }

  /**
   * Creates a non-root evaluator.
   *
   * @param root
   *          Root context for stack of evaluators (contains information which does not change during the evaluation)
   * @param parent
   *          Parent evaluator, not null
   * @param aggregationList
   *          List of tuples to add to aggregation context, or null
 */
  protected EvaluatorImpl( EvaluatorRoot root, EvaluatorImpl parent, List<List<Member>> aggregationList ) {
    this.iterationLength = 1;
    this.root = root;
    assert parent != null;
    this.parent = parent;

    ancestorCommandCount = parent.ancestorCommandCount + parent.commandCount;
    nonEmpty = parent.nonEmpty;
    nativeEnabled = parent.nativeEnabled;
    evalAxes = parent.evalAxes;
    cellReader = parent.cellReader;
    currentMembers = parent.currentMembers.clone();
    calculations = parent.calculations.clone();
    calculationCount = parent.calculationCount;
    slicerMembers = new ArrayList<>( parent.slicerMembers );
    slicerMembersByHierarchy = new HashMap<>( parent.slicerMembersByHierarchy );
    slicerTuples = parent.slicerTuples;
    slicerPredicateInfo = parent.slicerPredicateInfo;
    disjointSlicerTuple = parent.disjointSlicerTuple;
    multiLevelSlicerTuple = parent.multiLevelSlicerTuple;
    expandingMember = parent.expandingMember;

    commands = new Object[10];
    commands[0] = Command.SAVEPOINT; // sentinel
    commandCount = 1;

    // Build aggregationLists, combining parent's aggregationLists (if not
    // null) and the new aggregation list (if any).
    List<List<List<Member>>> aggregationListsInner = null;
    if ( parent.aggregationLists != null ) {
      aggregationListsInner = new ArrayList<>( parent.aggregationLists );
    }

    if ( parent.slicerPredicateInfo != null ) {
      this.slicerPredicateInfo = parent.slicerPredicateInfo;
    }

    if ( aggregationList != null ) {
      if ( aggregationListsInner == null ) {
        aggregationListsInner = new ArrayList<>();
      }
      aggregationListsInner.add( aggregationList );
      List<Member> tuple = aggregationList.get( 0 );
      for ( Member member : tuple ) {
        setContext( member.getHierarchy().getAllMember() );
      }
    }
    this.aggregationLists =
        aggregationListsInner == null ? Collections.<List<List<Member>>> emptyList() : Collections.unmodifiableList(
            aggregationListsInner );
  }

  /**
   * Creates a root evaluator.
   *
   * @param root
   *          Shared context between this evaluator and its children
 */
  public EvaluatorImpl( EvaluatorRoot root ) {
    this.iterationLength = 1;
    this.root = root;
    this.parent = null;
    ancestorCommandCount = 0;
    nonEmpty = false;
    nativeEnabled =
        root.connection.getContext().getConfig().enableNativeNonEmpty()
        || root.connection.getContext().getConfig().enableNativeCrossJoin();
    evalAxes = false;
    cellReader = null;
    currentMembers = root.defaultMembers.clone();
    calculations = new Calculation[currentMembers.length];
    calculationCount = 0;
    slicerMembers = new ArrayList<>();
    slicerMembersByHierarchy = new HashMap<>();
    aggregationLists = null;

    commands = new Object[10];
    commands[0] = Command.SAVEPOINT; // sentinel
    commandCount = 1;

    for ( CalculableMember member : currentMembers ) {
      if ( member.isEvaluated() ) {
        addCalculation( member, true );
      }
    }

    // we expect client to set CellReader
  }

  /**
   * Creates an evaluator over a root a provider supplies.
   *
   * <p>The root is where the provider says how a cell is read, what the slicer evaluator
   * is and whether the result is still owed values, so there is no evaluator without one.
   */
  public static Evaluator create( EvaluatorRoot root ) {
    return new EvaluatorImpl( root );
  }

  @Override
public Cube getMeasureCube() {
    final CalculableMember measure = currentMembers[0];
    if ( measure instanceof StoredMeasure rolapStoredMeasure) {
      return rolapStoredMeasure.getCube();
    }
    return null;
  }

  @Override
public boolean mightReturnNullForUnrelatedDimension() {
    if ( !this.getCube().getCatalog().getInternalConnection().getContext()
            .getConfig().ignoreMeasureForNonJoiningDimension() ) {
      return false;
    }
    return getCube().drawsFromOtherCubes();
  }

  @Override
public boolean needToReturnNullForUnrelatedDimension( Member[] members ) {
    assert mightReturnNullForUnrelatedDimension() : "Should not even call this method if nulls are impossible";
    // The measure says what it reaches; the cube in context says whether what it does not reach is left
    // standing. Both are asked of the thing that knows, so a catalog that groups its measures some other
    // way than by cube answers them too.
    if ( !( currentMembers[0] instanceof Measure measure ) ) {
      return false;
    }
    if ( getCube().unrelatedDimensionsStayFor( measure ) ) {
      return false;
    }
    return !measure.dimensionsNotReached( members ).isEmpty();
  }

  @Override
public boolean nativeEnabled() {
    return nativeEnabled;
  }

public EvaluatorRoot getRoot() {
	return root;
}
  @Override
public boolean currentIsEmpty() {
    // If a cell evaluates to null, it is always deemed empty.
    Object o = evaluateCurrent();
    if ( NullSemantics.isNull( o ) ) {
      return true;
    }
    // Without something that counts the facts behind the measure, nobody can tell an empty cell from one
    // that works out to zero - and a measure that is worked out from a formula counts nothing at all.
    Optional<Member> counted = currentMembers[0] instanceof Measure measure ? measure.factCount()
        : Optional.empty();
    if ( counted.isEmpty() ) {
      return false;
    }
    // For other cell values (e.g. zero), the cell is deemed empty if nothing was counted behind it.
    final int savepoint = savepoint();
    try {
      setContext( counted.get() );
      o = evaluateCurrent();
    } finally {
      restore( savepoint );
    }
    return o == null || ( o instanceof Number number && number.intValue() == 0 );
  }

  @Override
public Member getPreviousContext( Hierarchy hierarchy ) {
    for ( EvaluatorImpl e = this; e != null; e = e.parent ) {
      for ( int i = commandCount - 1; i > 0; ) {
        Command command = (Command) commands[i];
        if ( command == Command.SET_CONTEXT ) {
          return (Member) commands[i - 2];
        }
        i -= command.width;
      }
    }
    return null;
  }

  @Override
public final QueryTiming getTiming() {
    return root.execution.getQueryTiming();
  }

  @Override
public final int savepoint() {
    final int commandCount1 = commandCount;
    if ( commands[commandCount - 1] == Command.SAVEPOINT ) {
      // Already at a save point; no need to create another.
      return commandCount1;
    }

    // enough room for CHECKSUM command, if asserts happen to be enabled
    ensureCommandCapacity( commandCount + 3 );
    commands[commandCount++] = Command.SAVEPOINT;
    // noinspection AssertWithSideEffects
    return commandCount1;
  }

  @Override
public final void setNativeEnabled( boolean nativeEnabled ) {
    if ( nativeEnabled != this.nativeEnabled ) {
      ensureCommandCapacity( commandCount + 2 );
      commands[commandCount++] = this.nativeEnabled;
      commands[commandCount++] = Command.SET_NATIVE_ENABLED;
      this.nativeEnabled = nativeEnabled;
    }
  }

  protected final Logger getLogger() {
    return LOGGER;
  }

  @Override
public final Member[] getMembers() {
    return currentMembers;
  }

  @Override
public final Member[] getNonAllMembers() {
    if ( nonAllMembers == null ) {
      nonAllMembers = new CalculableMember[root.nonAllPositionCount];
      for ( int i = 0; i < root.nonAllPositionCount; i++ ) {
        int nonAllPosition = root.nonAllPositions[i];
        nonAllMembers[i] = currentMembers[nonAllPosition];
      }
    }
    return nonAllMembers;
  }

  public final List<List<List<Member>>> getAggregationLists() {
    return aggregationLists;
  }

  public final void setCellReader( CellReader cellReader ) {
    if ( cellReader != this.cellReader ) {
      ensureCommandCapacity( commandCount + 2 );
      commands[commandCount++] = this.cellReader;
      commands[commandCount++] = Command.SET_CELL_READER;
      this.cellReader = cellReader;
    }
  }

  @Override
public Cube getCube() {
    return root.cube;
  }

  @Override
public final Query getQuery() {
    return root.query;
  }

  @Override
public final CatalogReader getCatalogReader() {
    return root.schemaReader;
  }

  @Override
public LocalDateTime getQueryStartTime() {
    return root.getQueryStartTime();
  }

  @Override
public EvaluatorImpl push() {
    return pushClone( null );
  }

  private void ensureCommandCapacity( int minCapacity ) {
    if ( minCapacity > commands.length ) {
      int newCapacity = commands.length * 2;
      if ( newCapacity < minCapacity ) {
        newCapacity = minCapacity;
      }
      commands = Arrays.copyOf( commands, newCapacity );
    }
  }

  /**
   * Adds a command to the stack that ensures that the state after restoring is the same as the current state.
   *
   * 
   * Returns true so that can conveniently be called from 'assert'.
   *
   * @return true
 */
  private boolean addChecksumStateCommand() {
    // assume that caller has checked that command array is large enough
    commands[commandCount++] = checksumState();
    commands[commandCount++] = Command.CHECKSUM;
    return true;
  }

  /**
   * Creates a clone of the current validator.
   *
   * @param aggregationList
   *          List of tuples to add to aggregation context, or null
 */
  protected EvaluatorImpl pushClone(List<List<Member>> aggregationList ) {
    root.execution.checkCancelOrTimeout();
    return new EvaluatorImpl( root, this, aggregationList );
  }

  @Override
public final void restore( int savepoint ) {
    while ( commandCount > savepoint ) {
      ( (Command) commands[--commandCount] ).execute( this );
    }
  }

  @Override
public final Evaluator pushAggregation( List<List<Member>> list ) {
    return pushClone( list );
  }

  /**
   * Returns true if the other object is a {@link EvaluatorImpl} with identical context.
 */
  @Override
public final boolean equals( Object obj ) {
    if ( !( obj instanceof EvaluatorImpl that ) ) {
      return false;
    }
    return Arrays.equals( this.currentMembers, that.currentMembers );
  }

  @Override
public final int hashCode() {
    return Util.hashArray( 0, this.currentMembers );
  }

  /**
   * Adds a slicer member to the evaluator context, and remember it as part of the slicer. The slicer members are passed
   * onto derived evaluators so that functions using those evaluators can choose to ignore the slicer members. One such
   * function is CrossJoin emptiness check.
   *
   * @param members
   *          members in slicer
   * @param membersByHierarchy
   *          members in slicer by hierarchy
 */
  public final void setSlicerContext( List<Member> members, Map<Hierarchy, Set<Member>> membersByHierarchy ) {
    for ( Member member : members ) {
      setContext( member );
    }
    slicerMembers.addAll( members );
    slicerMembersByHierarchy.putAll( membersByHierarchy );
  }

  /**
   * Return the list of slicer members in the current evaluator context.
   *
   * @return slicerMembers
 */
  @Override
  public final List<Member> getSlicerMembers() {
    return slicerMembers;
  }

  @Override
  public final Map<Hierarchy, Set<Member>> getSlicerMembersByHierarchy() {
    return slicerMembersByHierarchy;
  }

  /**
   * Sets the slicer tuple object, used later by native evaluation and non-empty crossjoins.
   *
   * @param tuples
   *          slicer
 */
  public final void setSlicerTuples( TupleList tuples ) {
    slicerTuples = tuples;
    if ( tuples != null ) {
      disjointSlicerTuple = isDisjointSlicerTuple( tuples );
      multiLevelSlicerTuple = hasMultipleLevelSlicer();
      slicerPredicateInfo = describeSlicer( tuples, (Measure) currentMembers[0] );
    } else {
      disjointSlicerTuple = false;
      multiLevelSlicerTuple = false;
    }
  }

  /**
   * Return the list of compound slicer tuples
 */
  public final TupleList getSlicerTuples() {
    return slicerTuples;
  }

  public boolean isDisjointSlicerTuple() {
    return disjointSlicerTuple;
  }

  public boolean isMultiLevelSlicerTuple() {
    return multiLevelSlicerTuple;
  }

  /**
   * Returns an optimized list of tuples related to the slicer based on the current evaluator. This function removes
   * overridden compound slicer members from the tuple list.
   *
   * TODO: Add Virtual Cube test cases, demonstrating unrelated dimensions. TODO: Test various Tuple List sizes - tuples
   * sizes that are bigger and smaller than list size
   *
   * @param dropWhatTheMeasureDoesNotReach
   *          whether a dimension the measure in context does not reach is taken out of the slicer
   *
   * @return optimized slicer tuple list
 */
  public final TupleList getOptimizedSlicerTuples( boolean dropWhatTheMeasureDoesNotReach ) {
    // removes members in the tuple list that are no longer compound.
    // for each member in the tuple, see if the evaluator is still set to
    // the current member
    if ( slicerTuples == null ) {
      return null;
    }
    int toRemove = 0;
    boolean[] removeMember = new boolean[slicerTuples.getArity()];
    for ( int i = 0; i < slicerTuples.get( 0 ).size(); i++ ) {
      Hierarchy h = slicerTuples.get( 0 ).get( i ).getHierarchy();
      // check to see if the current member is overridden
      // and not expanding.
      if ( !( getContext( h ) instanceof CompoundSlicerMember ) && ( getExpanding() != null
          && ( !getExpanding().getHierarchy().equals( h )
              || !( getExpanding() instanceof CompoundSlicerMember ) ) ) ) {
        toRemove++;
        removeMember[i] = true;
      }
      // A dimension the measure does not reach says nothing about its cells, so it goes too
      if ( !removeMember[i] && dropWhatTheMeasureDoesNotReach && currentMembers[0] instanceof Measure measure
          && measure.levelReached( (Level) slicerTuples.get( 0 ).get( i ).getLevel() ).isEmpty() ) {
        toRemove++;
        removeMember[i] = true;
      }
    }
    if ( toRemove == slicerTuples.getArity() ) {
      return null;
    }
    if ( toRemove > 0 ) {
      // collapse tuple
      final Set<List<Member>> processedTuples = new LinkedHashSet<>( slicerTuples.size() );
      for ( List<Member> tuple : slicerTuples ) {
        List<Member> tupleCopy = new ArrayList<>( slicerTuples.getArity() - toRemove );
        for ( int j = 0; j < tuple.size(); j++ ) {
          final Member member = tuple.get( j );
          if ( !removeMember[j] ) {
            tupleCopy.add( member );
          }
        }
        processedTuples.add( tupleCopy );
      }
      return new DelegatingTupleList( slicerTuples.getArity() - toRemove, new ArrayList<>(
          processedTuples ) );
    }
    return slicerTuples;
  }

  @Override
public final Member setContext( Member member ) {
    // Note: the body of this function is identical to calling
    // 'setContext(member, true)'. We inline the logic for performance.

    final CalculableMember m = (CalculableMember) member;
    final int ordinal = m.getHierarchy().getOrdinalInCube();
    final CalculableMember previous = currentMembers[ordinal];

    // If the context is unchanged, save ourselves some effort. It would be
    // a mistake to use equals here; we might treat the visual total member
    // 'Gender.All' the same as the true 'Gender.All' because they have the
    // same unique name, and that would be wrong.
    if ( m == previous ) {
      return previous;
    }
    // We call 'exists' before 'removeCalcMember' for efficiency.
    // 'exists' has a smaller stack to search before 'removeCalcMember'
    // adds an 'ADD_CALCULATION' command.
    if ( !exists( ordinal ) ) {
      ensureCommandCapacity( commandCount + 3 );
      commands[commandCount++] = previous;
      commands[commandCount++] = ordinal;
      commands[commandCount++] = Command.SET_CONTEXT;
    }
    if ( previous.isEvaluated() ) {
      removeCalculation( previous, false );
    }
    currentMembers[ordinal] = m;
    if ( previous.isAll() && !m.isAll() && isNewPosition( ordinal ) ) {
      root.nonAllPositions[root.nonAllPositionCount] = ordinal;
      root.nonAllPositionCount++;
    }
    if ( m.isEvaluated() ) {
      addCalculation( m, false );
    }
    nonAllMembers = null;
    return previous;
  }

  @Override
public final void setContext( Member member, boolean safe ) {
    final CalculableMember m = (CalculableMember) member;
    final int ordinal = m.getHierarchy().getOrdinalInCube();
    final CalculableMember previous = currentMembers[ordinal];

    // If the context is unchanged, save ourselves some effort. It would be
    // a mistake to use equals here; we might treat the visual total member
    // 'Gender.All' the same as the true 'Gender.All' because they have the
    // same unique name, and that would be wrong.
    if ( m == previous ) {
      return;
    }

    // We call 'exists' before 'removeCalcMember' for efficiency.
    // 'exists' has a smaller stack to search before 'removeCalcMember'
    // adds an 'ADD_CALCULATION' command.
    if (safe && !exists( ordinal ) ) {
        ensureCommandCapacity( commandCount + 3 );
        commands[commandCount++] = previous;
        commands[commandCount++] = ordinal;
        commands[commandCount++] = Command.SET_CONTEXT;
    }
    if ( previous.isEvaluated() ) {
      removeCalculation( previous, false );
    }
    currentMembers[ordinal] = m;
    if ( previous.isAll() && !m.isAll() && isNewPosition( ordinal ) ) {
      root.nonAllPositions[root.nonAllPositionCount] = ordinal;
      root.nonAllPositionCount++;
    }
    if ( m.isEvaluated() ) {
      addCalculation( m, false );
    }
    nonAllMembers = null;
  }

  /**
   * Returns whether a member of the hierarchy with a given ordinal has been preserved on the stack since the last
   * savepoint.
   *
   * @param ordinal
   *          Hierarchy ordinal
   * @return Whether there is a member with the given hierarchy ordinal on the stack
 */
  private boolean exists( int ordinal ) {
    for ( int i = commandCount - 1;; ) {
      final Command command = (Command) commands[i];
      switch ( command ) {
        case SAVEPOINT:
          return false;
        case SET_CONTEXT:
          final Integer memberOrdinal = (Integer) commands[i - 1];
          if ( ordinal == memberOrdinal ) {
            return true;
          }
          break;
        default:
          break;
      }
      i -= command.width;
    }
  }

  private boolean isNewPosition( int ordinal ) {
    for ( int nonAllPosition : root.nonAllPositions ) {
      if ( ordinal == nonAllPosition ) {
        return false;
      }
    }
    return true;
  }

  @Override
public final void setContext( List<Member> memberList ) {
    for (Member member : memberList) {
      if (member == null) {
          throw new IllegalArgumentException(NULL_MEMBER_IN + memberList);
      }
      setContext( member );
    }
  }

  @Override
public final void setContext( List<Member> memberList, boolean safe ) {
    for (Member member : memberList) {
      if (member == null) {
          throw new IllegalArgumentException(NULL_MEMBER_IN + memberList);
      }
      setContext( member, safe );
    }
  }

  @Override
public final void setContext( Member[] members ) {
    for (Member member : members) {
      if (member == null) {
          throw new IllegalArgumentException(NULL_MEMBER_IN + Arrays.toString( members ));
      }
      setContext( member );
    }
  }

  @Override
public final void setContext( Member[] members, boolean safe ) {
    for (Member member : members) {
      if (member == null) {
          throw new IllegalArgumentException("member should be not null");
      }
      setContext( member, safe );
    }
  }

  @Override
  public CalculableMember getContext( Hierarchy hierarchy ) {
    return currentMembers[hierarchy.getOrdinalInCube()];
  }

  @Override
public final Object evaluateCurrent() {
    // Get the member in the current context which is (a) calculated, and
    // (b) has the highest solve order. If there are no calculated members,
    // go ahead and compute the cell.
    Calculation maxSolveMember;
    switch ( calculationCount ) {
      case 0:
        // Seam A: unwrap the CellValue state delivered by the cell/cache
        // layer into the calc-layer conventions (MDX NULL is Java null,
        // NotLoaded propagates as the marker object, an evaluation error
        // is represented by its Throwable).
        final CellValue cv = cellReader.get( this );
        return switch ( cv ) {
          case NullValue v -> null;
          case NotLoaded n -> n;
          case ErrorValue e -> e.cause();
          case ObjectValue ov -> ov.value();
        };

      case 1:
        maxSolveMember = calculations[0];
        break;

      default:
        maxSolveMember = switch (root.solveOrderMode) {
        case ABSOLUTE -> getAbsoluteMaxSolveOrder();
        case SCOPED -> getScopedMaxSolveOrder();
        default -> throw Util.unexpected( root.solveOrderMode );
        };
    }
    final int savepoint = savepoint();
    maxSolveMember.setContextIn( this );
    final Calc calc = maxSolveMember.getCompiledExpression( root );
    final Object o;
    try {
      o = calc.evaluate( this );
    } finally {
      restore( savepoint );
    }
    return o;
  }

  public void setExpanding( Member member ) {
    assert member != null;
    ensureCommandCapacity( commandCount + 3 );
    commands[commandCount++] = this.expandingMember;
    commands[commandCount++] = this.firstExpanding;
    commands[commandCount++] = Command.SET_EXPANDING;
    expandingMember = member;
    firstExpanding = true; // REVIEW: is firstExpanding used?

    final int totalCommandCount = commandCount + ancestorCommandCount;
    if ( totalCommandCount > root.recursionCheckCommandCount ) {
      checkRecursion( this, commandCount - 4 );

      // Set the threshold where we will next check for infinite
      // recursion.
      root.recursionCheckCommandCount = totalCommandCount + ( root.defaultMembers.length << 4 );
    }
  }

  /**
   * Returns the calculated member being currently expanded.
   *
   * 
   * This can be useful if many calculated members are generated with essentially the same expression. The compiled
   * expression can call this method to find which instance of the member is current, and therefore the calculated
   * members can share the same {@link Calc} object.
   *
   * @return Calculated member currently being expanded
 */
  public Member getExpanding() {
    return expandingMember;
  }

  /**
   * Makes sure that there is no evaluator with identical context on the stack.
   *
   * @param eval
   *          Evaluator
   * @throws org.eclipse.daanse.olap.fun.MondrianEvaluationException
   *           if there is a loop
 */
  private static void checkRecursion( EvaluatorImpl eval, int c ) {
    CalculableMember[] members = eval.currentMembers.clone();

    // Find an ancestor evaluator that has identical context to this one:
    // same member context, and expanding the same calculation.
    while ( true ) {
      if ( c < 0 ) {
        eval = eval.parent;
        if ( eval == null ) {
          return;
        }
        c = eval.commandCount - 1;
      } else {
        Command command = (Command) eval.commands[c];
        switch ( command ) {
          case SET_CONTEXT:
            int memberOrdinal = (Integer) eval.commands[c - 1];
            CalculableMember member = (CalculableMember) eval.commands[c - 2];
            members[memberOrdinal] = member;
            break;
          case SET_EXPANDING:
            Member expanding = (CalculableMember) eval.commands[c - 2];
            if ( Arrays.equals( members, eval.currentMembers ) && expanding == eval.expandingMember ) {
              throw FunUtil.newEvalException( null,
                  new StringBuilder("Infinite loop while evaluating calculated member '")
                      .append(eval.expandingMember).append("'; context stack is ")
                      .append(eval.getContextString()).toString() );
            }
            break;
          default:
            break;
        }
        c -= command.width;
      }
    }
  }

  private String getContextString() {
    CalculableMember[] members = currentMembers.clone();
    final boolean skipDefaultMembers = true;
    final StringBuilder buf = new StringBuilder( "{" );
    int frameCount = 0;
    boolean changedSinceLastSavepoint = false;
    for ( EvaluatorImpl eval = this; eval != null; eval = eval.parent ) {
      if ( eval.expandingMember == null ) {
        continue;
      }
      for ( int c = eval.commandCount - 1; c > 0; ) {
        Command command = (Command) eval.commands[c];
        switch ( command ) {
          case SAVEPOINT:
            if ( changedSinceLastSavepoint ) {
              if ( frameCount++ > 0 ) {
                buf.append( ", " );
              }
              buf.append( "(" );
              int memberCount = 0;
              for ( Member m : members ) {
                if ( skipDefaultMembers && m == m.getHierarchy().getDefaultMember() ) {
                  continue;
                }
                if ( memberCount++ > 0 ) {
                  buf.append( ", " );
                }
                buf.append( m.getUniqueName() );
              }
              buf.append( ")" );
            }
            changedSinceLastSavepoint = false;
            break;
          case SET_CONTEXT:
            changedSinceLastSavepoint = true;
            int memberOrdinal = (Integer) eval.commands[c - 1];
            CalculableMember member = (CalculableMember) eval.commands[c - 2];
            members[memberOrdinal] = member;
            break;
          default:
            break;
        }
        c -= command.width;
      }
    }
    buf.append( "}" );
    return buf.toString();
  }

  @Override
public final Object getProperty( String name, Object defaultValue ) {
    Object o = defaultValue;
    int maxSolve = Integer.MIN_VALUE;
    int i = -1;
    for ( Member member : getNonAllMembers() ) {
      i++;
      // more than one usage
      if ( member == null ) {
        if ( getLogger().isDebugEnabled() ) {
          getLogger().debug( new StringBuilder("EvaluatorImpl.getProperty: member == null  , count=")
              .append(i).toString() );
        }
        continue;
      }

      // Don't call member.getPropertyValue unless this member's
      // solve order is greater than one we've already seen.
      // The getSolveOrder call is cheap call compared to the
      // getPropertyValue call, and when we're evaluating millions
      // of members, this has proven to make a significant performance
      // difference.
      final int solve = member.getSolveOrder();
      if ( solve > maxSolve ) {
        final Object p = member.getPropertyValue( name );
        if ( p != null ) {
          o = p;
          maxSolve = solve;
        }
      }
    }
    return o;
  }

  /**
   * Returns the format string for this cell. This is computed by evaluating the format expression in the current
   * context, and therefore different cells may have different format strings.
   *
   *  return != null
 */
  @Override
public final String getFormatString() {
    final Expression formatExp = (Expression) getProperty( StandardProperty.FORMAT_EXP_PARSED.getName(), null );
    if ( formatExp == null ) {
      return "Standard";
    }
    final Calc formatCalc = root.getCompiled( formatExp, true, null );
    final Object o = formatCalc.evaluate( this );
    if ( o == null ) {
      return "Standard";
    }
    return o.toString();
  }

  private Format getFormat() {
    final String formatString = getFormatString();
    return getFormat( formatString );
  }

  private Format getFormat( String formatString ) {
    return Format.get( formatString, root.connection.getLocale() );
  }

  @Override
public final Locale getConnectionLocale() {
    return root.connection.getLocale();
  }

  @Override
public final String format( Object o ) {
    if ( o instanceof Throwable ) {
      return "#ERR: " + o.toString();
    }
    Format format = getFormat();
    return format.format( o );
  }

  @Override
public final String format( Object o, String formatString ) {
    if ( o instanceof Throwable ) {
      return "#ERR: " + o.toString();
    }
    Format format = getFormat( formatString );
    return format.format( o );
  }

  /**
   * Creates a key which uniquely identifes an expression and its context. The context includes members of dimensions
   * which the expression is dependent upon.
 */
  private Object getExpResultCacheKey( ExpCacheDescriptor descriptor ) {
    boolean includeAggregationList = false;
    if ( aggregationLists != null && !aggregationLists.isEmpty() ) {
      // Don't include empty aggregation lists in the cache key or we'll get
      // a ton of cache misses due to empty collections with different hash codes
      // across different Evaluators
      includeAggregationList = true;
    }

    // in NON EMPTY mode the result depends on everything, e.g.
    // "NON EMPTY [Customer].[Name].members" may return different results
    // for 1997-01 and 1997-02
    final List<Object> key;
    if ( nonEmpty ) {
      key = new ArrayList<>( currentMembers.length + ( includeAggregationList ? 2 : 1 ) );
      key.add( descriptor.getExp() );
      // noinspection ManualArrayToCollectionCopy
      key.addAll(Arrays.asList(currentMembers));
    } else {
      final int[] hierarchyOrdinals = descriptor.getDependentHierarchyOrdinals();
      key = new ArrayList<>( hierarchyOrdinals.length + ( includeAggregationList ? 2 : 1 ) );
      key.add( descriptor.getExp() );
      for ( final int hierarchyOrdinal : hierarchyOrdinals ) {
        final Member member = currentMembers[hierarchyOrdinal];
        assert member != null;
        key.add( member );
      }
    }
    // See MONDRIAN-2713
    if ( includeAggregationList ) {
      key.add( aggregationLists );
    }
    return key;
  }

  @Override
public final Object getCachedResult( ExpCacheDescriptor cacheDescriptor ) {
    // Look up a cached result, and if not present, compute one and add to
    // cache. Use a dummy value to represent nulls.
    final Object key = getExpResultCacheKey( cacheDescriptor );
    Object result = root.getCacheResult( key );
    if ( result == null ) {
      boolean aggCacheDirty = cellReader.isDirty();
      int aggregateCacheMissCountBefore = cellReader.getMissCount();
      result = cacheDescriptor.evaluate( this );
      int aggregateCacheMissCountAfter = cellReader.getMissCount();

      boolean isValidResult;

      if ( !aggCacheDirty && ( aggregateCacheMissCountBefore == aggregateCacheMissCountAfter ) ) {
        // Cache the evaluation result as valid result if the
        // evaluation did not use any missing aggregates. Missing
        // aggregates could be used when aggregate cache is not fully
        // loaded, or if new missing aggregates are seen.
        isValidResult = true;
      } else {
        // Cache the evaluation result as invalid result if the
        // evaluation uses missing aggregates.
        isValidResult = false;
      }
      root.putCacheResult( key, result == null ? nullResult : result, isValidResult );
    } else if ( result == nullResult ) {
      result = null;
    }

    return result;
  }

  public final void clearExpResultCache( boolean clearValidResult ) {
    root.clearResultCache( clearValidResult );
  }

  @Override
public final boolean isNonEmpty() {
    return nonEmpty;
  }

  @Override
public final void setNonEmpty( boolean nonEmpty ) {
    if ( nonEmpty != this.nonEmpty ) {
      ensureCommandCapacity( commandCount + 2 );
      commands[commandCount++] = this.nonEmpty;
      commands[commandCount++] = Command.SET_NON_EMPTY;
      this.nonEmpty = nonEmpty;
    }
  }

  @Override
public final RuntimeException newEvalException( Object context, String s ) {
    return FunUtil.newEvalException( (FunctionMetaData) context, s );
  }

  @Override
public final NamedSetEvaluator getNamedSetEvaluator( NamedSet namedSet, boolean create ) {
    return root.evaluateNamedSet( namedSet, create );
  }

  @Override
public final SetEvaluator getSetEvaluator( Expression exp, boolean create ) {
    return root.evaluateSet( exp, create );
  }

  @Override
public final int getMissCount() {
    return cellReader.getMissCount();
  }

  @Override
public final Object getParameterValue( ParameterSlot slot ) {
    return root.getParameterValue( slot );
  }

  final public void addCalculation( Calculation calculation, boolean reversible ) {
    assert calculation != null;
    calculations[calculationCount++] = calculation;

    if ( reversible && !( calculation instanceof CalculableMember ) ) {
      // Add command to remove this calculation.
      ensureCommandCapacity( commandCount + 2 );
      commands[commandCount++] = calculation;
      commands[commandCount++] = Command.REMOVE_CALCULATION;
    }
  }

  /**
   * Returns the member with the highest solve order according to AS2000 rules. This was the behavior prior to solve
   * order mode being configurable.
   *
   * 
   * The SOLVE_ORDER value is absolute regardless of where it is defined; e.g. a query defined calculated member with a
   * SOLVE_ORDER of 1 always takes precedence over a cube defined value of 2.
   *
   * 
   * No special consideration is given to the aggregate function.
 */
  private Calculation getAbsoluteMaxSolveOrder() {
    // Find member with the highest solve order.
    Calculation maxSolveMember = calculations[0];
    for ( int i = 1; i < calculationCount; i++ ) {
      Calculation member = calculations[i];
      if ( expandsBefore( member, maxSolveMember ) ) {
        maxSolveMember = member;
      }
    }
    return maxSolveMember;
  }

  /**
   * Returns the member with the highest solve order according to AS2005 scoping rules.
   *
   * 
   * By default, cube calculated members are resolved before any session scope calculated members, and session scope
   * members are resolved before any query defined calculation. The SOLVE_ORDER value only applies within the scope in
   * which it was defined.
   *
   * 
   * The aggregate function is always applied to base members; i.e. as if SOLVE_ORDER was defined to be the lowest value
   * in a given evaluation in a SSAS2000 sense.
 */
  private Calculation getScopedMaxSolveOrder() {
    // Finite state machine that determines the member with the highest
    // solve order.
    Calculation maxSolveMember = null;
    ScopedMaxSolveOrderFinderState state = ScopedMaxSolveOrderFinderState.START;
    for ( int i = 0; i < calculationCount; i++ ) {
      Calculation calculation = calculations[i];
      switch ( state ) {
        case START:
          maxSolveMember = calculation;
          if ( maxSolveMember.containsAggregateFunction() ) {
            state = ScopedMaxSolveOrderFinderState.AGG_SCOPE;
          } else if ( maxSolveMember.isCalculatedInQuery() ) {
            state = ScopedMaxSolveOrderFinderState.QUERY_SCOPE;
          } else {
            state = ScopedMaxSolveOrderFinderState.CUBE_SCOPE;
          }
          break;

        case AGG_SCOPE:
          if ( calculation.containsAggregateFunction() ) {
            if (maxSolveMember != null && expandsBefore( calculation, maxSolveMember ) ) {
              maxSolveMember = calculation;
            }
          } else if ( calculation.isCalculatedInQuery() ) {
            maxSolveMember = calculation;
            state = ScopedMaxSolveOrderFinderState.QUERY_SCOPE;
          } else {
            maxSolveMember = calculation;
            state = ScopedMaxSolveOrderFinderState.CUBE_SCOPE;
          }
          break;

        case CUBE_SCOPE:
          if ( calculation.containsAggregateFunction() ) {
            continue;
          }

          if ( calculation.isCalculatedInQuery() ) {
            maxSolveMember = calculation;
            state = ScopedMaxSolveOrderFinderState.QUERY_SCOPE;
          } else if (maxSolveMember != null &&  expandsBefore( calculation, maxSolveMember ) ) {
            maxSolveMember = calculation;
          }
          break;

        case QUERY_SCOPE:
          if ( calculation.containsAggregateFunction() ) {
            continue;
          }

          if ( calculation.isCalculatedInQuery() && maxSolveMember != null && expandsBefore( calculation, maxSolveMember )) {
              maxSolveMember = calculation;
          }
          break;
      }
    }

    return maxSolveMember;
  }

  /**
   * Returns whether a given calculation expands before another. A calculation expands before another if its solve order
   * is higher, or if its solve order is the same and its dimension ordinal is lower.
   *
   * @param calc1
   *          First calculated member or tuple
   * @param calc2
   *          Second calculated member or tuple
   * @return Whether calc1 expands before calc2
 */
  private boolean expandsBefore( Calculation calc1, Calculation calc2 ) {
    final int solveOrder1 = calc1.getSolveOrder();
    final int solveOrder2 = calc2.getSolveOrder();
    if ( solveOrder1 > solveOrder2 ) {
      return true;
    } else {
      return solveOrder1 == solveOrder2 && calc1.getHierarchyOrdinal() < calc2.getHierarchyOrdinal();
    }
  }

  final void removeCalculation( Calculation calculation, boolean reversible ) {
    for ( int i = 0; i < calculationCount; i++ ) {
      if ( calculations[i] == calculation ) {
        // overwrite this member with the end member
        --calculationCount;
        calculations[i] = calculations[calculationCount];
        assert calculations[i] != null;
        calculations[calculationCount] = null; // to allow gc

        if ( reversible && !( calculation instanceof CalculableMember ) ) {
          // Add a command to re-add the calculation.
          ensureCommandCapacity( commandCount + 2 );
          commands[commandCount++] = calculation;
          commands[commandCount++] = Command.ADD_CALCULATION;
        }
        return;
      }
    }
    throw new AssertionError( new StringBuilder("calculation ")
        .append(calculation).append(" not on stack").toString() );
  }

  @Override
public final int getIterationLength() {
    return iterationLength;
  }

  @Override
public final void setIterationLength( int iterationLength ) {
    ensureCommandCapacity( commandCount + 2 );
    commands[commandCount++] = this.iterationLength;
    commands[commandCount++] = Command.SET_ITERATION_LENGTH;
    this.iterationLength = iterationLength;
  }

  @Override
public final boolean isEvalAxes() {
    return evalAxes;
  }

  @Override
public final void setEvalAxes( boolean evalAxes ) {
    if ( evalAxes != this.evalAxes ) {
      ensureCommandCapacity( commandCount + 2 );
      commands[commandCount++] = this.evalAxes;
      commands[commandCount++] = Command.SET_EVAL_AXES;
      this.evalAxes = evalAxes;
    }
  }

  private int checksumState() {
    int h = 0;
    h = h * 31 + Arrays.asList( currentMembers ).hashCode();
    h =
        h * 31 + new HashSet<>( Arrays.asList( calculations ).subList( 0, calculationCount ) )
            .hashCode();
    h = h * 31 + slicerMembers.hashCode();
    h = h * 31 + ( expandingMember == null ? 0 : expandingMember.hashCode() );
    h = h * 31 + ( aggregationLists == null ? 0 : aggregationLists.hashCode() );
    h =
        h * 31 + ( nonEmpty ? 0x1 : 0x2 ) + ( nativeEnabled ? 0x4 : 0x8 ) + ( firstExpanding ? 0x10 : 0x20 )
            + ( evalAxes ? 0x40 : 0x80 );
    if ( false ) {
      // Enable this code block to debug checksum mismatches.
        String msg = new StringBuilder("h=").append(h).append(": ").append(Arrays.asList( Arrays.asList( currentMembers ),
            new HashSet<>( Arrays.asList( calculations ).subList( 0, calculationCount ) ),
            expandingMember, aggregationLists, nonEmpty, nativeEnabled, firstExpanding, evalAxes )).toString();
        LOGGER.debug(msg);
    }
    return h;
  }

  /**
   * Checks if unrelated dimensions to the measure in the current context should be ignored.
   *
   * @return boolean
 */
  private enum Command {
    SET_CONTEXT( 2 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        --evaluator.commandCount;
        final CalculableMember member = (CalculableMember) evaluator.commands[--evaluator.commandCount];
        evaluator.setContext( member, false );
      }
    },
    SET_NATIVE_ENABLED( 1 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        evaluator.nativeEnabled = (Boolean) evaluator.commands[--evaluator.commandCount];
      }
    },
    SET_NON_EMPTY( 1 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        evaluator.nonEmpty = (Boolean) evaluator.commands[--evaluator.commandCount];
      }
    },
    SET_EVAL_AXES( 1 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        evaluator.evalAxes = (Boolean) evaluator.commands[--evaluator.commandCount];
      }
    },
    SET_EXPANDING( 2 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        evaluator.firstExpanding = (Boolean) evaluator.commands[--evaluator.commandCount];
        evaluator.expandingMember = (Member) evaluator.commands[--evaluator.commandCount];
      }
    },
    SET_ITERATION_LENGTH( 1 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        evaluator.iterationLength = (Integer) evaluator.commands[--evaluator.commandCount];
      }
    },
    SET_CELL_READER( 1 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        evaluator.cellReader = (CellReader) evaluator.commands[--evaluator.commandCount];
      }
    },
    CHECKSUM( 1 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        final int value = (Integer) evaluator.commands[--evaluator.commandCount];
        final int currentState = evaluator.checksumState();
        assert value == currentState : new StringBuilder("Current checksum ").append(currentState)
            .append(" != previous checksum ").append(value).toString();
      }
    },
    ADD_CALCULATION( 1 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        final Calculation calculation = (Calculation) evaluator.commands[--evaluator.commandCount];
        evaluator.calculations[evaluator.calculationCount++] = calculation;
      }
    },
    REMOVE_CALCULATION( 1 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        final Calculation calculation = (Calculation) evaluator.commands[--evaluator.commandCount];
        evaluator.removeCalculation( calculation, false );
      }
    },
    SAVEPOINT( 0 ) {
      @Override
      void execute( EvaluatorImpl evaluator ) {
        // nothing to do; command is just a marker
      }
    };

    public final int width;

    Command( int argCount ) {
      this.width = argCount + 1;
    }

    abstract void execute( EvaluatorImpl evaluator );
  }


  // ---- what only a provider can answer ------------------------------------

  /**
   * Whether the tuples of a compound slicer come from unrelated hierarchies, which
   * changes how the slicer may be pushed down. A provider that pushes nothing down says
   * {@code false}.
   */
  protected boolean isDisjointSlicerTuple( TupleList tuples ) {
    return false;
  }

  /** Whether the compound slicer spans more than one level of the same hierarchy. */
  protected boolean hasMultipleLevelSlicer() {
    return false;
  }

  /**
   * What the provider wants to remember about this compound slicer, handed back later by
   * {@link #getSlicerPredicateInfo()}. The engine never looks inside it. A provider that
   * needs nothing returns {@code null}.
   */
  protected Object describeSlicer( TupleList tuples, Measure measure ) {
    return null;
  }



}
