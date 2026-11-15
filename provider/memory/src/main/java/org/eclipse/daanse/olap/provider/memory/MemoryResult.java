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
package org.eclipse.daanse.olap.provider.memory;

import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellReader;
import org.eclipse.daanse.olap.result.CellInfo;
import org.eclipse.daanse.olap.result.ResultCell;
import org.eclipse.daanse.olap.result.ResultImpl;

/**
 * The result of a query against a {@link MemoryContext}.
 *
 * <p>The evaluation is the engine's own, {@link ResultImpl}: slicer and compound slicer, axes,
 * named sets, format strings and cell properties behave as they do for every other provider.
 * This class only says where the cells come from. The reader of the context answers every cell
 * at once and never defers one, so the engine's loop over pending loads ends after its first
 * pass.
 */
public class MemoryResult extends ResultImpl {

    MemoryResult(Execution execution, boolean execute) {
        super(execution, execute);
    }

    @Override
    protected CellReader createCellReader(Cube cube) {
        return ((MemoryContext) execution.getDaanseStatement().getDaanseConnection().getContext()).cellReader();
    }

    @Override
    protected Cell createCell(int[] pos, CellInfo ci) {
        return new ResultCell(this, pos, ci);
    }
}
