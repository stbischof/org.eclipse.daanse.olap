/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.calc.base.aggregate;

import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;

/**
 * The value of a member a role shows only in part: the aggregate over the children the role may see,
 * and nothing else. Whoever asks the member gets what those children add up to, worked out cell by
 * cell.
 */
public class LimitedRollupCalc extends AggregateCalc {

    /**
     * @param returnType     the type the member answers with
     * @param visibleChildren the children the role may see, worked out when the cell is asked for
     */
    public LimitedRollupCalc( Type returnType, TupleListCalc visibleChildren ) {
        super( returnType, visibleChildren, new CurrentValueUnknownCalc( returnType ) );
    }
}
