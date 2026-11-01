/*
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import org.eclipse.daanse.dmv.parser.api.DmvParserProvider;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.eclipse.daanse.olap.api.agg.AggregationFactory;
import org.eclipse.daanse.olap.api.aggregator.CustomAggregatorFactory;
import org.eclipse.daanse.olap.api.cache.CatalogCache;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.monitor.EventBus;
import org.eclipse.daanse.olap.api.result.ResultShepherd;

/**
 * The Context gives access to all resources and configurations that are needed
 * to calculate and Data Cubes
 *
 * @author stbischof
 *
 * @param <C> Connection
 *
 */
public interface Context<C extends Connection> {

    List<String> KEYWORD_LIST = List.of("$AdjustedProbability", "$Distance", "$Probability", "$ProbabilityStDev",
            "$ProbabilityStdDeV", "$ProbabilityVariance", "$StDev", "$StdDeV", "$Support", "$Variance",
            "AddCalculatedMembers", "Action", "After", "Aggregate", "All", "Alter", "Ancestor", "And", "Append", "As",
            "ASC", "Axis", "Automatic", "Back_Color", "BASC", "BDESC", "Before", "Before_And_After", "Before_And_Self",
            "Before_Self_After", "BottomCount", "BottomPercent", "BottomSum", "Break", "Boolean", "Cache", "Calculated",
            "Call", "Case", "Catalog_Name", "Cell", "Cell_Ordinal", "Cells", "Chapters", "Children",
            "Children_Cardinality", "ClosingPeriod", "Cluster", "ClusterDistance", "ClusterProbability", "Clusters",
            "CoalesceEmpty", "Column_Values", "Columns", "Content", "Contingent", "Continuous", "Correlation", "Cousin",
            "Covariance", "CovarianceN", "Create", "CreatePropertySet", "CrossJoin", "Cube", "Cube_Name",
            "CurrentMember", "CurrentCube", "Custom", "Cyclical", "DefaultMember", "Default_Member", "DESC",
            "Descendents", "Description", "Dimension", "Dimension_Unique_Name", "Dimensions", "Discrete", "Discretized",
            "DrillDownLevel", "DrillDownLevelBottom", "DrillDownLevelTop", "DrillDownMember", "DrillDownMemberBottom",
            "DrillDownMemberTop", "DrillTrough", "DrillUpLevel", "DrillUpMember", "Drop", "Else", "Empty", "End",
            "Equal_Areas", "Exclude_Null", "ExcludeEmpty", "Exclusive", "Expression", "Filter", "FirstChild",
            "FirstRowset", "FirstSibling", "Flattened", "Font_Flags", "Font_Name", "Font_size", "Fore_Color",
            "Format_String", "Formatted_Value", "Formula", "From", "Generate", "Global", "Head", "Hierarchize",
            "Hierarchy", "Hierary_Unique_name", "IIF", "IsEmpty", "Include_Null", "Include_Statistics", "Inclusive",
            "Input_Only", "IsDescendant", "Item", "Lag", "LastChild", "LastPeriods", "LastSibling", "Lead", "Level",
            "Level_Number", "Level_Unique_Name", "Levels", "LinRegIntercept", "LinRegR2", "LinRegPoint", "LinRegSlope",
            "LinRegVariance", "Long", "MaxRows", "Median", "Member", "Member_Caption", "Member_Guid", "Member_Name",
            "Member_Ordinal", "Member_Type", "Member_Unique_Name", "Members", "Microsoft_Clustering",
            "Microsoft_Decision_Trees", "Mining", "Model", "Model_Existence_Only", "Models", "Move", "MTD", "Name",
            "Nest", "NextMember", "Non", "NonEmpty", "Normal", "Not", "Ntext", "Nvarchar", "OLAP", "On",
            "OpeningPeriod", "OpenQuery", "Or", "Ordered", "Ordinal", "Pages", "ParallelPeriod", "Parent",
            "Parent_Level", "Parent_Unique_Name", "PeriodsToDate", "PMML", "Predict", "Predict_Only",
            "PredictAdjustedProbability", "PredictHistogram", "Prediction", "PredictionScore", "PredictProbability",
            "PredictProbabilityStDev", "PredictProbabilityVariance", "PredictStDev", "PredictSupport",
            "PredictVariance", "PrevMember", "Probability", "Probability_StDev", "Probability_StdDev",
            "Probability_Variance", "Properties", "Property", "QTD", "RangeMax", "RangeMid", "RangeMin", "Rank",
            "Recursive", "Refresh", "Related", "Rename", "Rollup", "Rows", "Schema_Name", "Sections", "Select", "Self",
            "Self_And_After", "Sequence_Time", "Server", "Session", "Set", "SetToArray", "SetToStr", "Shape", "Skip",
            "Solve_Order", "Sort", "StdDev", "Stdev", "StripCalculatedMembers", "StrToSet", "StrToTuple", "SubSet",
            "Support", "Tail", "Text", "Thresholds", "ToggleDrillState", "TopCount", "TopPercent", "TopSum",
            "TupleToStr", "Under", "Uniform", "UniqueName", "Use", "Value", "Var", "Variance", "VarP", "VarianceP",
            "VisualTotals", "When", "Where", "With", "WTD", "Xor");

    CatalogCache getCatalogCache();

    /**
     * Gives access to a QueryProvider.
     *
     * @return QueryProvider.
     */
//    QueryProvider getQueryProvider();
    /*
     * The human readable name of the Context. By default the name of the Schema.
     * May be overridden.
     */
    String getName();

    /*
     * The human readable description of the Context. By default the getDescription
     * of the Schema. May be overridden.
     */
    Optional<String> getDescription();

    List<String> getAccessRoles();

    ExpressionCompilerFactory getExpressionCompilerFactory();

    /*
     * Gives access to the Connection.
     */
    C getConnectionWithDefaultRole();

    C getConnection(ConnectionProps props);

    void addConnection(C rolapConnection);

    void removeConnection(C rolapConnection);

    ResultShepherd getResultShepherd();

//	AggregationManager getAggregationManager();

    void addStatement(Statement statement);

    void removeStatement(Statement internalStatement);

    EventBus getMonitor();

    List<Statement> getStatements(C connection);

    <T> T getConfigValue(String key, T dflt, Class<T> clazz);

    /**
     * The configuration of this context as a typed view.
     *
     * <p>
     * Prefer this over {@link #getConfigValue(String, Object, Class)}: the getters
     * name their setting, carry its documentation, and get their default and their
     * type right without the caller having to repeat either. The raw accessor
     * remains for the test contexts, which write into the underlying map.
     * </p>
     *
     * <p>
     * The returned view reads the configuration on every call, so a context whose
     * configuration is updated keeps answering with the current values through the
     * same instance.
     * </p>
     */
    ContextConfig getConfig();

    Semaphore getQueryLimitSemaphore();

    FunctionService getFunctionService();

    MdxParserProvider getMdxParserProvider();

    /**
     * The parser for DMV statements ({@code SELECT ... FROM $SYSTEM.<rowset>}) - a language
     * of its own, with its own parser service. Empty when no DMV parser is installed; a DMV
     * query is then refused with that reason instead of being guessed at by other parsers.
     */
    Optional<DmvParserProvider> getDmvParserProvider();

    default List<String> getKeywordList() {
        return KEYWORD_LIST;
    }

    AggregationFactory getAggragationFactory();

    Evaluator createEvaluator(Statement statement);

    /**
     * Creates a dummy evaluator.
     */
    Evaluator createDummyEvaluator(Statement statement);

    ExpressionCompiler createProfilingCompiler(ExpressionCompiler compiler);

    ExpressionCompiler createDependencyTestingCompiler(ExpressionCompiler compiler);

    List<CustomAggregatorFactory> getCustomAggregators();
}
