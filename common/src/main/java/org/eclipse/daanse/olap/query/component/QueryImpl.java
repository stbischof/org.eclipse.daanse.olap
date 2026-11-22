/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1998-2005 Julian Hyde
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
package org.eclipse.daanse.olap.query.component;



import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.NameResolver;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.access.AccessMember;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;
import org.eclipse.daanse.olap.api.calc.profile.ProfileHandler;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.KeyMember;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.LimitedMember;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.MetaData;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.execution.ExecutionContext;
import org.eclipse.daanse.olap.api.execution.QueryTiming;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.IdentifierSegment;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.AxisOrdinal;
import org.eclipse.daanse.olap.api.query.component.CellProperty;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.FunctionCall;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.MemberProperty;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.query.component.Subcube;
import org.eclipse.daanse.olap.api.query.component.UnresolvedFunCall;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.api.calc.profile.SimpleCalculationProfileWriter;
import org.eclipse.daanse.olap.common.AbstractQueryPart;
import org.eclipse.daanse.olap.common.ConfigConstants;
import org.eclipse.daanse.olap.common.DelegatingCatalogReader;
import org.eclipse.daanse.olap.common.ExecuteDurationUtil;
import org.eclipse.daanse.olap.common.IdBatchResolver;
import org.eclipse.daanse.olap.common.NameResolverImpl;
import org.eclipse.daanse.olap.common.ParameterImpl;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.common.ValidatorImpl;
import org.eclipse.daanse.olap.common.Walker;
import org.eclipse.daanse.olap.element.OlapMetaDataBase;
import org.eclipse.daanse.olap.exceptions.MdxAxisShowSubtotalsNotSupportedException;
import org.eclipse.daanse.olap.exceptions.ParameterIsNotModifiableException;
import org.eclipse.daanse.olap.exceptions.ResultStyleException;
import org.eclipse.daanse.olap.exceptions.UnknownParameterException;
import org.eclipse.daanse.olap.execution.ExecutionImpl;
import org.eclipse.daanse.olap.query.base.ParameterExpressions;
import org.eclipse.daanse.olap.query.IdentifierParser;
import org.eclipse.daanse.olap.util.ArrayStack;
import org.eclipse.daanse.olap.util.type.TypeUtil;

/**
 * Query is an MDX query.
 *
 * It is created by calling {@link Connection#parseQuery},
 * and executed by calling {@link Connection#execute},
 * to return a {@link Result}.
 *
 * Query control
 *
 * Most queries are model citizens, executing quickly (often using cached
 * results from previous queries), but som queries take more time, or more
 * database resources, or more results, than is reasonable. Mondrian offers
 * three ways to control rogue queries:
 *
 * You can set a query timeout by setting the
 *     QueryTimeout parameter. If the query
 *     takes longer to execute than the value of this parameter, the system
 *     will kill it.
 *
 * The ConfigConstants#QUERY_LIMIT setting limits the number
 *     of cells returned by a query.
 *
 * At any time while a query is executing, another thread can cancel the
 *     query by calling
 *     {@link #getStatement()}.{@link Statement#cancel() cancel()}.
 *     The call to {@link Connection#execute(QueryImpl)}
 *     will throw an exception.
 *
 *
 *
 * @author jhyde, 20 January, 1999
 */
public class QueryImpl extends AbstractQueryPart implements Query {

    private Formula[] formulas;

    /** Public because an implementation of the connection still reaches in here. */
    public QueryAxis[] axes;

    private QueryAxis slicerAxis;

    /**
     * Definitions of all parameters used in this query.
     */
    private final List<Parameter> parameters = new ArrayList<>();

    private final Map<String, Parameter> parametersByName =
        new HashMap<>();

    /**
     * Cell properties. Not currently used.
     */
    private final CellProperty[] cellProperties;

    /**
     * Cube this query belongs to.
     */
    private final Cube cube;

    private Subcube subcube;

    private final Statement statement;
    public Calc<?>[] axisCalcs;
    public Calc<?> slicerCalc;

    /**
     * Set of FunDefs for which alerts about non-native evaluation
     * have already been posted.
     */
    Set<FunctionDefinition> alertedNonNativeFunDefs;

    /**
     * Unique list of members referenced from the measures dimension.
     * Will be used to determine if cross joins can be processed natively
     * for virtual cubes.
     */
    private Set<Member> measuresMembers;

    /**
     * If true, virtual cubes can be processed using native cross joins.
     * It defaults to true, unless functions are applied on measures.
     */
    private boolean nativeCrossJoinVirtualCube;

    /**
     * Used for virtual cubes.
     * Comtains a list of base cubes related to a virtual cube
     */
    private List<Cube> baseCubes;

    /**
     * If true, enforce validation even when ignoreInvalidMembers is set.
     */
    private boolean strictValidation;

    /**
     * How should the query be returned? Valid values are:
     *    ResultStyle.ITERABLE
     *    ResultStyle.LIST
     *    ResultStyle.MUTABLE_LIST
     * For java4, use LIST
     */
    private ResultStyle resultStyle = ResultStyle.ITERABLE;

    private Map<String, Object> evalCache = new HashMap<>();

    /**
     * List of aliased expressions defined in this query, and where they are
     * defined. There might be more than one aliased expression with the same
     * name.
     */
    private final List<ScopedNamedSet> scopedNamedSets =
        new ArrayList<>();
    private boolean ownStatement;
    private final static String calculatedMember = "calculated member";
    private final static String calculatedSet = "calculated set";
    private final static String mdxFormulaNotFound = "Calculated {0} ''{1}'' has not been found in query ''{2}''";
    private final static String mdxCalculatedFormulaUsedOnAxis = "Cannot delete {0} ''{1}''. It is used on {2} axis.";
    private final static String duplicateAxis = "Duplicate axis name ''{0}''.";
    private final static String hierarchyInIndependentAxes =
        "Hierarchy ''{0}'' appears in more than one independent axis.";
    private final static String mdxCalculatedFormulaUsedInFormula =
        "Cannot delete {0} ''{1}''. It is used in definition of {2} ''{3}''.";
    private final static String mdxCalculatedFormulaUsedInQuery =
        "Cannot delete {0} ''{1}''. It is used in query ''{2}''.";
    private final static String mdxCalculatedFormulaUsedOnSlicer = "Cannot delete {0} ''{1}''. It is used on slicer.";
    private final static String nonContiguousAxis = """
    Axis numbers specified in a query must be sequentially specified, and cannot contain gaps. Axis {0,number} ({1}) is missing.
    """;
    public final static String parameterDefinedMoreThanOnce =
        "Parameter ''{0}'' is defined more than once in this statement";

    /**
   * Creates a Query.
   */
  public QueryImpl(Statement statement, Formula[] formulas, QueryAxis[] axes, String cubeName, QueryAxis slicerAxis,
               CellProperty[] cellProps, boolean strictValidation ) {
      this(
              statement,
              Util.lookupCube( statement.getCatalogReader(), cubeName, true ),
              formulas,
              new SubcubeImpl(cubeName, null, new QueryAxisImpl[] {}, null),
              axes,
              slicerAxis,
              cellProps,
              new Parameter[0],
              strictValidation );
  }

  public QueryImpl(Statement statement, Formula[] formulas, QueryAxis[] axes, Subcube subcube, QueryAxis slicerAxis,
                   CellProperty[] cellProps, boolean strictValidation ) {
    this( statement, Util.lookupCube( statement.getCatalogReader(), subcube.getCubeName(), true ), formulas, subcube, axes, slicerAxis, cellProps,
        new Parameter[0], strictValidation );
  }

    /**
     * Creates a Query.
     */
    public QueryImpl(
            Statement statement,
            Cube mdxCube,
            Formula[] formulas,
            QueryAxis[] axes,
            QueryAxis slicerAxis,
            CellProperty[] cellProps,
            Parameter[] parameters,
            boolean strictValidation)
    {
        this(
            statement,
            mdxCube,
            formulas,
            null,
            axes,
            slicerAxis,
            cellProps,
            parameters,
            strictValidation);

    }

    public QueryImpl(
        Statement statement,
        Cube mdxCube,
        Formula[] formulas,
        Subcube subcube,
        QueryAxis[] axes,
        QueryAxis slicerAxis,
        CellProperty[] cellProps,
        Parameter[] parameters,
        boolean strictValidation)
    {
        this.statement = statement;
        this.cube = mdxCube;
        this.formulas = formulas;
        this.subcube = subcube;
        this.axes = axes;
        normalizeAxes();
        applyNonEmptyOnAllAxis();
        this.slicerAxis = slicerAxis;
        this.cellProperties = cellProps;
        this.parameters.addAll(Arrays.asList(parameters));
        this.measuresMembers = new HashSet<>();
        // assume, for now, that cross joins on virtual cubes can be
        // processed natively; as we parse the query, we'll know otherwise
        this.nativeCrossJoinVirtualCube = true;
        this.strictValidation = strictValidation;
        this.alertedNonNativeFunDefs = new HashSet<>();
        statement.setQuery(this);
        resolve();

        if (Util.PROFILE_LOGGER.isDebugEnabled()
            && statement.getProfileHandler() == null)
        {
            statement.enableProfiling(
                new ProfileHandler() {
                    @Override
					public void explain(String plan, QueryTiming timing) {
                        if (timing != null) {
                            plan += "\n" + timing;
                        }
                        Util.PROFILE_LOGGER.debug(plan);
                    }
                }
            );
        }
    }

    public QueryImpl(QueryImpl query) {
        this(
            query.statement,
            query.cube,
            FormulaImpl.cloneArray(query.formulas),
            query.subcube,
            QueryAxisImpl.cloneArray(query.axes),
            (query.slicerAxis == null) ? null : new QueryAxisImpl(query.slicerAxis),
            query.cellProperties,
            query.parameters.toArray(new Parameter[query.parameters.size()]),
            query.strictValidation);
    }



  public QueryComponent[] getCellProperties() {
        return this.cellProperties;
    }

    /**
     * Checks whether the property name is present in the query.
     */
    public boolean hasCellProperty(String propertyName) {
        for (QueryComponent cellProp : cellProperties) {
            if (((CellProperty)cellProp).isNameEquals(propertyName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether any cell property present in the query
     */
    public boolean isCellPropertyEmpty() {
        return cellProperties.length == 0;
    }

    /**
     * Adds a new formula specifying a set
     * to an existing query.
     */
    public void addFormula(Id id, Expression exp) {
        addFormula(
            new FormulaImpl(false, id, exp, new MemberProperty[0], null, null));
    }

    /**
     * Adds a new formula specifying a member
     * to an existing query.
     *
     * @param id Name of member
     * @param exp Expression for member
     * @param memberProperties Properties of member
     */
    public void addFormula(
        Id id,
        Expression exp,
        MemberProperty[] memberProperties)
    {
        addFormula(new FormulaImpl(true, id, exp, memberProperties, null, null));
    }

    /**
     * Adds a new formula specifying a member or a set
     * to an existing query; resolve is called after
     * the formula has been added.
     *
     * @param formula Formula to add to query
     */
    public void addFormula(Formula formula) {
        formulas = Util.append(formulas, formula);
        resolve();
    }

    /**
     * Adds some number of new formulas specifying members
     * or sets to an existing query; resolve is only called
     * once, after all the new members have been added to
     * the query.
     *
     * @param additions Formulas to add to query
     */
    public void addFormulas(Formula... additions) {
        formulas = Util.appendArrays(formulas, additions);
        resolve();
    }

    /**
     * Creates a validator for this query.
     *
     * @return Validator
     */
    public Validator createValidator() {
        return createValidator(new HashMap<>());
    }


    public Validator createValidator(
        Map<QueryComponent, QueryComponent> resolvedIdentifiers)
    {
        return createValidator(
                statement.getDaanseConnection().getContext().getFunctionService(),
            false,
            resolvedIdentifiers);
    }

    /**
     * Creates a validator for this query that uses a given function table and
     * function validation policy.
     *
     * @param functionTable Function table
     * @param alwaysResolveFunDef Whether to always resolve function
     *     definitions (see {@link Validator#alwaysResolveFunDef()})
     * @return Validator
     */
    public Validator createValidator(
        FunctionService functionTable,
        boolean alwaysResolveFunDef)
    {
        return new QueryValidator(
            functionTable,
            alwaysResolveFunDef,
            QueryImpl.this,
            new HashMap<>());
    }


    public Validator createValidator(
    		FunctionService functionTable,
        boolean alwaysResolveFunDef,
        Map<QueryComponent, QueryComponent> resolvedIdentifiers)
    {
        return new QueryValidator(
            functionTable,
            alwaysResolveFunDef,
            QueryImpl.this,
            resolvedIdentifiers);
    }

    public Connection getConnection() {
        return statement.getDaanseConnection();
    }

    /**
     * Determines whether an alert for non-native evaluation needs
     * to be posted.
     *
     * @param funDef function type to alert for
     *
     * @return true if alert should be raised
     */
    public boolean shouldAlertForNonNative(FunctionDefinition funDef) {
        return alertedNonNativeFunDefs.add(funDef);
    }

    /**
     * With enableNonEmptyOnAllAxis every axis of the query behaves as if it had
     * been written NON EMPTY. The slicer is excluded.
     *
     * <p>
     * This lives here rather than in the axis constructor because axes are built
     * while parsing, before any execution is bound - the axis has no way to reach
     * the Context, this class does.
     * </p>
     */
    private void applyNonEmptyOnAllAxis() {
        boolean nonEmptyOnAllAxis = statement.getDaanseConnection().getContext().getConfig().enableNonEmptyOnAllAxis();
        if (!nonEmptyOnAllAxis) {
            return;
        }
        for (QueryAxis axis : axes) {
            if (axis != null && !axis.getAxisOrdinal().isFilter()) {
                axis.setNonEmpty(true);
            }
        }
    }

    private void normalizeAxes() {
        for (int i = 0; i < axes.length; i++) {
            AxisOrdinal correctOrdinal =
                AxisOrdinal.StandardAxisOrdinal.forLogicalOrdinal(i);
            if (axes[i].getAxisOrdinal() != correctOrdinal) {
                for (int j = i + 1; j < axes.length; j++) {
                    if (axes[j].getAxisOrdinal() == correctOrdinal) {
                        // swap axes
                        QueryAxis temp = axes[i];
                        axes[i] = axes[j];
                        axes[j] = temp;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Performs type-checking and validates internal consistency of a query,
     * using the default resolver.
     *
     * This method is called automatically when a query is created; you need
     * to call this method manually if you have modified the query's expression
     * tree in any way.
     */
    public void resolve() {
        // Before commencing validation, create all calculated members
        // and calculated sets
        createFormulaElements();
        Map<QueryComponent, QueryComponent> resolvedIdentifiers =
            new IdBatchResolver(this).resolve();
        final Validator validator = createValidator(resolvedIdentifiers);
        resolve(validator); // resolve self and children
        // Create a dummy result so we can use its evaluator
        final Evaluator evaluator = getConnection().getContext().createDummyEvaluator(statement);
        ExpressionCompiler compiler =
            createCompiler(
                evaluator, validator, Collections.singletonList(resultStyle));
        compile(compiler);
    }

    private void createFormulaElements() {
        if (formulas != null) {
            // Resolving of formulas should be done in two parts
            // because formulas might depend on each other, so all calculated
            // mdx elements have to be defined during resolve.
            for (Formula formula : formulas) {
                formula.createElement(this);
            }
        }
    }

    /**
     * @return true if the relevant property for ignoring invalid members is
     * set to true for this query's environment (a different property is
     * checked depending on whether environment is schema load vs query
     * validation)
     */
    public boolean ignoreInvalidMembers()
    {
        final boolean load = getCube().isLoadInProgress();
        return
            !strictValidation
            && (load
                ? getConnection().getContext()
                        .getConfig().ignoreInvalidMembers()
                : getConnection().getContext()
                .getConfig().ignoreInvalidMembersDuringQuery());
    }

    /**
     * A Query's ResultStyle can only be one of the following:
     *   ResultStyle.ITERABLE
     *   ResultStyle.LIST
     *   ResultStyle.MUTABLE_LIST
     */
    public void setResultStyle(ResultStyle resultStyle) {
        switch (resultStyle) {
        case ITERABLE:
        case LIST:
        case MUTABLE_LIST:
            this.resultStyle = resultStyle;
            break;
        default:
            throw ResultStyleException.generateBadType(
                ResultStyle.ITERABLE_LIST_MUTABLELIST,
                resultStyle);
        }
    }

    public ResultStyle getResultStyle() {
        return resultStyle;
    }

    public Map<Hierarchy, Calc> subcubeHierarchyCalcs = new HashMap<>();
    public Map<Hierarchy, HashMap<Member, Member>> subcubeHierarchies = new HashMap<>();

    /**
     * Generates compiled forms of all expressions.
     *
     * @param compiler Compiler
     */
    private void compile(ExpressionCompiler compiler) {

        if(this.subcube != null) {

            for(Hierarchy hierarchy : getCube().getHierarchies()) {

                List<org.eclipse.daanse.olap.api.element.Level> levels = (List<Level>) hierarchy.getLevels();
                org.eclipse.daanse.olap.api.element.Level lastLevel = levels.getLast();
                LevelExpressionImpl levelExpr = new LevelExpressionImpl(lastLevel);
                Expression levelMembers = new UnresolvedFunCallImpl( new PlainPropertyOperationAtom("AllMembers"),
                  new Expression[] {levelExpr}
                );

                Expression resultExp = null;

                List<Expression> subcubeAxisExps = this.subcube.getAxisExps();
                ArrayList<Expression> hierarchyExps = new ArrayList<>();
                for(int j = 0; j < subcubeAxisExps.size(); j++) {
                    Expression subcubeAxisExp = subcubeAxisExps.get(j);
                    subcubeAxisExp = subcubeAxisExp.accept(compiler.getValidator());
                    Hierarchy[] subcubeAxisHierarchies = collectHierarchies(subcubeAxisExp);
                    if(Arrays.asList(subcubeAxisHierarchies).contains(hierarchy)) {
                        hierarchyExps.add(subcubeAxisExp);
                    }
                }
                for(Expression hierarchyExp: hierarchyExps) {
                    Expression prevExp;
                    if(resultExp == null) {
                        prevExp = levelMembers;
                    }
                    else {
                        prevExp = resultExp;
                    }
                    Expression axisInBracesExp =
                            new UnresolvedFunCallImpl(
                                    new BracesOperationAtom(), new Expression[] {hierarchyExp});
                    if(hierarchyExps.size() > 1) {
                        resultExp = new UnresolvedFunCallImpl(
                        		new FunctionOperationAtom("Exists"),
                                new Expression[] {prevExp, axisInBracesExp}
                        );
                    }
                    else {
                        resultExp = axisInBracesExp;
                    }
                }

                if(resultExp != null) {
                    HierarchyExpressionImpl hierarchyExpr = new HierarchyExpressionImpl(hierarchy);
                    Expression hierarchyAllMembersExp = new UnresolvedFunCallImpl(
                    		new PlainPropertyOperationAtom("AllMembers"),
                            new Expression[] {hierarchyExpr}
                    );

                    resultExp = new UnresolvedFunCallImpl(
                    		new FunctionOperationAtom("Exists"),
                            new Expression[] {hierarchyAllMembersExp, resultExp}
                    );

                    resultExp = resultExp.accept(compiler.getValidator());

                    Calc calc = compiler.compileList(resultExp);
                    subcubeHierarchyCalcs.put(hierarchy, calc);

                }
            }
        }

        if (formulas != null) {
            for (Formula formula : formulas) {
                formula.compile();
            }
        }

        if (axes != null) {
            axisCalcs = new Calc[axes.length];
            for (int i = 0; i < axes.length; i++) {
                axisCalcs[i] = axes[i].compile(compiler, resultStyle);
            }
        }
        if (slicerAxis != null) {
            slicerCalc = slicerAxis.compile(compiler, resultStyle);
        }
    }

    /**
     * Performs type-checking and validates internal consistency of a query.
     *
     * @param validator Validator
     */
    public void resolve(Validator validator) {
        // Register all parameters.
        parameters.clear();
        parametersByName.clear();
        accept(new ParameterFinder());

        // Register all aliased expressions ('expr AS alias') as named sets.
        accept(new AliasedExpressionFinder());

        // Validate formulas.
        if (formulas != null) {
            for (Formula formula : formulas) {
                validator.validate(formula);
            }
        }

        // Validate axes.
        if (axes != null) {
            Set<Integer> axisNames = new HashSet<>();
            for (QueryAxis axis : axes) {
                validator.validate(axis);
                if (!axisNames.add(axis.getAxisOrdinal().logicalOrdinal())) {
                    throw new OlapRuntimeException(MessageFormat.format(duplicateAxis,
                        axis.getAxisName()));
                }
            }

            // Make sure that there are no gaps. If there are N axes, then axes
            // 0 .. N-1 should exist.
            int seekOrdinal =
                AxisOrdinal.StandardAxisOrdinal.COLUMNS.logicalOrdinal();
            for (QueryAxis axis : axes) {
                if (!axisNames.contains(seekOrdinal)) {
                    AxisOrdinal axisName =
                        AxisOrdinal.StandardAxisOrdinal.forLogicalOrdinal(
                            seekOrdinal);
                    throw new OlapRuntimeException(MessageFormat.format(nonContiguousAxis,
                        seekOrdinal,
                        axisName.name()));
                }
                ++seekOrdinal;
            }
        }
        if (slicerAxis != null) {
            slicerAxis.validate(validator);
        }

        // Make sure that no hierarchy is used on more than one axis.
        for (Hierarchy hierarchy :  getCube().getHierarchies()) {
            int useCount = 0;
            for (QueryAxis axis : allAxes()) {
                if (axis.getSet().getType().usesHierarchy(hierarchy, true)) {
                    ++useCount;
                }
            }
            if (useCount > 1) {
                throw new OlapRuntimeException(MessageFormat.format(hierarchyInIndependentAxes,
                    hierarchy.getUniqueName()));
            }
        }
    }

    @Override
    public void explain(PrintWriter pw) {
    	SimpleCalculationProfileWriter spw = new SimpleCalculationProfileWriter(pw);
        final boolean profiling = getStatement().getProfileHandler() != null;
        for (Formula formula : formulas) {
            formula.getMdxMember(); // TODO:
        }
        if (slicerCalc != null) {
            pw.println("Axis (FILTER):");

				CalculationProfile calcProfile = slicerCalc.getCalculationProfile();
				spw.write(calcProfile);


            pw.println();
        }
        int i = -1;
        for (QueryAxis axis : axes) {
            ++i;
			Calc<?> axisCalc = axisCalcs[i];
            pw.println(new StringBuilder("Axis (").append(axis.getAxisName()).append("):").toString());


				CalculationProfile calcProfile = axisCalc.getCalculationProfile();
				spw.write(calcProfile);




            pw.println();
        }
        pw.flush();
    }

    /**
     * Returns a collection of all axes, including the slicer as the first
     * element, if there is a slicer.
     *
     * @return Collection of all axes including slicer
     */
	private Collection<QueryAxis> allAxes() {
		if (slicerAxis == null) {
			return Arrays.asList(axes);
		} else {
			// noinspection unchecked
			Collection<QueryAxis> composite = new ArrayList<>();
			composite.add(slicerAxis);
			composite.addAll(Arrays.asList(axes));
			return composite;
		}
    }

    @Override
	public void unparse(PrintWriter pw) {
        if (formulas != null) {
            for (int i = 0; i < formulas.length; i++) {
                if (i == 0) {
                    pw.print("with ");
                } else {
                    pw.print("  ");
                }
                formulas[i].unparse(pw);
                pw.println();
            }
        }
        pw.print("select ");
        if (axes != null) {
            for (int i = 0; i < axes.length; i++) {
                axes[i].unparse(pw);
                if (i < axes.length - 1) {
                    pw.println(",");
                    pw.print("  ");
                } else {
                    pw.println();
                }
            }
        }
        if (subcube != null) {
            pw.print("from ");
            subcube.unparse(pw);
        }
        if (slicerAxis != null) {
            pw.print("where ");
            slicerAxis.unparse(pw);
            pw.println();
        }
    }

    /** Returns the MDX query string. */
    @Override
	public String toString() {
        resolve();
        return Util.unparse(this);
    }

    @Override
	public Object[] getChildren() {
        // Chidren are axes, slicer, and formulas (in that order, to be
        // consistent with replaceChild).
        List<QueryComponent> list = new ArrayList<>();
        list.addAll(Arrays.asList(axes));
        if (slicerAxis != null) {
            list.add(slicerAxis);
        }
        list.addAll(Arrays.asList(formulas));
        return list.toArray();
    }

    public QueryAxis getSlicerAxis() {
        return slicerAxis;
    }

    public void setSlicerAxis(QueryAxis axis) {
        this.slicerAxis = axis;
    }

    /**
     * Adds a level to an axis expression.
     */
    public void addLevelToAxis(AxisOrdinal axis, Level level) {
        if (axis == null) {
            throw new IllegalArgumentException("axis should not be null");
        }

        axes[axis.logicalOrdinal()].addLevel(level);
    }

    /**
     * Returns the hierarchies in an expression.
     *
     * If the expression's type is a dimension with several hierarchies,
     * assumes that the expression yields a member of the first (default)
     * hierarchy of the dimension.
     *
     * For example, the expression
     * Crossjoin(
     *   Hierarchize(
     *     Union(
     *       {[Time].LastSibling}, [Time].LastSibling.Children)),
     *       {[Measures].[Unit Sales], [Measures].[Store Cost]})
     *
     *
     * has type {[Time.Monthly], [Measures]} even though
     * [Time].LastSibling might return a member of either
     * [Time.Monthly] or [Time.Weekly].
     */
    private Hierarchy[] collectHierarchies(Expression queryPart) {
        Type exprType = queryPart.getType();
        if (exprType instanceof SetType setType) {
            exprType = setType.getElementType();
        }
        if (exprType instanceof TupleType tupleType) {
            final Type[] types = tupleType.elementTypes;
            ArrayList<Hierarchy> hierarchyList = new ArrayList<>();
            for (Type type : types) {
                hierarchyList.add(getTypeHierarchy(type));
            }
            return hierarchyList.toArray(new Hierarchy[hierarchyList.size()]);
        }
        return new Hierarchy[] {getTypeHierarchy(exprType)};
    }

    private Hierarchy getTypeHierarchy(final Type type) {
        Hierarchy hierarchy = type.getHierarchy();
        if (hierarchy != null) {
            return hierarchy;
        }
        final Dimension dimension = type.getDimension();
        if (dimension != null) {
            return dimension.getHierarchy();
        }
        return null;
    }

    /**
     * Assigns a value to the parameter with a given name.
     *
     * @throws RuntimeException if there is not parameter with the given name
     */
    @Override
    public void setParameter(final String parameterName, final Object value) {
        // Need to resolve query before we set parameters, in order to create
        // slots to store them in. (This code will go away when parameters
        // belong to prepared statements.)
        if (parameters.isEmpty()) {
            resolve();
        }

        final Parameter param =
            getCatalogReader(false).getParameter(parameterName);
        if (param == null) {
            throw new UnknownParameterException(parameterName);
        }
        if (!param.isModifiable()) {
            throw new ParameterIsNotModifiableException(
                parameterName, param.getScope().name());
        }
        final ExecutionImpl execution = new ExecutionImpl(statement,
            ExecuteDurationUtil.executeDurationValue(getConnection().getContext()));
        final Object value2 = ExecutionContext.where(execution.asContext(), () -> {
            return quickParse(parameterName, param.getType(), value, QueryImpl.this);
        });
        param.setValue(value2);
    }

    /**
     * Converts a value into something appropriate for a given type.
     *
     * Viz:
     *
     * For numerics, takes number or string and returns a {@link Number}.
     * For strings, takes string, or calls {@link Object#toString()} on any
     *     other type
     * For members, takes member or string
     * For sets of members, requires a list of members or strings and
     *     converts each element to a member.
     *
     *
     * @param type Type
     * @param value Value
     * @param query Query
     * @return Value of appropriate type
     * @throws NumberFormatException If value needs to be a number but isn't
     */
    private static Object quickParse(
        String parameterName,
        Type type,
        Object value,
        QueryImpl query)
        throws NumberFormatException
    {
    	DataType category = TypeUtil.typeToCategory(type);
        switch (category) {
        case NUMERIC:
            if (value instanceof Number || value == null) {
                return value;
            }
            if (value instanceof String s) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return Double.parseDouble(s);
                }
            }
            throw Util.newInternal(
                new StringBuilder("Invalid value '").append(value).append("' for parameter '").append(parameterName)
                    .append("', type ").append(type).toString());
        case STRING:
            if (value == null) {
                return null;
            }
            return value.toString();
        case SET:
            if (value instanceof String str) {
                value = IdentifierParser.parseIdentifierList(str);
            }
            if (!(value instanceof List)) {
                throw Util.newInternal(
                    new StringBuilder("Invalid value '").append(value).append("' for parameter '")
                        .append(parameterName).append("', type ").append(type).toString());
            }
            List<Member> expList = new ArrayList<>();
            final List<?> list = (List<?>) value;
            final SetType setType = (SetType) type;
            final Type elementType = setType.getElementType();
            for (Object o : list) {
                // In keeping with MDX semantics, null members are omitted from
                // lists.
                if (o == null) {
                    continue;
                }
                final Member member =
                    (Member) quickParse(parameterName, elementType, o, query);
                expList.add(member);
            }
            return expList;
        case MEMBER:
            if (value == null) {
                // Setting a member parameter to null is the same as setting to
                // the null member of the hierarchy. May not be equivalent to
                // the default value of the parameter, nor the same as the all
                // member.
                if (type.getHierarchy() != null) {
                    value = type.getHierarchy().getNullMember();
                } else if (type.getDimension() != null) {
                    value = type.getDimension().getHierarchy().getNullMember();
                }
            }
            if (value instanceof String str) {
                value = Util.parseIdentifier(str);
            }
            if (value instanceof List l
                && Util.canCast(l, Segment.class))
            {
                final List<Segment> segmentList = Util.cast(l);
                final OlapElement olapElement = Util.lookup(query, segmentList);
                if (olapElement instanceof Member) {
                    value = olapElement;
                }
            }
            if (value instanceof List l
                && Util.canCast(l, IdentifierSegment.class))
            {
                final List<IdentifierSegment> olap4jSegmentList =
                    Util.cast(l);
                final List<Segment> segmentList =
                    Util.convert(olap4jSegmentList);
                final OlapElement olapElement = Util.lookup(query, segmentList);
                if (olapElement instanceof Member) {
                    value = olapElement;
                }
            }
            if (value instanceof Member m && type.isInstance(value)) {
                return value;
            }
            throw Util.newInternal(
                new StringBuilder("Invalid value '").append(value).append("' for parameter '")
                    .append(parameterName).append("', type ").append(type).toString());
        default:
            throw new RuntimeException("Bad Category: "+category.getName());
        }
    }

    /**
     * Swaps the x- and y- axes.
     * Does nothing if the number of axes != 2.
     */
    public void swapAxes() {
        if (axes.length == 2) {
            Expression e0 = axes[0].getSet();
            boolean nonEmpty0 = axes[0].isNonEmpty();
            Expression e1 = axes[1].getSet();
            boolean nonEmpty1 = axes[1].isNonEmpty();
            axes[1].setSet(e0);
            axes[1].setNonEmpty(nonEmpty0);
            axes[0].setSet(e1);
            axes[0].setNonEmpty(nonEmpty1);
            // showSubtotals ???
        }
    }

    /**
     * Returns the parameters defined in this query.
     */
    public Parameter[] getParameters() {
        return parameters.toArray(new Parameter[parameters.size()]);
    }

    public Cube getCube() {
        return cube;
    }

    /**
     * Returns a schema reader.
     *
     * @param accessControlled If true, schema reader returns only elements
     * which are accessible to the statement's current role
     *
     * @return schema reader
     */
    public CatalogReader getCatalogReader(boolean accessControlled) {
        final Role role;
        if (accessControlled) {
            // full access control
            role = getConnection().getRole();
        } else {
            role = null;
        }
        final CatalogReader cubeCatalogReader = cube.getCatalogReader(role);
        return new QueryCatalogReader(cubeCatalogReader, QueryImpl.this);
    }

    /**
     * Looks up a member whose unique name is memberUniqueName
     * from cache. If the member is not in cache, returns null.
     */
    public Member lookupMemberFromCache(String memberUniqueName) {
        // first look in defined members
        for (Member member : getDefinedMembers()) {
            if (Util.equalName(member.getUniqueName(), memberUniqueName)
                || Util.equalName(
                    getUniqueNameWithoutAll(member),
                    memberUniqueName))
            {
                return member;
            }
        }
        return null;
    }

    private String getUniqueNameWithoutAll(Member member) {
        // build unique string
        Member parentMember = member.getParentMember();
        if ((parentMember != null) && !parentMember.isAll()) {
            return Util.makeFqName(
                getUniqueNameWithoutAll(parentMember),
                member.getName());
        } else {
            return Util.makeFqName(member.getHierarchy(), member.getName());
        }
    }

    /**
     * Looks up a named set.
     */
    private NamedSet lookupNamedSet(Segment segment) {
        if (!(segment instanceof NameSegment nameSegment)) {
            return null;
        }
        for (Formula formula : formulas) {
            if (!formula.isMember()
                && formula.getElement() != null
                && formula.getName().equals(nameSegment.getName()))
            {
                return (NamedSet) formula.getElement();
            }
        }
        return null;
    }

    /**
     * Creates a named set defined by an alias.
     */
    public ScopedNamedSet createScopedNamedSet(
        String name,
        QueryComponent scope,
        Expression expr)
    {
        final ScopedNamedSet scopedNamedSet =
            new ScopedNamedSet(
                name, scope, expr);
        scopedNamedSets.add(scopedNamedSet);
        return scopedNamedSet;
    }

    /**
     * Looks up a named set defined by an alias.
     *
     * @param nameParts Multi-part identifier for set
     * @param scopeList Parse tree node where name is used (last in list) and
     */
    ScopedNamedSet lookupScopedNamedSet(
        List<Segment> nameParts,
        ArrayStack<QueryComponent> scopeList)
    {
        if (nameParts.size() != 1) {
            return null;
        }
        if (!(nameParts.getFirst() instanceof NameSegment)) {
            return null;
        }
        String name = ((NameSegment) nameParts.getFirst()).getName();
        ScopedNamedSet bestScopedNamedSet = null;
        int bestScopeOrdinal = -1;
        for (ScopedNamedSet scopedNamedSet : scopedNamedSets) {
            if (Util.equalName(scopedNamedSet.name, name)) {
                int scopeOrdinal = scopeList.indexOf(scopedNamedSet.scope);
                if (scopeOrdinal > bestScopeOrdinal) {
                    bestScopedNamedSet = scopedNamedSet;
                    bestScopeOrdinal = scopeOrdinal;
                }
            }
        }
        return bestScopedNamedSet;
    }

    /**
     * Returns an array of the formulas used in this query.
     */
    public Formula[] getFormulas() {
        return formulas;
    }

    /**
     * Returns an array of this query's axes.
     */
    public QueryAxis[] getAxes() {
        return axes;
    }

    /**
     * Remove a formula from the query. If failIfUsedInQuery is
     * true, checks and throws an error if formula is used somewhere in the
     * query.
     */
    public void removeFormula(String uniqueName, boolean failIfUsedInQuery) {
        Formula formula = findFormula(uniqueName);
        if (failIfUsedInQuery && formula != null) {
            OlapElement mdxElement = formula.getElement();
            // search the query tree to see if this formula expression is used
            // anywhere (on the axes or in another formula)
            Walker walker = new Walker(this);
            while (walker.hasMoreElements()) {
                Object queryElement = walker.nextElement();
                if (!queryElement.equals(mdxElement)) {
                    continue;
                }
                // mdxElement is used in the query. lets find on on which axis
                // or formula
                String formulaType = formula.isMember()
                    ? calculatedMember
                    : calculatedSet;

                int i = 0;
                Object parent = walker.getAncestor(i);
                Object grandParent = walker.getAncestor(i + 1);
                while ((parent != null) && (grandParent != null)) {
                    if (grandParent instanceof Query) {
                        if (parent instanceof Axis) {
                            throw new OlapRuntimeException(MessageFormat.format(
                                mdxCalculatedFormulaUsedOnAxis,
                                    formulaType,
                                    uniqueName,
                                    ((QueryAxisImpl) parent).getAxisName()));

                        } else if (parent instanceof Formula form) {
                            String parentFormulaType =
                                form.isMember()
                                    ? calculatedMember
                                    : calculatedSet;
                            throw new OlapRuntimeException(MessageFormat.format(
                                mdxCalculatedFormulaUsedInFormula,
                                    formulaType, uniqueName, parentFormulaType,
                                    form.getUniqueName()));

                        } else {
                            throw new OlapRuntimeException(MessageFormat.format(
                                mdxCalculatedFormulaUsedOnSlicer,
                                    formulaType, uniqueName));
                        }
                    }
                    ++i;
                    parent = walker.getAncestor(i);
                    grandParent = walker.getAncestor(i + 1);
                }
                throw new OlapRuntimeException(MessageFormat.format(
                    mdxCalculatedFormulaUsedInQuery,
                        formulaType, uniqueName, Util.unparse(this)));
            }
        }

        // remove formula from query
        List<Formula> formulaList = new ArrayList<>();
        for (Formula formula1 : formulas) {
            if (!formula1.getUniqueName().equalsIgnoreCase(uniqueName)) {
                formulaList.add(formula1);
            }
        }

        // it has been found and removed
        this.formulas = formulaList.toArray(new Formula[formulaList.size()]);
    }

    /**
     * Returns whether a formula can safely be removed from the query. It can be
     * removed if the member or set it defines it not used anywhere else in the
     * query, including in another formula.
     *
     * @param uniqueName Unique name of the member or set defined by the formula
     * @return whether the formula can safely be removed
     */
    public boolean canRemoveFormula(String uniqueName) {
        Formula formula = findFormula(uniqueName);
        if (formula == null) {
            return false;
        }

        OlapElement mdxElement = formula.getElement();
        // Search the query tree to see if this formula expression is used
        // anywhere (on the axes or in another formula).
        Walker walker = new Walker(this);
        while (walker.hasMoreElements()) {
            Object queryElement = walker.nextElement();
            if (queryElement instanceof MemberExpression memberExpr
                && memberExpr.getMember().equals(mdxElement))
            {
                return false;
            }
            if (queryElement instanceof NamedSetExpression namedSetExpr
                && namedSetExpr.getNamedSet().equals(
                    mdxElement))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Looks up a calculated member or set defined in this Query.
     *
     * @param uniqueName Unique name of calculated member or set
     * @return formula defining calculated member, or null if not found
     */
    @Override
    public Formula findFormula(String uniqueName) {
        for (Formula formula : formulas) {
            if (formula.getUniqueName().equalsIgnoreCase(uniqueName)) {
                return formula;
            }
        }
        return null;
    }

    /**
     * Finds formula by name and renames it to new name.
     */
    public void renameFormula(String uniqueName, String newName) {
        Formula formula = findFormula(uniqueName);
        if (formula == null) {
            throw new OlapRuntimeException(MessageFormat.format( mdxFormulaNotFound,
                "formula", uniqueName, Util.unparse(this)));
        }
        formula.rename(newName);
    }

    List<Member> getDefinedMembers() {
        List<Member> definedMembers = new ArrayList<>();
        for (final Formula formula : formulas) {
            if (formula.isMember()
                && formula.getElement() != null
                && getConnection().getRole().canAccess(formula.getElement()))
            {
                definedMembers.add((Member) formula.getElement());
            }
        }
        return definedMembers;
    }

    /**
     * Finds axis by index and sets flag to show empty cells on that axis.
     */
    public void setAxisShowEmptyCells(int axis, boolean showEmpty) {
        if (axis >= axes.length) {
            throw new MdxAxisShowSubtotalsNotSupportedException(
                axis);
        }
        axes[axis].setNonEmpty(!showEmpty);
    }

    /**
     * Returns Hierarchy[] used on axis. It calls
     * {@link #collectHierarchies}.
     */
    @Override
    public Hierarchy[] getMdxHierarchiesOnAxis(AxisOrdinal axis) {
        if (axis.logicalOrdinal() >= axes.length) {
            throw new MdxAxisShowSubtotalsNotSupportedException(
                axis.logicalOrdinal());
        }
        QueryAxis queryAxis =
            axis.isFilter()
            ? slicerAxis
            : axes[axis.logicalOrdinal()];
        return collectHierarchies(queryAxis.getSet());
    }

    public Hierarchy[] getMdxHierarchiesOnAxis(QueryAxisImpl axis) {
        if(axis == null) {
            return new Hierarchy[0];
        }
        return collectHierarchies(axis.getSet());
    }

    /**
     * Compiles an expression, using a cached compiled expression if available.
     *
     * @param exp Expression
     * @param scalar Whether expression is scalar
     * @param resultStyle Preferred result style; if null, use query's default
     *     result style; ignored if expression is scalar
     * @return compiled expression
     */
    public Calc compileExpression(
        Expression exp,
        boolean scalar,
        ResultStyle resultStyle)
    {
        // REVIEW: Set query on a connection's shared internal statement is
        // not re-entrant.
        statement.setQuery(this);
        Evaluator evaluator = this.getConnection().getContext().createEvaluator(statement);
        final Validator validator = createValidator();
        List<ResultStyle> resultStyleList;
        resultStyleList =
            Collections.singletonList(
                resultStyle != null ? resultStyle : this.resultStyle);
        final ExpressionCompiler compiler =
            createCompiler(
                evaluator, validator, resultStyleList);
        if (scalar) {
            return compiler.compileScalar(exp, false);
        } else {
            return compiler.compile(exp);
        }
    }

    public ExpressionCompiler createCompiler() {
        // REVIEW: Set query on a connection's shared internal statement is
        // not re-entrant.
        statement.setQuery(this);
        Evaluator evaluator = this.getConnection().getContext().createEvaluator(statement);
        Validator validator = createValidator();
        return createCompiler(
            evaluator,
            validator,
            Collections.singletonList(resultStyle));
    }

    private ExpressionCompiler createCompiler(
        final Evaluator evaluator,
        final Validator validator,
        List<ResultStyle> resultStyleList)
    {

		ExpressionCompilerFactory factory = statement.getDaanseConnection().getContext()
				.getExpressionCompilerFactory();
		ExpressionCompiler compiler = factory.createExpressionCompiler(evaluator, validator, resultStyleList);

        final int expDeps =
            statement.getQuery().getConnection().getContext().getConfig().testExpDependencies();
        final ProfileHandler profileHandler = statement.getProfileHandler();
        if (profileHandler != null) {
            // Cannot test dependencies and profile at the same time. Profiling
            // trumps.
            compiler = getConnection().getContext().createProfilingCompiler(compiler);
        } else if (expDeps > 0) {
            compiler = getConnection().getContext().createDependencyTestingCompiler(compiler);
        }
        return compiler;
    }

    /**
     * Keeps track of references to members of the measures dimension
     *
     * @param olapElement potential measure member
     */
    public void addMeasuresMembers(OlapElement olapElement)
    {
        if (olapElement instanceof Member member && member.isMeasure()) {
            measuresMembers.add(member);
        }
    }

    /**
     * @return set of members from the measures dimension referenced within
     * this query
     */
    public Set<Member> getMeasuresMembers() {
        return Collections.unmodifiableSet(measuresMembers);
    }

    /**
     * Indicates that the query cannot use native cross joins to process
     * this virtual cube
     */
    public void setVirtualCubeNonNativeCrossJoin() {
        nativeCrossJoinVirtualCube = false;
    }

    /**
     * @return true if the query can use native cross joins on a virtual
     * cube
     */
    public boolean nativeCrossJoinVirtualCube() {
        return nativeCrossJoinVirtualCube;
    }

    /**
     * Saves away the base cubes related to the virtual cube
     * referenced in this query
     *
     * @param baseCubes set of base cubes
     */
    public void setBaseCubes(List<Cube> baseCubes) {
        this.baseCubes = baseCubes;
    }

    /**
     * return the set of base cubes associated with the virtual cube referenced
     * in this query
     *
     * @return set of base cubes
     */
    public List<Cube> getBaseCubes() {
        return baseCubes;
    }

    @Override
    public Object accept(QueryComponentVisitor visitor) {
        Object o = visitor.visitQuery(this);

        if (visitor.visitChildren()) {
            // visit formulas
            for (Formula formula : formulas) {
                formula.accept(visitor);
            }
            // visit axes
            for (QueryAxis axis : axes) {
                axis.accept(visitor);
            }
            if (slicerAxis != null) {
                slicerAxis.accept(visitor);
            }
        }
        return o;
    }

    /**
     * Put an Object value into the evaluation cache with given key.
     * This is used by Calc's to store information between iterations
     * (rather than re-generate each time).
     *
     * @param key the cache key
     * @param value the cache value
     */
    public void putEvalCache(String key, Object value) {
        evalCache.put(key, value);
    }

    /**
     * Gets the Object associated with the value.
     *
     * @param key the cache key
     * @return the cached value or null.
     */
    public Object getEvalCache(String key) {
        return evalCache.get(key);
    }

    /**
     * Remove all entries in the evaluation cache
     */
    public void clearEvalCache() {
        evalCache.clear();
    }

    public Statement getStatement() {
        return statement;
    }

    /**
     * Sets that the query owns its statement; therefore it will need to
     * close it when the query is closed.
     *
     * @param ownStatement Whether the statement belongs to the query
     */
    public void setOwnStatement(boolean ownStatement) {
        this.ownStatement = ownStatement;
    }

    /**
     * Source of metadata within the scope of a query.
     *
     * Note especially that {@link #getCalculatedMember(java.util.List)}
     * returns the calculated members defined in this query. It does not
     * perform access control; all calculated members defined in a query are
     * visible to everyone.
     */
    private static class QueryCatalogReader
        extends DelegatingCatalogReader
        implements NameResolver.Namespace
    {
        private final QueryImpl query;

        public QueryCatalogReader(CatalogReader cubeCatalogReader, QueryImpl query) {
            super(cubeCatalogReader);
            this.query = query;
        }

        @Override
		public CatalogReader withoutAccessControl() {
            return new QueryCatalogReader(
                schemaReader.withoutAccessControl(), query);
        }

        @Override
		public Member getMemberByUniqueName(
            List<Segment> uniqueNameParts,
            boolean failIfNotFound,
            MatchType matchType)
        {
            final String uniqueName = Util.implode(uniqueNameParts);
            Member member = query.lookupMemberFromCache(uniqueName);
            if (member == null) {
                // Not a calculated member in the query, so go to the cube.
                member = schemaReader.getMemberByUniqueName(
                    uniqueNameParts, failIfNotFound, matchType);
            }
            if (!failIfNotFound && member == null) {
                return null;
            }
            if (getRole().canAccess(member)) {
                return member;
            } else {
                return null;
            }
        }

        @Override
        public List<Member> getLevelMembers(
                Level level,
                boolean includeCalculated)
        {
            return getLevelMembers(level, includeCalculated, null);
        }

        @Override
        public List<Member> getLevelMembers(
                Level level,
                Evaluator context)
        {
            return getLevelMembers(level, false, context);
        }

        @Override
		public List<Member> getLevelMembers(
            Level level,
            boolean includeCalculated,
            Evaluator context)
        {
            List<Member> members = super.getLevelMembers(level, false, context);
            if (includeCalculated) {
                members = Util.addLevelCalculatedMembers(this, level, members);
            }

            Hierarchy hierarchy = level.getHierarchy();
            if(query.subcubeHierarchies.containsKey(hierarchy)) {
                ArrayList<Member> newMembers = new ArrayList<>();
                HashMap<Member, Member> subcubeMembers = query.subcubeHierarchies.get(hierarchy);
                for (int i = 0; i < members.size(); i++) {
                    Member sourceMember = members.get(i);
                    if(subcubeMembers.containsKey(sourceMember)) {
                        newMembers.add(subcubeMembers.get(sourceMember));
                    }
                }
                members = newMembers;
            }

            return members;
        }

        @Override
		public List<Member> getMemberChildren(Member member) {
            // The member itself, not the stand-in a role put in front of it.
            Member unlimited = query.withoutRoleLimit(member);
            return query.getSubcubeMembers(super.getMemberChildren(unlimited), false);
        }

        @Override
		public List<Member> getMemberChildren(List<Member> members) {
            // The member itself, not the stand-in a role put in front of it.
            List<Member> unlimitedMembers = query.withoutRoleLimit(members);
            return query.getSubcubeMembers(super.getMemberChildren(unlimitedMembers), false);
        }

        @Override
		public List<Member> getMemberChildren(Member member, Evaluator context) {
            // The member itself, not the stand-in a role put in front of it.
            Member unlimited = query.withoutRoleLimit(member);
            return query.getSubcubeMembers(super.getMemberChildren(unlimited, context), false);
        }

        @Override
		public List<Member> getMemberChildren(
                List<Member> members, Evaluator context)
        {
            // The member itself, not the stand-in a role put in front of it.
            List<Member> unlimitedMembers = query.withoutRoleLimit(members);
            return query.getSubcubeMembers(super.getMemberChildren(unlimitedMembers, context), false);
        }

        @Override
		public Map<? extends Member, AccessMember> getMemberChildrenWithDetails(
                Member member,
                Evaluator evaluator)
        {
            // The member itself, not the stand-in a role put in front of it.
            Member unlimited = query.withoutRoleLimit(member);
            Map<Member, AccessMember> sourceMembers = (Map<Member, AccessMember>)super.getMemberChildrenWithDetails(unlimited, evaluator);
            HashMap<Member, AccessMember> newMembers = new HashMap<>();
            for(Map.Entry<Member, AccessMember> entry : sourceMembers.entrySet()) {
                Member subcubeMember = query.getSubcubeMember(entry.getKey(), false);
                if(subcubeMember != null) {
                    newMembers.put(subcubeMember, entry.getValue());
                }
            }

            return newMembers;
        }

        @Override
		public Member getCalculatedMember(List<Segment> nameParts) {
            for (final Formula formula : query.formulas) {
                if (!formula.isMember()) {
                    continue;
                }
                Member member = (Member) formula.getElement();
                if (member == null) {
                    continue;
                }
                if (!Util.matches(member, nameParts)) {
                    continue;
                }
                if (!query.getConnection().getRole().canAccess(member)) {
                    continue;
                }
                return member;
            }
            return null;
        }



        @Override
		public List<Member> getCalculatedMembers(Hierarchy hierarchy) {
            List<Member> result = new ArrayList<>();
            // Add calculated members in the cube.
            final List<Member> calculatedMembers =
                super.getCalculatedMembers(hierarchy);
            result.addAll(calculatedMembers);
            // Add calculated members defined in the query.
            for (Member member : query.getDefinedMembers()) {
                if (member.getHierarchy().equals(hierarchy)) {
                    result.add(member);
                }
            }
            return result;
        }

        @Override
		public List<Member> getCalculatedMembers(Level level) {
            List<Member> hierarchyMembers =
                getCalculatedMembers(level.getHierarchy());
            List<Member> result = new ArrayList<>();
            for (Member member : hierarchyMembers) {
                if (member.getLevel().equals(level)) {
                    result.add(member);
                }
            }
            return result;
        }

        @Override
		public List<Member> getCalculatedMembers() {
            return query.getDefinedMembers();
        }

        @Override
		public OlapElement getElementChild(OlapElement parent, Segment s)
        {
            return getElementChild(parent, s, MatchType.EXACT);
        }

        @Override
		public OlapElement getElementChild(
            OlapElement parent,
            Segment s,
            MatchType matchType)
        {
            // first look in cube
            OlapElement mdxElement =
                schemaReader.getElementChild(parent, s, matchType);
            if (mdxElement != null) {
                return mdxElement;
            }
            // then look in defined members (fixes MONDRIAN-77)

            // then in defined sets
            if (!(s instanceof NameSegment)) {
                return null;
            }
            String name = ((NameSegment) s).getName();
            for (Formula formula : query.formulas) {
                if (formula.isMember()) {
                    continue;       // have already done these
                }
                Id id = formula.getIdentifier();
                if (id.getSegments().size() == 1
                    && id.getSegments().get(0).matches(name))
                {
                    return formula.getNamedSet();
                }
            }

            return mdxElement;
        }

        @Override
        public OlapElement lookupCompoundInternal(
            OlapElement parent,
            List<Segment> names,
            boolean failIfNotFound,
            DataType category,
            MatchType matchType)
        {
            if (matchType == MatchType.EXACT) {
                OlapElement oe = lookupCompound(
                    parent, names, failIfNotFound, category,
                    MatchType.EXACT_SCHEMA);
                if (oe != null) {
                    return oe;
                }
            }
            // First look to ourselves.
            if ((DataType.UNKNOWN == category || DataType.MEMBER == category) && parent == query.cube) {
                final Member calculatedMember = getCalculatedMember(names);
                if (calculatedMember != null) {
                    return calculatedMember;
                }
            }
            if ((DataType.UNKNOWN == category || DataType.SET == category) && parent == query.cube) {
                final NamedSet namedSet = getNamedSet(names);
                if (namedSet != null) {
                    return namedSet;
                }
            }
            // Then delegate to the next reader.
            OlapElement olapElement = super.lookupCompoundInternal(
                parent, names, failIfNotFound, category, matchType);
            if (olapElement instanceof Member member) {
                final Formula formula = (Formula)
                    member.getPropertyValue(StandardProperty.FORMULA.getName());
                if (formula != null) {
                    // This is a calculated member defined against the cube.
                    // Create a free-standing formula using the same
                    // expression, then use the member defined in that formula.
                    final Formula formulaClone = new FormulaImpl(formula);
                    formulaClone.createElement(query);
                    formulaClone.accept(query.createValidator());
                    olapElement = formulaClone.getMdxMember();
                }
            }
            return olapElement;
        }

        @Override
		public NamedSet getNamedSet(List<Segment> nameParts) {
            if (nameParts.size() != 1) {
                return null;
            }
            return query.lookupNamedSet(nameParts.get(0));
        }

        @Override
		public Parameter getParameter(String name) {
            // Look for a parameter defined in the query.
            for (Parameter parameter : query.parameters) {
                if (parameter.getName().equals(name)) {
                    return parameter;
                }
            }

            return super.getParameter(name);
        }

        @Override
		public OlapElement lookupChild(
            OlapElement parent,
            IdentifierSegment segment,
            MatchType matchType)
        {
            // ignore matchType
            return lookupChild(parent, segment);
        }

        @Override
		public OlapElement lookupChild(
            OlapElement parent,
            IdentifierSegment segment)
        {
            // Only look for calculated members and named sets defined in the
            // query.
            for (Formula formula : query.getFormulas()) {
                if (NameResolverImpl.matches(formula, parent, segment)) {
                    return formula.getElement();
                }
            }

            // The member itself, not the stand-in a role put in front of it.
            OlapElement parentOlapElement = parent;
            if(parent instanceof KeyMember keyed) {
                parentOlapElement = query.withoutRoleLimit(keyed);
            }
            OlapElement child = null;
            for (NameResolver.Namespace namespace : this.getNamespaces()) {
                if(namespace != this) {
                    child = namespace.lookupChild(parentOlapElement, segment);
                    if (child != null) {
                        break;
                    }
                }
            }

            if(child instanceof KeyMember keyed) {
                return query.getSubcubeMember(keyed, true);
            }

            return null;
        }

        @Override
		public Member getHierarchyDefaultMember(Hierarchy hierarchy) {
            Member member = super.getHierarchyDefaultMember(hierarchy);
            return query.getSubcubeMember(member, true);
        }

        @Override
		public List<NameResolver.Namespace> getNamespaces() {
            final List<NameResolver.Namespace> list =
                new ArrayList<>();
            list.add(this);
            list.addAll(super.getNamespaces());
            return list;
        }
    }



    /**
     * Implementation of {@link org.eclipse.daanse.olap.api.Validator} that works within a
     * particular query.
     *
     * It's unlikely that we would want a validator that is
     * NOT within a particular query, but by organizing the code this way, with
     * the majority of the code in {@link org.eclipse.daanse.olap.common.ValidatorImpl}, the
     * dependencies between Validator and Query are explicit.
     */
    private static class QueryValidator extends ValidatorImpl {
        private final boolean alwaysResolveFunDef;
        private QueryImpl query;
        private final CatalogReader schemaReader;

        /**
         * Creates a QueryValidator.
         *
         * @param functionTable Function table
         * @param alwaysResolveFunDef Whether to always resolve function
         *     definitions (see {@link #alwaysResolveFunDef()})
         * @param query Query
         */
        public QueryValidator(
        	FunctionService functionTable, boolean alwaysResolveFunDef, QueryImpl query,
            Map<QueryComponent, QueryComponent> resolvedIdentifiers)
        {
            super(functionTable, resolvedIdentifiers);
            this.alwaysResolveFunDef = alwaysResolveFunDef;
            this.query = query;
            this.schemaReader = new ScopedCatalogReader(this, true);
        }

        @Override
		public CatalogReader getCatalogReader() {
            return schemaReader;
        }

        @Override
		protected void defineParameter(Parameter param) {
            final String name = param.getName();
            query.parameters.add(param);
            query.parametersByName.put(name, param);
        }

        @Override
		public QueryImpl getQuery() {
            return query;
        }

        @Override
		public boolean alwaysResolveFunDef() {
            return alwaysResolveFunDef;
        }

        public ArrayStack<QueryComponent> getScopeStack() {
            return stack;
        }
    }

    /**
     * Catalog reader that depends on the current scope during the validation
     * of a query. Depending on the scope, different calculated sets may be
     * visible. The scope is represented by the expression stack inside the
     * validator.
     */
    private static class ScopedCatalogReader
        extends DelegatingCatalogReader
        implements NameResolver.Namespace
    {
        private final QueryValidator queryValidator;
        private final boolean accessControlled;

        /**
         * Creates a ScopedCatalogReader.
         *
         * @param queryValidator Validator that is being used to validate the
         *     query
         * @param accessControlled Access controlled
         */
        private ScopedCatalogReader(
            QueryValidator queryValidator,
            boolean accessControlled)
        {
            super(queryValidator.getQuery().getCatalogReader(accessControlled));
            this.queryValidator = queryValidator;
            this.accessControlled = accessControlled;
        }

        @Override
		public CatalogReader withoutAccessControl() {
            if (!accessControlled) {
                return this;
            }
            return new ScopedCatalogReader(queryValidator, false);
        }

        @Override
		public List<NameResolver.Namespace> getNamespaces() {
            final List<NameResolver.Namespace> list =
                new ArrayList<>();
            list.add(this);
            list.addAll(super.getNamespaces());
            return list;
        }

        @Override
        public OlapElement lookupCompoundInternal(
            OlapElement parent,
            final List<Segment> names,
            boolean failIfNotFound,
            DataType category,
            MatchType matchType)
        {
            if ( DataType.SET == category || DataType.UNKNOWN == category ) {
                final ScopedNamedSet namedSet =
                    queryValidator.getQuery().lookupScopedNamedSet(
                        names, queryValidator.getScopeStack());
                if (namedSet != null) {
                    return namedSet;
                }
            }
            return super.lookupCompoundInternal(
                parent, names, failIfNotFound, category, matchType);
        }

        @Override
		public OlapElement lookupChild(
            OlapElement parent,
            IdentifierSegment segment,
            MatchType matchType)
        {
            // ignore matchType
            return lookupChild(parent, segment);
        }

        @Override
		public OlapElement lookupChild(
            OlapElement parent,
            IdentifierSegment segment)
        {
            if (!(parent instanceof Cube)) {
                return null;
            }
            return queryValidator.getQuery().lookupScopedNamedSet(
                Collections.singletonList(Util.convert(segment)),
                queryValidator.getScopeStack());
        }
    }

    public static class ScopedNamedSet implements NamedSet {
        private final String name;
        private final QueryComponent scope;
        private Expression expr;

        /**
         * Creates a ScopedNamedSet.
         *
         * @param name Name
         * @param scope Scope of named set (the function call that encloses
         *     the 'expr AS name', often GENERATE or FILTER)
         * @param expr Expression that defines the set
         */
        private ScopedNamedSet(String name, QueryComponent scope, Expression expr) {
            this.name = name;
            this.scope = scope;
            this.expr = expr;
        }

        @Override
		public String getName() {
            return name;
        }

        @Override
		public String getNameUniqueWithinQuery() {
            return new StringBuilder().append(System.identityHashCode(this)).toString();
        }

        @Override
		public boolean isDynamic() {
            return true;
        }

        @Override
        public List<Hierarchy> getHierarchies() {
            return List.of();
        }

        @Override
        public String getDisplayFolder() {
            return null;
        }

        @Override
		public Expression getExp() {
            return expr;
        }

        public void setExp(Expression expr) {
            this.expr = expr;
        }

        @Override
		public void setName(String newName) {
            throw new UnsupportedOperationException();
        }

        @Override
		public Type getType() {
            return expr.getType();
        }

        @Override
		public MetaData getMetaData()  {
            return OlapMetaDataBase.empty();
        }

        @Override
		public NamedSet validate(Validator validator) {
            Expression newExpr = expr.accept(validator);
            final Type type = newExpr.getType();
            if (type instanceof MemberType
                || type instanceof TupleType)
            {
                newExpr =
                    new UnresolvedFunCallImpl(
                        new BracesOperationAtom(), new Expression[] {newExpr})
                    .accept(validator);
            }
            this.expr = newExpr;
            return this;
        }

        @Override
        @SuppressWarnings("java:S4144")
		public String getUniqueName() {
            return name;
        }

        @Override
		public String getDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
		public OlapElement lookupChild(
                CatalogReader schemaReader, Segment s, MatchType matchType)
        {
            throw new UnsupportedOperationException();
        }

        @Override
		public String getQualifiedName() {
            throw new UnsupportedOperationException();
        }

        @Override
		public String getCaption() {
            throw new UnsupportedOperationException();
        }

        @Override
		public boolean isVisible() {
            throw new UnsupportedOperationException();
        }

        @Override
		public Hierarchy getHierarchy() {
            throw new UnsupportedOperationException();
        }

        @Override
		public Dimension getDimension() {
            throw new UnsupportedOperationException();
        }

        @Override
		public String getLocalized(LocalizedProperty prop, Locale locale) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Visitor that locates and registers parameters.
     */
    private class ParameterFinder extends MdxVisitorImpl {
        @Override
		public Object visitParameterExpression(ParameterExpression parameterExpr) {
            Parameter parameter = parameterExpr.getParameter();
            if (!parameters.contains(parameter)) {
                parameters.add(parameter);
                parametersByName.put(parameter.getName(), parameter);
            }
            return null;
        }

        @Override
		public Object visitUnresolvedFunCall(UnresolvedFunCall call) {
            if (call.getOperationAtom().name().equals("Parameter")) {
                // Is there already a parameter with this name?
                String parameterName =
                    ParameterExpressions.getParameterName(call.getArgs());
                if (parametersByName.get(parameterName) != null) {
                    throw new OlapRuntimeException(MessageFormat.format(
                        parameterDefinedMoreThanOnce, parameterName));
                }

                Type type =
                    ParameterExpressions.getParameterType(call.getArgs());

                // Create a temporary parameter. We don't know its
                // type yet. The default of NULL is temporary.
                Parameter parameter = new ParameterImpl(
                    parameterName, NullLiteralImpl.nullValue, null, type);
                parameters.add(parameter);
                parametersByName.put(parameterName, parameter);
            }
            return null;
        }
    }

    /**
     * Visitor that locates and registers all aliased expressions
     * ('expr AS alias') as named sets. The resulting named sets have scope,
     * therefore they can only be seen and used within that scope.
     */
    private class AliasedExpressionFinder extends MdxVisitorImpl {
        @Override
        public Object visitQueryAxis(QueryAxis queryAxis) {
            registerAlias(queryAxis, queryAxis.getSet());
            return super.visitQueryAxis(queryAxis);
        }

        @Override
		public Object visitUnresolvedFunCall(UnresolvedFunCall call) {
            registerAliasArgs(call);
            return super.visitUnresolvedFunCall(call);
        }

        @Override
		public Object visitResolvedFunCall(ResolvedFunCall call) {
            registerAliasArgs(call);
            return super.visitResolvedFunCall(call);
        }

        /**
         * Registers all arguments of a function that are named sets.
         *
         * @param call Function call
         */
        private void registerAliasArgs(FunctionCall call) {
            for (Expression exp : call.getArgs()) {
                registerAlias((QueryComponent) call, exp);
            }
        }

        /**
         * Registers a named set if an expression is of the form "expr AS
         * alias".
         *
         * @param parent Parent node
         * @param exp Expression that may be an "AS"
         */
        private void registerAlias(QueryComponent parent, Expression exp) {
            if (exp instanceof FunctionCall call2 && call2.getOperationAtom() instanceof InfixOperationAtom
                && call2.getOperationAtom().name().equals("AS")) {
                // Scope is the function enclosing the 'AS' expression.
                // For example, in
                //    Filter(Time.Children AS s, x > y)
                // the scope of the set 's' is the Filter function.
                assert call2.getArgCount() == 2;
                if (call2.getArg(1) instanceof Id id) {
                    createScopedNamedSet(
                        ((NameSegment) id.getSegments().get(0))
                            .getName(),
                        parent,
                        call2.getArg(0));
                } else if (call2.getArg(1) instanceof NamedSetExpression set) {
                    createScopedNamedSet(
                        set.getNamedSet().getName(),
                        parent,
                        call2.getArg(0));
                }
            }
        }
    }

    public void replaceSubcubeMembers() {
        for(QueryAxis queryAxis: this.axes) {
            Expression exp = queryAxis.getSet();
            queryAxis.setSet(replaceSubcubeMember(exp));
        }
        if(this.slicerAxis != null) {
            Expression exp = this.slicerAxis.getSet();
            this.slicerAxis.setSet(replaceSubcubeMember(exp));
        }
        for(Formula formula: this.formulas) {
            Expression exp = formula.getExpression();
            if(exp != null){
                formula.setExpression(replaceSubcubeMember(exp));
            }
        }
    }

    @Override
    public Map<Hierarchy, Calc> getSubcubeHierarchyCalcs() {
        return subcubeHierarchyCalcs;
    }

    @Override
    public Calc getSlicerCalc() {
        return slicerCalc;
    }

    @Override
    public Calc[] getAxisCalcs() {
        return axisCalcs;
    }

    @Override
    public void setSubcubeHierarchies(HashMap<Hierarchy, HashMap<Member, Member>> subcubeHierarchies) {
        this.subcubeHierarchies =  subcubeHierarchies;
    }

    private List<Member> getSubcubeMembers(List<Member> members, boolean addNullMember) {
        ArrayList<Member> newMembers = new ArrayList<>();
        for(Member sourceMember: members) {
            Member subcubeMember = this.getSubcubeMember(sourceMember, addNullMember);
            if(subcubeMember != null) {
                newMembers.add(subcubeMember);
            }
        }
        return newMembers;
    }

    private List<Member> withoutRoleLimit(List<Member> members) {
        ArrayList<Member> newMembers = new ArrayList<>();
        for(Member sourceMember: members) {
            newMembers.add(this.withoutRoleLimit(sourceMember));
        }
        return newMembers;
    }

    private Member withoutRoleLimit(Member member) {
        if(member == null || !(member instanceof LimitedMember)) {
            return member;
        }
        else {
            return ((LimitedMember)member).getMember();
        }
    }

    private Member getSubcubeMember(Member member, boolean addNullMember) {
        Hierarchy hierarchy = member.getHierarchy();
        if(this.subcubeHierarchies.containsKey(hierarchy)) {
            HashMap<Member, Member> subcubeMembers = this.subcubeHierarchies.get(hierarchy);
            if(subcubeMembers.containsKey(member)) {
                return subcubeMembers.get(member);
            }
            else if(addNullMember) {
                return hierarchy.getNullMember();
            }
            return null;
        }
        return member;
    }

    private Expression replaceSubcubeMember(Expression exp) {
        if(exp instanceof MemberExpression memberExpr) {
            Member subcubeMember = this.getSubcubeMember(memberExpr.getMember(), true);
            return new MemberExpressionImpl(subcubeMember);
        }
        if(exp instanceof ResolvedFunCall resolvedFunCall) {
            for (int i = 0; i < resolvedFunCall.getArgs().length; i++) {
                resolvedFunCall.getArgs()[i] = replaceSubcubeMember(resolvedFunCall.getArgs()[i]);
            }
        }
        return exp;
    }
}
