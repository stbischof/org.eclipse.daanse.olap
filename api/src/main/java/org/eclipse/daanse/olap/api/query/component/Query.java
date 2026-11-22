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
package org.eclipse.daanse.olap.api.query.component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;

public non-sealed interface Query extends QueryComponent {

    CatalogReader getCatalogReader(boolean b);

    Cube getCube();

    void setResultStyle(ResultStyle list);

    QueryAxis[] getAxes();

    Calc compileExpression(Expression exp, boolean scalar, ResultStyle resultStyle);

    Map<Hierarchy, Calc> getSubcubeHierarchyCalcs();

    void replaceSubcubeMembers();

    void resolve();

    void clearEvalCache();

    QueryAxis getSlicerAxis();

    QueryComponent[] getCellProperties();

    Set<Member> getMeasuresMembers();

    Calc getSlicerCalc();

    Calc[] getAxisCalcs();

    void setSubcubeHierarchies(HashMap<Hierarchy, HashMap<Member, Member>> subcubeHierarchies);

    void putEvalCache(String key, Object value);

    Object getEvalCache(String key);

    Formula[] getFormulas();

    Statement getStatement();

    Connection getConnection();

    void addFormulas(Formula[] toArray);

    Formula findFormula(String toString);

    Validator createValidator();

    Collection<Cube> getBaseCubes();

    void addMeasuresMembers(OlapElement olapElement);

    void setBaseCubes(List<Cube> baseCubeList);

    boolean nativeCrossJoinVirtualCube();

    boolean shouldAlertForNonNative(FunctionDefinition fun);

    ExpressionCompiler createCompiler();

    boolean hasCellProperty(String name);

    Parameter[] getParameters();

    ResultStyle getResultStyle();

    boolean ignoreInvalidMembers();

    boolean isCellPropertyEmpty();

    void setVirtualCubeNonNativeCrossJoin();

    Object accept(QueryComponentVisitor visitor);

    Hierarchy[] getMdxHierarchiesOnAxis(AxisOrdinal forLogicalOrdinal);

    void setParameter(String string, Object object);

    /**
     * The handler the profiling scopes chose for this query (context, cube, connection), or {@code null}. The
     * statement falls back to it when it has no handler of its own, so the choice holds for this query only.
     */
    default org.eclipse.daanse.olap.api.calc.profile.ProfileHandler getProfileHandler() {
        return null;
    }
}
