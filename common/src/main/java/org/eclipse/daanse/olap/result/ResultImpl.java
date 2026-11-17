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

import org.eclipse.daanse.olap.cellformatter.UserDefinedCellFormatter;
import org.eclipse.daanse.olap.cellformatter.LocaleCellFormatter;
import org.eclipse.daanse.olap.cellformatter.FormatStringCellFormatter;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.InternalOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.result.CellValue;
import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.result.NullValue;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.access.HierarchyAccess;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleCursor;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterator;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.DimensionType;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.LimitedMember;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.exception.CellRequestQuantumExceededException;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.execution.ExecutionContext;
import org.eclipse.daanse.olap.api.formatter.CellFormatter;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.HierarchyExpression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.ScalarType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.calc.base.cache.CacheCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedUnknownCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.DelegatingTupleList;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.ListTupleList;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;
import org.eclipse.daanse.olap.common.ExpCacheDescriptorImpl;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.exceptions.ResourceLimitExceededException;
import org.eclipse.daanse.olap.exceptions.ResultLimitExceededException;
import org.eclipse.daanse.olap.fun.DaanseEvaluationException;
import org.eclipse.daanse.olap.fun.sort.Sorter;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.AbstractAggregateFunDef;
import org.eclipse.daanse.olap.calc.base.aggregate.AggregateCalc;
import org.eclipse.daanse.olap.key.CellKey;
import org.eclipse.daanse.olap.query.component.MdxVisitorImpl;
import org.eclipse.daanse.olap.query.component.ResolvedFunCallImpl;
import  org.eclipse.daanse.olap.util.CancellationChecker;
import org.eclipse.daanse.olap.api.element.VisualTotalMember;
import org.eclipse.daanse.olap.api.result.CellReader;
import org.eclipse.daanse.olap.evaluator.CompoundSlicerMember;
import org.eclipse.daanse.olap.evaluator.CompoundSlicerMemberImpl;
import org.eclipse.daanse.olap.evaluator.DependencyTestingEvaluator;
import org.eclipse.daanse.olap.evaluator.EvaluatorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The result of running a query: determines the members of every axis, builds
 * the axes and evaluates every cell, in as many passes as the provider's cell
 * reader needs to load what it deferred.
 * <p>
 * What only a provider knows is a protected hook with a neutral default: how
 * an evaluator and a cell reader are made, how a cell is materialised, what a
 * phase loads, and the members a relational provider substitutes for rollup
 * policies and compound slicers. A provider whose reader never defers runs one
 * pass through every loop, because {@link CellReader#isDirty()} never answers
 * true.
 */
public abstract class ResultImpl extends ResultBase {

    public static final Logger LOGGER = LoggerFactory.getLogger( ResultImpl.class );
    public static final String DAANSE_EXCEPTION_IN_EXECUTE_STRIPE = "Daanse: exception in executeStripe.";

  private EvaluatorImpl evaluator;
  public EvaluatorImpl slicerEvaluator;
  private final CellKey point;

  private CellInfoContainer cellInfos;

  /**
   * The statement Locale's formatter, resolved once per execution in executeBody;
   * the Locale is constant over a statement, so the per-cell leaf path reads only this field.
   */
  private FormatStringCellFormatter defaultLocaleFormatter;
  private CellReader cellReader;
  private final CellReader aggregatingReader;
  private Modulos modulos = null;
  private final int maxEvalDepth;
  private final Map<Integer, Boolean> positionsHighCardinality = new HashMap<>();

  /**
   * Creates a result.
   *
   * @param execution
   *          Execution of a statement
   * @param execute
   *          Whether to execute the query
 */
  protected ResultImpl( final Execution execution, boolean execute ) {
    super( execution, null );
    this.maxEvalDepth = query.getConnection().getContext().getConfig().maxEvalDepth();
    int solveOrder = execution
        .getDaanseStatement().getDaanseConnection()
        .getContext().getConfig().compoundSlicerMemberSolveOrder();
    this.point = CellKey.Generator.newCellKey( axes.length );
    final Cube cube = query.getCube();
    this.cellReader = createCellReader( cube );
    this.aggregatingReader = createAggregatingCellReader();
    this.evaluator = createEvaluator();

    this.cellInfos = ( query.getAxes().length > 4 ) ? new CellInfoMap( point ) : new CellInfoPool( query.getAxes().length );

    if ( !execute ) {
      return;
    }

    boolean normalExecution = true;
    try {
      // This call to clear the cube's cache only has an
      // effect if caching has been disabled, otherwise
      // nothing happens.
      // Clear the local cache before a query has run
      beforeExecute( cube );

      /////////////////////////////////////////////////////////////////
      //
      // Evaluation Algorithm
      //
      // There are three basic steps to the evaluation algorithm:
      // 1) Determine all Members for each axis but do not save
      // information (do not build the AxisImpl),
      // 2) Save all Members for each axis (build AxisImpl).
      // 3) Evaluate and store each Cell determined by the Members
      // of the axes.
      // Step 1 converges on the stable set of Members pre axis.
      // Steps 1 and 2 make sure that the data has been loaded.
      //
      // More detail follows.
      //
      // Explicit and Implicit Members:
      // A Member is said to be 'explicit' if it appears on one of
      // the Axes (one of the AxisImpl Position List of Members).
      // A Member is 'implicit' if it is in the query but does not
      // end up on any Axes (its usage, for example, is in a function).
      // When for a Dimension none of its Members are explicit in the
      // query, then the default Member is used which is like putting
      // the Member in the Slicer.
      //
      // Special Dimensions:
      // There are 2 special dimensions.
      // The first is the Time dimension. If in a schema there is
      // no ALL Member, then Whatever happens to be the default
      // Member is used if Time Members are not explicitly set
      // in the query.
      // The second is the Measures dimension. This dimension
      // NEVER has an ALL Member. A cube's default Measure is set
      // by convention - its simply the first Measure defined in the
      // cube.
      //
      // First an evaluator is created. During its creation,
      // it gets a Member from each Hierarchy. Each Member is the
      // default Member of the Hierarchy. For most Hierarchies this
      // Member is the ALL Member, but there are cases where 1)
      // a Hierarchy does not have an ALL Member or 2) the Hierarchy
      // has an ALL Member but that Member is not the default Member.
      // In these cases, the default Member is still used, but its
      // use can cause evaluation issues (seemingly strange evaluation
      // results).
      //
      // Next, load all root Members for Hierarchies that have no ALL
      // Member and load ALL Members that are not the default Member.
      //
      // Determine the Members of the Slicer axis (Step 1 above). Any
      // Members found are added to the AxisMember object. If one of these
      // Members happens to be a Measure, then the Slicer is explicitly
      // specifying the query's Measure and this should be put into the
      // evaluator's context (replacing the default Measure which just
      // happens to be the first Measure defined in the cube). Other
      // Members found in the AxisMember object are also placed into the
      // evaluator's context since these also are explicitly specified.
      // Also, any other Members in the AxisMember object which have the
      // same Hierarchy as Members in the list of root Members for
      // Hierarchies that have no ALL Member, replace those Members - they
      // Slicer has explicitly determined which ones to use. The
      // AxisMember object is now cleared.
      // The Slicer does not depend upon the other Axes, but the other
      // Axes depend upon both the Slicer and each other.
      //
      // The AxisMember object also checks if the number of Members
      // exceeds the ResultLimit property throwing a
      // TotalMembersLimitExceeded Exception if it does.
      //
      // For all non-Slicer axes, the Members are determined (Step 1
      // above). If a Measure is found in the AxisMember, then an
      // Axis is explicitly specifying a Measure.
      // If any Members in the AxisMember object have the same Hierarchy
      // as a Member in the set of root Members for Hierarchies that have
      // no ALL Member, then replace those root Members with the Member
      // from the AxisMember object. In this case, again, a Member
      // was explicitly specified in an Axis. If this replacement
      // occurs, then one must redo this step with the new Members.
      //
      // Now Step 3 above is done. First to the Slicer Axis and then
      // to the other Axes. Here the Axes are actually generated.
      // If a Member of an Axis is an Calculated Member (and the
      // Calculated Member is not a Member of the Measure Hierarchy),
      // then find the Dimension associated with the Calculated
      // Member and remove Members with the same Dimension in the set of
      // root Members for Hierarchies that have no ALL Member.
      // This is done because via the Calculated Member the Member
      // was implicitly specified in the query. If this removal occurs,
      // then the Axes must be re-evaluated repeating Step 3.
      //
      /////////////////////////////////////////////////////////////////

      // The AxisMember object is used to hold Members that are found
      // during Step 1 when the Axes are determined.
      final AxisMemberList axisMembers = new AxisMemberList(
          query.getConnection().getContext().getConfig().resultLimit());

      // list of ALL Members that are not default Members
      final List<Member> nonDefaultAllMembers = new ArrayList<>();

      // List of Members of Hierarchies that do not have an ALL Member
      List<List<Member>> nonAllMembers = new ArrayList<>();

      // List of Measures
      final List<Member> measureMembers = new ArrayList<>();

      /////////////////////////////////////////////////////////////////
      // Determine Subcube
      //
      HashMap<Hierarchy, HashMap<Member, Member>> subcubeHierarchies = new HashMap<>();

      for(Map.Entry<Hierarchy, Calc> entry : query.getSubcubeHierarchyCalcs().entrySet()) {
        Hierarchy hierarchy = entry.getKey();
        List<? extends org.eclipse.daanse.olap.api.element.Level> levels = hierarchy.getLevels();
        org.eclipse.daanse.olap.api.element.Level lastLevel = levels.getLast();

        Calc calc = entry.getValue();

        HashMap<Member, Member> subcubeHierarchyMembers = new HashMap<>();

        org.eclipse.daanse.olap.api.type.Type memberType1 =
                new org.eclipse.daanse.olap.api.type.MemberType(
                        hierarchy.getDimension(),
                        hierarchy,
                        null,
                        null);
        SetType setType = new SetType(memberType1);
        org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc tupleListCalc =
                new org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractProfilingNestedTupleListCalc(
                        setType, new Calc[0])
                {
                  @Override
				public TupleList evaluateInternal(
                          Evaluator evaluator)
                  {
                    ArrayList<Member> children = new ArrayList<>();
                    Member expandingMember = ((EvaluatorImpl) evaluator).getExpanding();

                    if(subcubeHierarchyMembers.containsKey(expandingMember)) {
                      for(Map.Entry<Member, Member> memberEntry : subcubeHierarchyMembers.entrySet()) {
                        Member childMember = memberEntry.getValue();
                        if(childMember.getParentUniqueName() != null &&
                                childMember.getParentUniqueName().equals(expandingMember.getUniqueName())) {
                          children.add(childMember);
                        }
                      }
                    }

                    return new org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList(children);
                  }

                  @Override
				public boolean dependsOn(Hierarchy hierarchy) {
                    return true;
                  }
                };
        final org.eclipse.daanse.olap.api.type.NumericType returnType =NumericType.INSTANCE;
        final Calc partialCalc =
                new AggregateCalc( returnType, tupleListCalc, new CurrentValueUnknownCalc( returnType ) );

        OperationAtom internalOperationAtom = new InternalOperationAtom("$x");

        FunctionMetaData functionMetaData = new FunctionMetaDataR(internalOperationAtom, "x",
                DataType.NUMERIC, new FunctionParameterR[] { });
        Expression partialExp =
                new ResolvedFunCallImpl(
                        new org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition(functionMetaData) {
                          @Override
						public Calc compileCall(
                                  ResolvedFunCall call, org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler compiler)
                          {
                            return partialCalc;
                          }

                          @Override
						public void unparse(Expression[] args, PrintWriter pw) {
                            pw.print("$RollupAccessibleChildren()");
                          }
                        },
                        new Expression[0],
                        returnType);

        final TupleIterable iterable =  ( (TupleIteratorCalc<?>) calc ).evaluate( evaluator );
        TupleCursor cursor;
        if ( iterable instanceof TupleList list ) {
          cursor = list.tupleCursor();
        } else {
          // Iterable
          cursor = iterable.tupleCursor();
        }
        HierarchyAccess hierarchyAccess = org.eclipse.daanse.olap.access.RoleImpl.createAllAccess(hierarchy);
        int currentIteration = 0;
        while ( cursor.forward() ) {
          CancellationChecker.checkCancelOrTimeout( currentIteration++, execution );
          Member member = cursor.member(0);
          //must be not isLeaf()
          if(member.getLevel().getDepth() < lastLevel.getDepth()) {
            if ( member instanceof LimitedMember limited ) {
              //it could happen if there is Roles
              member = limited.getMember();
            }
            member = limitedRollupMember( member, partialExp, hierarchyAccess );
          }
          subcubeHierarchyMembers.put(member, member);
        }
        subcubeHierarchies.put(hierarchy, subcubeHierarchyMembers);

      }
      query.setSubcubeHierarchies(subcubeHierarchies);

      query.replaceSubcubeMembers();
      query.resolve();

      //Create evaluator once more. It collected default members before subcube calculation.
      this.evaluator = createEvaluator();

      // load all root Members for Hierarchies that have no ALL
      // Member and load ALL Members that are not the default Member.
      // Also, all Measures are are gathered.
      loadSpecialMembers( nonDefaultAllMembers, nonAllMembers, measureMembers );

      // clear evaluation cache
      query.clearEvalCache();

      // Save, may be needed by some Expression Calc's
      query.putEvalCache( "ALL_MEMBER_LIST", nonDefaultAllMembers );

      final List<List<Member>> emptyNonAllMembers = Collections.emptyList();

      // Initial evaluator, to execute slicer.
      // Used by named sets in slicer
      slicerEvaluator = evaluator.push();

      /////////////////////////////////////////////////////////////////
      // Determine Slicer
      //
      axisMembers.setSlicer( true );
      loadMembers( emptyNonAllMembers, evaluator, query.getSlicerAxis(), query.getSlicerCalc(), axisMembers );
      axisMembers.setSlicer( false );

      // Save unadulterated context for the next time we need to evaluate
      // the slicer.
      final EvaluatorImpl savedEvaluator = evaluator.push();

      if ( !axisMembers.isEmpty() ) {
        evaluator.setSlicerContext( axisMembers.getMembers(), axisMembers.getMembersByHierarchy() );
        for ( Hierarchy h : axisMembers.getMembersByHierarchy().keySet() ) {
          if ( h.getDimension().isMeasures() ) {
            // A Measure was explicitly declared in the
            // Slicer, don't need to worry about Measures
            // for this query.
            measureMembers.clear();
            break;
          }
        }
        replaceNonAllMembers( nonAllMembers, axisMembers );
        axisMembers.clearMembers();
      }

      // Save evaluator that has slicer as its context.
      slicerEvaluator = evaluator.push();

      /////////////////////////////////////////////////////////////////
      // Execute Slicer
      //
      Axis savedSlicerAxis;
      EvaluatorImpl internalSlicerEvaluator;
      do {
        TupleIterable tupleIterable =
            evalExecute( nonAllMembers, nonAllMembers.size() - 1, savedEvaluator, query.getSlicerAxis(),
                query.getSlicerCalc() );
        // Materialize the iterable as a list. Although it may take
        // memory, we need the first member below, and besides, slicer
        // axes are generally small.
        TupleList tupleList = TupleCollections.materialize( tupleIterable, true );

        this.slicerAxis = new AxisImpl( tupleList );
        // the slicerAxis may be overwritten during slicer execution
        // if there is a compound slicer. Save it so that it can be
        // reverted before completing result construction.
        savedSlicerAxis = this.slicerAxis;

        // Use the context created by the slicer for the other
        // axes. For example, "select filter([Customers], [Store
        // Sales] > 100) on columns from Sales where
        // ([Time].[1998])" should show customers whose 1998 (not
        // total) purchases exceeded 100.
        internalSlicerEvaluator = this.evaluator;
        if ( tupleList.size() > 1 ) {
          tupleList = removeUnaryMembersFromTupleList( tupleList, evaluator );
          tupleList = AggregateCalc.optimizeTupleList( evaluator, tupleList, false );
          evaluator.setSlicerTuples( tupleList );

          final Calc valueCalc = new CurrentValueUnknownCalc( ScalarType.INSTANCE ) ;

          final List<Member> prevSlicerMembers = new ArrayList<>();

          final Calc calcCached = new AbstractProfilingNestedUnknownCalc( query.getSlicerCalc().getType() ) {
            @Override
			public Object evaluateInternal( Evaluator evaluator ) {
              try {
                evaluator.getTiming().markStart( "EvalForSlicer" );
                TupleList list =
                    AbstractAggregateFunDef.processUnrelatedDimensions( ( (EvaluatorImpl) evaluator )
                        .getOptimizedSlicerTuples( false ), evaluator );
                for ( Member member : prevSlicerMembers ) {
                  if ( evaluator.getContext( member.getHierarchy() ) instanceof CompoundSlicerMember ) {
                    evaluator.setContext( member );
                  }
                }
                return AggregateCalc.aggregate( valueCalc, evaluator, list );
              } finally {
                evaluator.getTiming().markEnd( "EvalForSlicer" );
              }

            }

            // depend on the full evaluation context
            @Override
			public boolean dependsOn( Hierarchy hierarchy ) {
              return true;
            }
          };

          final ExpCacheDescriptorImpl cacheDescriptor =
              new ExpCacheDescriptorImpl( query.getSlicerAxis().getSet(), calcCached, evaluator );
          // Generate a cached calculation for slicer aggregation
          // This is so critical for performance that we should consider creating an
          // optimized query level slicer cache.
          final Calc calc = new CacheCalc( query.getSlicerAxis().getSet().getType(), cacheDescriptor );

          // replace the slicer set with a placeholder to avoid
          // interaction between the aggregate calc we just created
          // and any calculated members that might be present in
          // the slicer.
          // Arbitrarily picks the first dim of the first tuple
          // to use as placeholder.
          if ( tupleList.get( 0 ).size() > 1 ) {
            for ( int i = 1; i < tupleList.get( 0 ).size(); i++ ) {
              Member placeholder =
                  setPlaceholderSlicerAxis( tupleList.get( 0 ).get( i ), calc, false, tupleList, solveOrder );
              prevSlicerMembers.add( evaluator.setContext( placeholder ) );
            }
          }

          Member placeholder =
              setPlaceholderSlicerAxis( tupleList.get( 0 ).get( 0 ), calc, true, tupleList, solveOrder );

          Util.explain( evaluator.getRoot().statement.getProfileHandler(), "Axis (FILTER):", query.getSlicerCalc(), evaluator
              .getTiming() );

          evaluator.setContext( placeholder );
        }
      } while ( phase() );

      // final slicerEvaluator
      slicerEvaluator = evaluator.push();

      /////////////////////////////////////////////////////////////////
      // Determine Axes
      //
      boolean changed = false;

      // reset to total member count
      axisMembers.clearTotalCellCount();

      for ( int i = 0; i < axes.length; i++ ) {
        final QueryAxis axis = query.getAxes()[i];
        final Calc calc = query.getAxisCalcs()[i];
        loadMembers( emptyNonAllMembers, evaluator, axis, calc, axisMembers );
      }

      if ( !axisMembers.isEmpty() ) {
        for ( Member m : axisMembers ) {
          if ( m.isMeasure() ) {
            // A Measure was explicitly declared on an
            // axis, don't need to worry about Measures
            // for this query.
            measureMembers.clear();
          }
        }
        changed = replaceNonAllMembers( nonAllMembers, axisMembers );
        axisMembers.clearMembers();
      }

      if ( changed ) {
        // only count number of members, do not collect any
        axisMembers.countOnly( true );
        // reset to total member count
        axisMembers.clearTotalCellCount();

        final int savepoint = evaluator.savepoint();
        try {
          for ( int i = 0; i < axes.length; i++ ) {
            final QueryAxis axis = query.getAxes()[i];
            final Calc calc = query.getAxisCalcs()[i];
            loadMembers( nonAllMembers, evaluator, axis, calc, axisMembers );
            evaluator.restore( savepoint );
          }
        } finally {
          evaluator.restore( savepoint );
        }
      }

      // throws exception if number of members exceeds limit
      axisMembers.checkLimit();

      /////////////////////////////////////////////////////////////////
      // Execute Axes
      //
      final int savepoint = evaluator.savepoint();
      do {
        try {
          boolean redo;
          do {
            evaluator.restore( savepoint );
            redo = false;
            for ( int i = 0; i < axes.length; i++ ) {
              QueryAxis axis = query.getAxes()[i];
              final Calc calc = query.getAxisCalcs()[i];
              TupleIterable tupleIterable =
                  evalExecute( nonAllMembers, nonAllMembers.size() - 1, evaluator, axis, calc );

              if ( !nonAllMembers.isEmpty() ) {
                final TupleIterator tupleIterator = tupleIterable.tupleIterator();
                if ( tupleIterator.hasNext() ) {
                  List<Member> tuple0 = tupleIterator.next();
                  // Only need to process the first tuple on
                  // the axis.
                  for ( Member m : tuple0 ) {
                    if ( m.isCalculated() ) {
                      CalculatedMeasureVisitor visitor = new CalculatedMeasureVisitor();
                      m.getExpression().accept( visitor );
                      Dimension dimension = visitor.dimension;
                      if ( removeDimension( dimension, nonAllMembers ) ) {
                        redo = true;
                      }
                    }
                  }
                }
              }

              if ( !redo ) {
                Util.explain(
                    evaluator.getRoot().statement.getProfileHandler(),
                    new StringBuilder("Axis (").append(axis.getAxisName()).append("):").toString(),
                    calc,
                    evaluator.getTiming() );
              }

              this.axes[i] = new AxisImpl( TupleCollections.materialize( tupleIterable, false ) );
            }
          } while ( redo );
        } catch ( CellRequestQuantumExceededException e ) {
          // Safe to ignore. Need to call 'phase' and loop again.
        }
      } while ( phase() );

      evaluator.restore( savepoint );

      // Get value for each Cell
      // Cells will not be calculated if only CELL_ORDINAL requested.
      QueryComponent[] cellProperties = query.getCellProperties();
      if(!(cellProperties.length == 1
              && ((NameSegment)
              org.eclipse.daanse.olap.common.Util.parseIdentifier(cellProperties[0].toString()).getFirst()).getName().equalsIgnoreCase(
            		  StandardProperty.CELL_ORDINAL.getName()    ))) {
        final EvaluatorImpl finalInternalSlicerEvaluator = internalSlicerEvaluator;
        ExecutionContext.where(execution.asContext(), () -> {
          executeBody( finalInternalSlicerEvaluator, query, new int[axes.length] );
          Util.explain( evaluator.getRoot().statement.getProfileHandler(), "QueryBody:", null, evaluator.getTiming() );
        });
      }

      // If you are very close to running out of memory due to
      // the number of CellInfo's in cellInfos, then calling this
      // may cause the out of memory one is trying to aviod.
      // On the other hand, calling this can reduce the size of
      // the ObjectPool's internal storage by half (but, of course,
      // it will not reduce the size of the stored objects themselves).
      // Only call this if there are lots of CellInfo.
      if ( this.cellInfos.size() > 10000 ) {
        this.cellInfos.trimToSize();
      }
      // revert the slicer axis so that the original slicer
      // can be included in the result.
      this.slicerAxis = savedSlicerAxis;
    } catch ( ResultLimitExceededException ex ) {
      // If one gets a ResultLimitExceededException, then
      // don't count on anything being worth caching.
      normalExecution = false;

      // De-reference data structures that might be holding
      // partial results but surely are taking up memory.
      evaluator = null;
      slicerEvaluator = null;
      cellInfos = null;
      cellReader = null;
      for ( int i = 0; i < axes.length; i++ ) {
        axes[i] = null;
      }
      slicerAxis = null;

      query.clearEvalCache();

      throw ex;
    } finally {
      if ( normalExecution ) {
        // Expression cache duration is for each query. It is time to
        // clear out the whole expression cache at the end of a query.
        evaluator.clearExpResultCache( true );
        execution.setExpCacheCounts( evaluator.root.expResultCacheHitCount, evaluator.root.expResultCacheMissCount );
        // same per-execution duration: without this the eval cache pins
        // member sets on the query object for its whole life
        query.clearEvalCache();
      }
      if ( LOGGER.isDebugEnabled() ) {
        LOGGER.debug( "ResultImpl<init>: {}", Util.printMemory());
      }
    }
  }

  /**
   * Sets slicerAxis to a dummy placeholder AxisImpl containing a single item TupleList with the null member of
   * hierarchy. This is used with compound slicer evaluation to avoid the slicer tuple list from interacting with the
   * aggregate calc which rolls up the set. This member will contain the AggregateCalc which rolls up the set on the
   * slicer.
 */
  private Member setPlaceholderSlicerAxis( final Member member, final Calc calc, boolean setAxis,
      TupleList tupleList, int solveOrder ) {
    FormatStringCellFormatter formatter = member.getDimension().isMeasures() ? formatterFor( member ) : null;

    Member placeholderMember = compoundSlicerPlaceholder( member, calc, formatter, tupleList, solveOrder );

    if ( setAxis ) {
      TupleList dummyList = TupleCollections.createList( 1 );
      dummyList.addTuple( placeholderMember );
      this.slicerAxis = new AxisImpl( dummyList );
    }
    return placeholderMember;
  }

  private boolean phase() {
    if ( cellReader.isDirty() ) {
      tracePhase();
      // flush the expression cache during each
      // phase of loading aggregations
      evaluator.clearExpResultCache( false );
      return loadPending();
    } else {
      publishCounters();
      return false;
    }
  }

  /**
   * This function removes single instance members from the compound slicer, enabling more regular slicer behavior for
   * those members. For instance, calculated members can override the context of these members correctly.
   *
   * @param tupleList
   *          The list to shrink.
   * @param evaluator
   *          The slicer evaluator.
   * @return a new list of tuples reduced in size.
 */
  private TupleList removeUnaryMembersFromTupleList( TupleList tupleList, EvaluatorImpl evaluator ) {
    // we can remove any unary coordinates from the compound slicer, and
    // account for them in the slicer evaluator.

    // First, determine if there are any unary members within the tuples.
    List<Member> first = null;
    boolean[] unary = null;
    for ( List<Member> tuple : tupleList ) {
      if ( first == null ) {
        first = tuple;
        unary = new boolean[tuple.size()];
        for ( int i = 0; i < unary.length; i++ ) {
          unary[i] = true;
        }
      } else {
        for ( int i = 0; i < tuple.size(); i++ ) {
          if ( unary[i] && !tuple.get( i ).equals( first.get( i ) ) ) {
            unary[i] = false;
          }
        }
      }
    }
    int toRemove = 0;
    for ( int i = 0; i < unary.length; i++ ) {
      if ( unary[i] ) {
        evaluator.setContext( first.get( i ) );
        toRemove++;
      }
    }

    // remove the unnecessary members from the compound slicer
    if ( toRemove > 0 ) {
      TupleList newList = new ListTupleList( tupleList.getArity() - toRemove, new ArrayList<>() );
      for ( List<Member> tuple : tupleList ) {
        List<Member> ntuple = new ArrayList<>();
        for ( int i = 0; i < tuple.size(); i++ ) {
          if ( !unary[i] ) {
            ntuple.add( tuple.get( i ) );
          }
        }
        newList.add( ntuple );
      }
      tupleList = newList;
    }
    return tupleList;
  }

  protected boolean removeDimension( Dimension dimension, List<List<Member>> memberLists ) {
    for ( int i = 0; i < memberLists.size(); i++ ) {
      List<Member> memberList = memberLists.get( i );
      if ( memberList.get( 0 ).getDimension().equals( dimension ) ) {
        memberLists.remove( i );
        return true;
      }
    }
    return false;
  }

  @Override
  public final Execution getExecution() {
    return execution;
  }

  private static class CalculatedMeasureVisitor extends MdxVisitorImpl {
    Dimension dimension;

    CalculatedMeasureVisitor() {
    }

    @Override
	public Object visitDimensionExpression( DimensionExpression dimensionExpr ) {
      dimension = dimensionExpr.getDimension();
      return null;
    }

    @Override
	public Object visitHierarchyExpression( HierarchyExpression hierarchyExpr ) {
      Hierarchy hierarchy = hierarchyExpr.getHierarchy();
      dimension = hierarchy.getDimension();
      return null;
    }

    @Override
	public Object visitMemberExpression( MemberExpression memberExpr ) {
      Member member = memberExpr.getMember();
      dimension = member.getHierarchy().getDimension();
      return null;
    }
  }

  protected boolean replaceNonAllMembers( List<List<Member>> nonAllMembers, AxisMemberList axisMembers ) {
    boolean changed = false;
    List<Member> mList = new ArrayList<>();
    for ( ListIterator<List<Member>> it = nonAllMembers.listIterator(); it.hasNext(); ) {
      List<Member> ms = it.next();
      Hierarchy h = ms.get( 0 ).getHierarchy();
      mList.clear();
      for ( Member m : axisMembers ) {
        if ( m.getHierarchy().equals( h ) ) {
          mList.add( m );
        }
      }
      if ( !mList.isEmpty() ) {
        changed = true;
        it.set( new ArrayList<>( mList ) );
      }
    }
    return changed;
  }

  protected void loadMembers(List<List<Member>> nonAllMembers, EvaluatorImpl evaluator, QueryAxis axis, Calc calc,
                             AxisMemberList axisMembers ) {
    int attempt = 0;
    evaluator.setCellReader( cellReader );
    while ( true ) {
      axisMembers.clearAxisCount();
      final int savepoint = evaluator.savepoint();
      try {
        evalLoad( nonAllMembers, nonAllMembers.size() - 1, evaluator, axis, calc, axisMembers );
      } catch ( CellRequestQuantumExceededException e ) {
        // Safe to ignore. Need to call 'phase' and loop again.
        // Decrement count because it wasn't a recursive formula that
        // caused the iteration.
        --attempt;
      } finally {
        evaluator.restore( savepoint );
      }

      if ( !phase() ) {
        break;
      } else {
        // Clear invalid expression result so that the next evaluation
        // will pick up the newly loaded aggregates.
        evaluator.clearExpResultCache( false );
      }

      if ( attempt++ > maxEvalDepth ) {
        throw Util.newInternal( new StringBuilder("Failed to load all aggregations after ")
            .append(maxEvalDepth).append(" passes; there's probably a cycle").toString() );
      }
    }
  }

  void evalLoad( List<List<Member>> nonAllMembers, int cnt, Evaluator evaluator, QueryAxis axis, Calc calc,
      AxisMemberList axisMembers ) {
    final int savepoint = evaluator.savepoint();
    try {
      if ( cnt < 0 ) {
        executeAxis( evaluator, axis, calc, false, axisMembers );
      } else {
        for ( Member m : nonAllMembers.get( cnt ) ) {
          evaluator.setContext( m );
          evalLoad( nonAllMembers, cnt - 1, evaluator, axis, calc, axisMembers );
        }
      }
    } finally {
      evaluator.restore( savepoint );
    }
  }

  TupleIterable evalExecute( List<List<Member>> nonAllMembers, int cnt, EvaluatorImpl evaluator, QueryAxis queryAxis,
      Calc calc ) {
    final int savepoint = evaluator.savepoint();
    final int arity = calc == null ? 0 : calc.getType().getArity();
    if ( cnt < 0 ) {
      try {
        return executeAxis( evaluator, queryAxis, calc, true, null );
      } finally {
        evaluator.restore( savepoint );
      }
      // No need to clear expression cache here as no new aggregates are
      // loaded(aggregatingReader reads from cache).
    } else {
      try {
        TupleList axisResult = TupleCollections.emptyList( arity );
        for ( Member m : nonAllMembers.get( cnt ) ) {
          evaluator.setContext( m );
          TupleIterable axis = evalExecute( nonAllMembers, cnt - 1, evaluator, queryAxis, calc );
          boolean ordered = false;
          if ( queryAxis != null ) {
            ordered = queryAxis.isOrdered();
          }
          axisResult = mergeAxes( axisResult, axis, ordered );
        }
        return axisResult;
      } finally {
        evaluator.restore( savepoint );
      }
    }
  }

  /**
   * Finds all root Members 1) whose Hierarchy does not have an ALL Member, 2) whose default Member is not the ALL
   * Member and 3) all Measures.
   *
   * @param nonDefaultAllMembers
   *          List of all root Members for Hierarchies whose default Member is not the ALL Member.
   * @param nonAllMembers
   *          List of root Members for Hierarchies that have no ALL Member.
   * @param measureMembers
   *          List all Measures
 */
  protected void loadSpecialMembers( List<Member> nonDefaultAllMembers, List<List<Member>> nonAllMembers,
      List<Member> measureMembers ) {
    CatalogReader schemaReader = evaluator.getCatalogReader();
    Member[] evalMembers = evaluator.getMembers();
    for ( Member em : evalMembers ) {
      if ( em.isCalculated() ) {
        continue;
      }
      Hierarchy h = em.getHierarchy();
      Dimension d = h.getDimension();
      if ( d.getDimensionType() == DimensionType.TIME_DIMENSION) {
        continue;
      }
      if ( !em.isAll() ) {
        List<Member> rootMembers = schemaReader.getHierarchyRootMembers( h );
        if ( em.isMeasure() ) {
          for ( Member mm : rootMembers ) {
            measureMembers.add( mm );
          }
        } else {
          if ( h.hasAll() ) {
            for ( Member m : rootMembers ) {
              if ( m.isAll() ) {
                nonDefaultAllMembers.add( m );
                break;
              }
            }
          } else {
            nonAllMembers.add( rootMembers );
          }
        }
      }
    }
  }

  @Override
protected Logger getLogger() {
    return LOGGER;
  }

  public Cube getCube() {
    return evaluator.getCube();
  }

  // implement Result
  @Override
public Axis[] getAxes() {
    return axes;
  }

  /**
   * Get the Cell for the given Cell position.
   *
   * @param pos
   *          Cell position.
   * @return the Cell associated with the Cell position.
 */
  @Override
public Cell getCell( int[] pos ) {
    if ( pos.length != point.size() ) {
      throw Util.newError( "coordinates should have dimension " + point.size() );
    }

    for ( int i = 0; i < pos.length; i++ ) {
      if ( positionsHighCardinality.containsKey(i) && Boolean.TRUE.equals( positionsHighCardinality.get( i ) ) ) {
        ExecutionContext.where(execution.asContext(), () -> {
          executeBody( evaluator, statement.getQuery(), pos );
        });
        break;
      }
    }

    CellInfo ci = cellInfos.lookup( pos );
    if ( ci.value == null ) {
      for ( int i = 0; i < pos.length; i++ ) {
        int po = pos[i];
        if ( po < 0 || po >= axes[i].getPositions().size() ) {
          throw Util.newError( "coordinates out of range" );
        }
      }
      ci.value = NullValue.INSTANCE;
    }

    return createCell( pos.clone(), ci );
  }

  private TupleIterable executeAxis( Evaluator evaluator, QueryAxis queryAxis, Calc axisCalc, boolean construct,
      AxisMemberList axisMembers ) {
    if ( queryAxis == null ) {
      // Create an axis containing one position with no members (not
      // the same as an empty axis).
      return new DelegatingTupleList( 0, Collections.singletonList( Collections.<Member> emptyList() ) );
    }
    final int savepoint = evaluator.savepoint();
    try {
      evaluator.setNonEmpty( queryAxis.isNonEmpty() );
      evaluator.setEvalAxes( true );
      final TupleIterable iterable = ( (TupleIteratorCalc<?>) axisCalc ).evaluate( evaluator );
      if ( axisCalc.getClass().getName().indexOf( "OrderFunDef" ) != -1 ) {
        queryAxis.setOrdered( true );
      }
      if ( iterable instanceof TupleList list ) {
        if (!construct && axisMembers != null ) {
          axisMembers.mergeTupleList( list );
        }
      } else {
        // Iterable
        TupleCursor cursor = iterable.tupleCursor();
        if (!construct && axisMembers != null ) {
          axisMembers.mergeTupleIter( cursor );
        }
      }
      return iterable;
    } finally {
      evaluator.restore( savepoint );
    }
  }

  private void executeBody(EvaluatorImpl evaluator, Query query, final int[] pos ) {
    defaultLocaleFormatter = FormatStringCellFormatter.forLocale( statement.getDaanseConnection().getLocale() );
    // Compute the cells several times. The first time, use a dummy
    // evaluator which collects requests.
    int count = 0;
    final int savepoint = evaluator.savepoint();
    while ( true ) {
      evaluator.setCellReader( cellReader );
      try {
        executeStripe( query.getAxes().length - 1, evaluator, pos );
      } catch ( CellRequestQuantumExceededException e ) {
        // Safe to ignore. Need to call 'phase' and loop again.
        // Decrement count because it wasn't a recursive formula that
        // caused the iteration.
        --count;
      }
      evaluator.restore( savepoint );

      // Retrieve the aggregations collected.
      //
      if ( !phase() ) {
        // We got all of the cells we needed, so the result must be
        // correct.
        return;
      } else {
        // Clear invalid expression result so that the next evaluation
        // will pick up the newly loaded aggregates.
        evaluator.clearExpResultCache( false );
      }

      if ( count++ > maxEvalDepth && !onEvalDepthExceeded( evaluator, count ) ) {
        throw Util.newInternal( new StringBuilder("Query required more than ").append(count)
            .append(" iterations").toString() );
      }

      cellInfos.clear();
    }
  }

  public boolean isDirty() {
    return cellReader.isDirty();
  }

  /**
   * Evaluates an expression. Intended for evaluating named sets.
   *
   *
   * Does not modify the contents of the evaluator.
   *
   * @param calc
   *          Compiled expression
   * @param slicerEvaluator
   *          Evaluation context for slicers
   * @param contextEvaluator
   *          Evaluation context (optional)
   * @return Result
 */
  public Object evaluateExp( Calc calc, EvaluatorImpl slicerEvaluator, Evaluator contextEvaluator ) {
    int attempt = 0;

    EvaluatorImpl evaluatorInner = slicerEvaluator.push();
    if ( contextEvaluator != null && contextEvaluator.isEvalAxes() ) {
      evaluatorInner.setEvalAxes( true );
    }

    final int savepoint = evaluatorInner.savepoint();
    boolean dirty = cellReader.isDirty();
    try {
      while ( true ) {
        evaluatorInner.restore( savepoint );

        evaluatorInner.setCellReader( cellReader );
        Object preliminaryValue = calc.evaluate( evaluatorInner );

        if ( preliminaryValue instanceof TupleIterable iterable ) {
          final TupleCursor cursor = iterable.tupleCursor();
          while ( cursor.forward() ) {
            // ignore
          }
        }

        if ( !phase() ) {
          break;
        } else {
          // Clear invalid expression result so that the next
          // evaluation will pick up the newly loaded aggregates.
          evaluatorInner.clearExpResultCache( false );
        }

        if ( attempt++ > maxEvalDepth ) {
          throw Util.newInternal( new StringBuilder("Failed to load all aggregations after ")
              .append(maxEvalDepth)
              .append("passes; there's probably a cycle").toString() );
        }
      }

      // If there were pending reads when we entered, some of the other
      // expressions may have been evaluated incorrectly. Set the
      // reader's 'dirty' flag so that the caller knows that it must
      // re-evaluate them.
      if ( dirty ) {
        markDirty();
      }

      evaluatorInner.restore( savepoint );
      evaluatorInner.setCellReader( aggregatingReader );
      return calc.evaluate( evaluatorInner );
    } finally {
      evaluatorInner.restore( savepoint );
    }
  }

  private void executeStripe( int axisOrdinal, EvaluatorImpl revaluator, final int[] pos ) {
    if ( axisOrdinal < 0 ) {
      AxisImpl axis = (AxisImpl) slicerAxis;
      TupleList tupleList = axis.getTupleList();
      final Iterator<List<Member>> tupleIterator = tupleList.iterator();
      if ( tupleIterator.hasNext() ) {
        final List<Member> members = tupleIterator.next();
        execution.checkCancelOrTimeout();
        final int savepoint = revaluator.savepoint();
        revaluator.setContext( members );
        Object o;
        try {
          o = revaluator.evaluateCurrent();
        } catch ( DaanseEvaluationException e ) {
          LOGGER.warn(DAANSE_EXCEPTION_IN_EXECUTE_STRIPE, e );
          o = e;
        } finally {
          revaluator.restore( savepoint );
        }

        CellInfo ci = null;

        // Get the Cell's format string and value formatting
        // Object.
        try {
          // This code is a combination of the code found in
          // the old result
          // getCellNoDefaultFormatString method and
          // the old cell getFormattedValue method.

          // Create a CellInfo object for the given position
          // integer array.
          ci = cellInfos.create( point.getOrdinals() );

          String cachedFormatString = null;

          // Determine if there is a CellFormatter registered for
          // the current Cube's Measure's Dimension. If so,
          // then find or create a UserDefinedCellFormatter
          // for it. If not, then find or create a Locale based
          // LocaleCellFormatter.
          Hierarchy measuresHierarchy = measuresHierarchy( getCube() );
          Member m = revaluator.getContext( measuresHierarchy );
          FormatStringCellFormatter valueFormatter = formatterFor( m );
          if ( valueFormatter == null ) {
            cachedFormatString = revaluator.getFormatString();
            valueFormatter = defaultLocaleFormatter;
          }

          ci.formatString = cachedFormatString;
          ci.valueFormatter = valueFormatter;
        } catch ( ResultLimitExceededException | CellRequestQuantumExceededException e) {
          // Do NOT ignore a ResultLimitExceededException!!!
          // or We need to throw this so another phase happens.
          // or Errors indicate fatal JVM problems; do not discard
          throw e;
        } catch ( DaanseEvaluationException e ) {
          // ignore but warn
          LOGGER.warn( DAANSE_EXCEPTION_IN_EXECUTE_STRIPE, e );
        } catch ( Exception e ) {
          LOGGER.warn( DAANSE_EXCEPTION_IN_EXECUTE_STRIPE, e );
        }

        // Store the cell state as a CellValue. The evaluator delivers
        // calc-layer conventions (Java null for MDX NULL, a Throwable for
        // an evaluation error); NotLoaded results belong to a dirty pass
        // and are discarded.
        final CellValue cv = CellValue.fromLegacyValue( o );
        if ( ci != null && !( cv instanceof NotLoaded ) ) {
          ci.value = cv;
        }
      }
    } else {
      AxisImpl axis = (AxisImpl) axes[axisOrdinal];
      TupleList tupleList = axis.getTupleList();
     tupleList.size();  // force materialize

        for ( List<Member> tuple : tupleList ) {
          List<Member> measures = new ArrayList<>( statement.getQuery().getMeasuresMembers() );
          for ( Member measure : measures ) {
            if ( needsDistinctRewrite( measure ) ) {
                processDistinctMeasureExpr( tuple, measure );
            }
          }
        }

        int tupleIndex = 0;
        for ( final List<Member> tuple : tupleList ) {
          point.setAxis( axisOrdinal, tupleIndex );
          final int savepoint = revaluator.savepoint();
          try {
            revaluator.setEvalAxes( true );
            revaluator.setContext( tuple );
            execution.checkCancelOrTimeout();
            executeStripe( axisOrdinal - 1, revaluator, pos );
          } finally {
            revaluator.restore( savepoint );
          }
          tupleIndex++;

      }
    }
  }



  /**
   * Distinct counts are aggregated separately from other measures. We need to apply filters to each level in the query.
   *
   *
   * Replace VisualTotalMember expressions with new expressions where all leaf level members are included.
   * 
   *
   *
   * Example. For MDX query:
   *
   *
   *
   *
   * WITH SET [XL_Row_Dim_0] AS
   *         VisualTotals(
   *           Distinct(
   *             Hierarchize(
   *               {Ascendants([Store].[All Stores].[USA].[CA]),
   *                Descendants([Store].[All Stores].[USA].[CA])})))
   *        select NON EMPTY
   *          Hierarchize(
   *            Intersect(
   *              {DrilldownLevel({[Store].[All Stores]})},
   *              [XL_Row_Dim_0])) ON COLUMNS
   *        from [HR]
   *        where [Measures].[Number of Employees]
   *
   *
   *
   *
   *
   * For member [Store].[All Stores], we replace aggregate expression
   *
   *
   *
   *
   * Aggregate({[Store].[All Stores].[USA]})
   *
   *
   *
   *
   * with
   *
   *
   *
   *
   * Aggregate({[Store].[All Stores].[USA].[CA].[Alameda].[HQ],
   *               [Store].[All Stores].[USA].[CA].[Beverly Hills].[Store 6],
   *               [Store].[All Stores].[USA].[CA].[Los Angeles].[Store 7],
   *               [Store].[All Stores].[USA].[CA].[San Diego].[Store 24],
   *               [Store].[All Stores].[USA].[CA].[San Francisco].[Store 14]
   *              })
   *
   *
   *
   *
   *
   * TODO: Can be optimized. For that particular query we don't need to go to the lowest level. We can simply replace it
   * with:
   *
   *
   * Aggregate({[Store].[All Stores].[USA].[CA]})
   *
   *
   * Because all children of [Store].[All Stores].[USA].[CA] are included.
   * 
 */
  private List<Member> processDistinctMeasureExpr( List<Member> tuple, Member measure ) {
    for ( Member member : tuple ) {
      if ( !( member instanceof VisualTotalMember ) ) {
        continue;
      }
      evaluator.setContext( measure );
      List<Member> exprMembers = new ArrayList<>();
      processMemberExpr( member, exprMembers );
      ( (VisualTotalMember) member ).setExpression( evaluator, exprMembers );
    }
    return tuple;
  }

  private static void processMemberExpr( Object o, List<Member> exprMembers ) {
    switch (o) {
      case VisualTotalMember member -> processMemberExpr(member.getExpression(), exprMembers);
      case Member member -> exprMembers.add(member);
      case MemberExpression memberExp -> processMemberExpr(memberExp.getMember(), exprMembers);
      case ResolvedFunCallImpl funCall -> processMemberExpr(funCall.getArgs(), exprMembers);
      case Expression[] exps -> {
        for (Expression exp : exps) {
          processMemberExpr(exp, exprMembers);
        }
      }
      default -> { }
    }
  }

  /**
   * Converts a set of cell coordinates to a cell ordinal.
   *
   *
   * This method can be expensive, because the ordinal is computed from the length of the axes, and therefore the axes
   * need to be instantiated.
 */
  public int getCellOrdinal( int[] pos ) {
    if ( modulos == null ) {
      makeModulos();
    }
    return modulos.getCellOrdinal( pos );
  }

  /**
   * Instantiates the calculator to convert cell coordinates to a cell ordinal and vice versa.
   *
   *
   * To create the calculator, any axis that is based upon an Iterable is converted into a List - thus increasing memory
   * usage.
 */
  protected void makeModulos() {
    modulos = Modulos.Generator.create( axes );
  }

  /**
   * The members that form the context of a cell, for a cell that needs no Evaluator.
   *
   * @param pos
   *          Coordinates of cell
   * @return Members which form the context of the given cell
 */
  public Member[] getCellMembers( int[] pos ) {
    Member[] members = evaluator.getMembers().clone();
    for ( int i = 0; i < pos.length; i++ ) {
      Position position = axes[i].getPositions().get( pos[i] );
      for ( Member member : position ) {
        int ordinal = member.getHierarchy().getOrdinalInCube();
        members[ordinal] = member;
      }
    }
    return members;
  }

  public Evaluator getRootEvaluator() {
    return evaluator;
  }

  public Evaluator getEvaluator( int[] pos ) {
    // Set up evaluator's context, so that context-dependent format
    // strings work properly.
    Evaluator cellEvaluator = evaluator.push();
    populateEvaluator( cellEvaluator, pos );
    return cellEvaluator;
  }

  public void populateEvaluator( Evaluator evaluator, int[] pos ) {
    for ( int i = -1; i < axes.length; i++ ) {
      Axis axis;
      int index;
      if ( i < 0 ) {
        axis = slicerAxis;
        if ( axis.getPositions().isEmpty() ) {
          continue;
        }
        index = 0;
      } else {
        axis = axes[i];
        index = pos[i];
      }
      Position position = axis.getPositions().get( index );
      evaluator.setContext( position );
    }
  }


  static TupleList mergeAxes( TupleList axis1, TupleIterable axis2, boolean ordered ) {
    if ( axis1.isEmpty() && axis2 instanceof TupleList tupleList ) {
      return tupleList;
    }
    Set<List<Member>> set = new HashSet<>();
    TupleList list = TupleCollections.createList( axis2.getArity() );
    for ( List<Member> tuple : axis1 ) {
      if ( set.add( tuple ) ) {
        list.add( tuple );
      }
    }
    int halfWay = list.size();
    for ( List<Member> tuple : axis2 ) {
      if ( set.add( tuple ) ) {
        list.add( tuple );
      }
    }

    // if there are unique members on both axes and no order function,
    // sort the list to ensure default order
    if ( halfWay > 0 && halfWay < list.size() && !ordered ) {
      list = Sorter.hierarchizeTupleList( list, false );
    }

    return list;
  }


  // ---- what only a provider can answer ------------------------------------

  /**
   * The evaluator this result runs with, over a root that answers the result seam. Called twice: once
   * before and once after the subcube members are resolved, because the root collects the default
   * members. Where the context asks for dependencies to be checked, that is what it gets, whatever the
   * catalog is made of: a calc that claims not to depend on a hierarchy is made to prove it. Otherwise
   * the art says which evaluator it runs.
   */
  protected EvaluatorImpl createEvaluator() {
    int checks = dependencyChecks();
    return checks > 0 ? new DependencyTestingEvaluator( this, checks ) : plainEvaluator();
  }

  /** How often a claimed independence is put to the test; none unless the context asks for it. */
  protected int dependencyChecks() {
    return query.getConnection().getContext().getConfig().testExpDependencies();
  }

  /** The evaluator of this art when nothing special is asked for. */
  protected EvaluatorImpl plainEvaluator() {
    return new EvaluatorImpl( new ResultEvaluatorRoot( this ) );
  }

  /**
   * The reader every pass evaluates against. A reader that defers cells
   * answers {@link NotLoaded} and is dirty until {@link #loadPending()} has
   * loaded them; a reader that never defers is never dirty.
   */
  protected abstract CellReader createCellReader( Cube cube );

  /**
   * The reader the final evaluation of a named set runs against, after every
   * pending load. The cell reader itself, unless the provider keeps a
   * separate cache reader.
   */
  protected CellReader createAggregatingCellReader() {
    return cellReader;
  }

  /** The cell for a position and the cell info the evaluation filled. */
  protected abstract Cell createCell( int[] pos, CellInfo ci );

  /**
   * Loads what the cell reader deferred during the last pass; answers whether
   * anything was loaded, i.e. whether the pass has to run again.
   */
  protected boolean loadPending() {
    return false;
  }

  /** Marks the cell reader dirty again after a nested evaluation loaded on its behalf. */
  protected void markDirty() {
    // nothing deferred, nothing to re-arm
  }

  /** Reports the counters of a loading phase to the execution. */
  protected void tracePhase() {
    // nothing to trace
  }

  /** Publishes the final cell-cache counters to the execution. */
  protected void publishCounters() {
    // nothing to publish
  }

  /** Runs before the first pass; a provider clears per-query caches here. */
  protected void beforeExecute( Cube cube ) {
    // nothing to clear
  }

  /** The measures hierarchy of a cube. */
  protected Hierarchy measuresHierarchy( Cube cube ) {
    for ( Hierarchy hierarchy : cube.getHierarchies() ) {
      if ( hierarchy.getDimension().isMeasures() ) {
        return hierarchy;
      }
    }
    throw Util.newInternal( "cube " + cube.getName() + " has no measures hierarchy" );
  }

  /** The formatter a measure declares, or {@code null} for the locale's format string. */
  protected FormatStringCellFormatter formatterFor( Member measure ) {
    return null;
  }

  /**
   * Whether a measure aggregates distinct values, so that a visual total on
   * its axis must be rewritten over leaf members.
   */
  protected boolean needsDistinctRewrite( Member measure ) {
    return false;
  }

  /**
   * The member that stands for {@code source} under a rollup policy, computing
   * {@code exp} over the accessible children. A provider without rollup
   * policies returns the source.
   */
  protected Member limitedRollupMember( Member source, Expression exp, HierarchyAccess access ) {
    return source;
  }

  /**
   * The placeholder member that carries a compound slicer: the null member of
   * the member's hierarchy, evaluating {@code calc} over {@code tuples}, with
   * the member's format properties. The default is the neutral
   * {@link CompoundSlicerMemberImpl}; a provider whose members carry more
   * brings its own.
   */
  protected Member compoundSlicerPlaceholder( Member member, Calc calc, FormatStringCellFormatter formatter, TupleList tuples,
      int solveOrder ) {
    CompoundSlicerMemberImpl placeholder =
        new CompoundSlicerMemberImpl( member.getHierarchy().getNullMember(), calc, tuples, solveOrder );
    placeholder.setProperty( StandardProperty.FORMAT_STRING.getName(),
        member.getPropertyValue( StandardProperty.FORMAT_STRING.getName() ) );
    placeholder.setProperty( StandardProperty.FORMAT_EXP_PARSED.getName(),
        member.getPropertyValue( StandardProperty.FORMAT_EXP_PARSED.getName() ) );
    return placeholder;
  }

  /**
   * Called when a cell pass exceeds the configured evaluation depth; answers
   * whether to keep going. {@code false} raises the error.
   */
  protected boolean onEvalDepthExceeded( EvaluatorImpl evaluator, int count ) {
    if ( evaluator instanceof DependencyTestingEvaluator ) {
      // Checking dependencies can start a new request every cycle: let it run as usual for a while,
      // then stop checking rather than stop answering.
      ( (DependencyTestingEvaluator.DteRoot) evaluator.getRoot() ).disabled = true;
      return count <= maxEvalDepth() * 2;
    }
    return false;
  }

  /** The evaluator of this result, for a subclass that narrows its type. */
  protected EvaluatorImpl evaluator() {
    return evaluator;
  }

  /** The cell reader of this result. */
  protected CellReader cellReader() {
    return cellReader;
  }

  protected int maxEvalDepth() {
    return maxEvalDepth;
  }

}
