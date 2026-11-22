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
package org.eclipse.daanse.olap.function.def.dimensions.string;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedHierarchyCalc;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.FunUtil;

public class DimensionsStringCalc extends AbstractNestedHierarchyCalc {

    protected DimensionsStringCalc(Type type, StringCalc stringCalc) {
        super(type, stringCalc);
    }

    @Override
    public Hierarchy evaluateInternal(Evaluator evaluator) {
        String dimensionName = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        return findHierarchy(dimensionName, evaluator);
    }

    /**
     * Looks up a hierarchy in the current cube with a given name.
     *
     * @param name      Hierarchy name
     * @param evaluator Evaluator
     * @return Hierarchy
     */
    private Hierarchy findHierarchy(String name, Evaluator evaluator) {
        if (name.indexOf("[") == -1) {
            name = Util.quoteMdxIdentifier(name);
        }
        OlapElement o = evaluator.getCatalogReader().lookupCompound(evaluator.getCube(), Util.parseIdentifier(name),
                false, DataType.HIERARCHY);
        if (o instanceof Hierarchy hierarchy) {
            return hierarchy;
        } else if (o == null) {
            throw FunUtil.newEvalException(DimensionsStringFunDef.functionMetaData,
                    new StringBuilder("Hierarchy '").append(name).append("' not found").toString());
        } else {
            throw FunUtil.newEvalException(DimensionsStringFunDef.functionMetaData,
                    new StringBuilder("Hierarchy(").append(name).append(") found ").append(o).toString());
        }
    }

}
