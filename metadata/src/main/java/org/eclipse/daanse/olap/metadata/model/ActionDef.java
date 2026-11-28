/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.metadata.model;

import java.util.List;

/** Something a client offers on a cell or a member. */
public sealed interface ActionDef {

    String name();

    /**
     * Shows the rows behind a cell.
     *
     * @param isDefault whether a plain DRILLTHROUGH without a RETURN clause uses it
     * @param returns   MDX text naming the levels, properties and measures to return
     */
    record DrillThrough(String name, boolean isDefault, List<String> returns, ElementInfo info)
            implements ActionDef {

        public DrillThrough {
            // kept as given, a null list or entry included, so the model check names it with its path
            // instead of a NullPointerException here
            returns = returns == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(returns));
        }
    }
}
