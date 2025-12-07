/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.udf.currentdatemember;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.MatchType;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.util.Format;

public class CurrentDateMemberCalc extends AbstractProfilingNestedCalc<Object> {

    private MatchType matchType;
    private HierarchyCalc hierarchyCalc;
    private StringCalc formatCalc;
    private Object resultDateMember;
    
    protected CurrentDateMemberCalc(Type type, final HierarchyCalc hierarchyCalc, final StringCalc formatCalc, MatchType matchType) {
        super(type);
        this.hierarchyCalc = hierarchyCalc;
        this.formatCalc = formatCalc;
        this.matchType = matchType;
    }

    @Override
    public Object evaluateInternal(Evaluator evaluator) {
        
        
        if (resultDateMember != null) {
            return resultDateMember;
        }

        // determine the current date
        String formatArg = formatCalc.evaluate(evaluator);

        final Locale locale = Locale.getDefault();
        final Format format = new Format((String) formatArg, locale);
        String currDateStr = format.format(getDate(evaluator));

        List<Segment> uniqueNames = Util.parseIdentifier(currDateStr);
        resultDateMember =
            evaluator.getCatalogReader().getMemberByUniqueName(
                uniqueNames, false, matchType);
        if (resultDateMember != null) {
            return resultDateMember;
        }

        // if there is no matching member, return the null member for
        // the specified dimension/hierarchy
        Object arg0 = hierarchyCalc.evaluate(evaluator);
        if (arg0 instanceof Hierarchy hier) {
            resultDateMember = hier.getNullMember();
        } else if (arg0 instanceof Dimension dim) {
            resultDateMember = dim.getHierarchy().getNullMember();
        }
        return resultDateMember;
    }
    
    /*
     * Package private function created for proper testing.
     * Returns Date for Format compatibility.
     */
    Date getDate(Evaluator evaluator) {
        LocalDateTime ldt = evaluator.getQueryStartTime();
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

}
