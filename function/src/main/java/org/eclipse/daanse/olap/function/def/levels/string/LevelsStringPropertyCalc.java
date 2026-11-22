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
package org.eclipse.daanse.olap.function.def.levels.string;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedLevelCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class LevelsStringPropertyCalc extends AbstractNestedLevelCalc {

    protected LevelsStringPropertyCalc(Type type, final HierarchyCalc hierarchyCalc, final StringCalc nameCalc) {
        super(type, hierarchyCalc, nameCalc);
    }

    @Override
    public Level evaluateInternal(Evaluator evaluator) {
        Hierarchy hierarchy = getChildCalc(0, HierarchyCalc.class).evaluate(evaluator);
        String name = getChildCalc(1, StringCalc.class).evaluate(evaluator);
        for (Level level : hierarchy.getLevels()) {
            if (level.getName().equals(name)) {
                return level;
            }
        }
        throw FunUtil.newEvalException(LevelsStringPropertyDef.hierarchyLevelsFunctionMetaData, new StringBuilder("Level '")
                .append(name).append("' not found in hierarchy '").append(hierarchy).append("'").toString());
    }

}
