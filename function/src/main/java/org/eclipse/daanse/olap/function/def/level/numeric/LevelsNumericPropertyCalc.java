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
package org.eclipse.daanse.olap.function.def.level.numeric;

import java.util.List;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedLevelCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class LevelsNumericPropertyCalc extends AbstractNestedLevelCalc {

    protected LevelsNumericPropertyCalc(Type type, final HierarchyCalc hierarchyCalc, final IntegerCalc ordinalCalc) {
        super(type, hierarchyCalc, ordinalCalc);
    }

    @Override
    public Level evaluateInternal(Evaluator evaluator) {
        Hierarchy hierarchy = getChildCalc(0, HierarchyCalc.class).evaluate(evaluator);
        Integer ordinal = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        return nthLevel(hierarchy, ordinal);
    }

    private Level nthLevel(Hierarchy hierarchy, int n) {
        List<? extends Level> levels = hierarchy.getLevels();

        if (n >= levels.size() || n < 0) {
            throw FunUtil.newEvalException(LevelsNumericPropertyDef.levelsFunctionMetaData,
                    new StringBuilder("Index '").append(n).append("' out of bounds").toString());
        }
        return levels.get(n);
    }

}
