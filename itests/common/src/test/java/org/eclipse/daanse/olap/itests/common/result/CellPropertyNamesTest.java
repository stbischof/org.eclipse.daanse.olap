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
package org.eclipse.daanse.olap.itests.common.result;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.provider.api.Olap;
import org.eclipse.daanse.olap.provider.junit.World;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The name of a cell property is read the way the catalog asks for it: without regard for case unless
 * the catalog says otherwise, as ContextConfig#caseSensitive states for cell property lookup.
 *
 * <pre>
 * Store    Amount
 * Berlin      10
 * </pre>
 */
class CellPropertyNamesTest {

    private static final String ONE_CELL = "SELECT {[Measures].[Amount]} ON 0 FROM [Sales]";

    private static World shop() {
        return World.of(Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("Store", d -> d.hierarchy(h -> h.level("City")))
                        .measure("Amount", m -> m.formatString("#,##0.00")))
                .build(), store -> store.fact("Sales").at("Store", "Berlin").value("Amount", 10).add());
    }

    private static Cell theOneCell(World world) {
        Result result = world.connection().execute(world.connection().parseQuery(ONE_CELL));
        return result.getCell(new int[] { 0 });
    }

    @Test
    void aPropertyIsFoundWhateverTheCaseOfItsName() {
        try (World world = shop()) {
            Cell cell = theOneCell(world);

            assertThat(cell.getPropertyValue("FORMAT_STRING")).isEqualTo("#,##0.00");
            assertThat(cell.getPropertyValue("format_string")).isEqualTo("#,##0.00");
        }
    }

    @Disabled("The owner has to say whether a cell reads the settings of its catalog after the query"
            + " has run. Expected: a catalog that asks for the case is answered by the case, so"
            + " cell_ordinal finds nothing where CELL_ORDINAL finds the ordinal. Today: a cell asks"
            + " ExecutionConfig.current(), which answers with the defaults whenever no execution is"
            + " current - and a client asks a cell for its properties after the run. Both"
            + " implementations do this alike (the relational one asked the same way before this became"
            + " shared), so it is no divergence between them but a seam: either the result keeps the"
            + " settings it was computed under, or a cell says plainly that it reads the defaults.")
    @Test
    void aCatalogThatAsksForTheCaseIsAnsweredByTheCase() {
        try (World world = shop().config("caseSensitive", true)) {
            Cell cell = theOneCell(world);

            // The ordinal of a cell is a question only the cell answers, so the case decides here.
            // A format string would still be found, because the measure carries it as a member
            // property and the evaluator asks the member for a name the cell did not know.
            assertThat(cell.getPropertyValue("CELL_ORDINAL")).isEqualTo(0);
            assertThat(cell.getPropertyValue("cell_ordinal")).isNull();
        }
    }
}
