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
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.xmla.bridge.execute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.result.CellSetAxis;
import org.eclipse.daanse.olap.api.result.CellSetAxisMetaData;
import org.eclipse.daanse.olap.api.result.CellSetMetaData;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.xmla.api.execute.statement.StatementResponse;
import org.eclipse.daanse.xmla.model.record.mddataset.CellTypeR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * XMLA serialization contract for NULL cells in
 * {@link XmlaResponseConverter#toStatementResponseMddataset}:
 * <ul>
 * <li>a NULL cell whose cell properties are all null is omitted from CellData
 * entirely — except at ordinal 0,</li>
 * <li>a NULL cell with a non-null property (e.g. FORMATTED_VALUE) is emitted
 * WITHOUT a &lt;Value&gt; element,</li>
 * <li>Double.POSITIVE_INFINITY is serialized as the string "INF",</li>
 * <li>a regular Double value keeps its numeric string form,</li>
 * <li>an error cell serializes its Throwable as xsd:string.</li>
 * </ul>
 */
class XmlaNullCellCharacterizationTest {

    private CellSet cellSet;
    private CellSetAxis columnsAxis;
    private CellSetAxis filterAxis;

    @BeforeEach
    void setUp() {
        cellSet = mock(CellSet.class);

        Statement statement = mock(Statement.class);
        Query query = mock(Query.class);
        lenient().when(cellSet.getStatement()).thenReturn(statement);
        lenient().when(statement.getQuery()).thenReturn(query);
        // no explicit CELL PROPERTIES in the MDX -> defaults VALUE + FORMATTED_VALUE
        lenient().when(query.getCellProperties()).thenReturn(new QueryComponent[0]);

        CellSetMetaData metaData = mock(CellSetMetaData.class);
        Cube cube = mock(Cube.class);
        lenient().when(cellSet.getMetaData()).thenReturn(metaData);
        lenient().when(metaData.getCube()).thenReturn(cube);
        lenient().when(cube.getName()).thenReturn("SalesCube");

        columnsAxis = mock(CellSetAxis.class);
        filterAxis = mock(CellSetAxis.class);
        lenient().when(cellSet.getAxes()).thenReturn(List.of(columnsAxis));
        lenient().when(cellSet.getFilterAxis()).thenReturn(filterAxis);

        // filter axis: empty WHERE clause -> no positions, no hierarchies
        CellSetAxisMetaData filterAxisMetaData = mock(CellSetAxisMetaData.class);
        lenient().when(filterAxis.getPositions()).thenReturn(List.of());
        lenient().when(filterAxis.getAxisMetaData()).thenReturn(filterAxisMetaData);
        lenient().when(filterAxisMetaData.getHierarchies()).thenReturn(List.of());
        lenient().when(filterAxisMetaData.getProperties()).thenReturn(List.of());
    }

    /** One measure member per column position, all on the Measures hierarchy. */
    private void mockAxisPositions(int count) {
        Hierarchy hierarchy = mock(Hierarchy.class);
        lenient().when(hierarchy.getName()).thenReturn("Measures");
        lenient().when(hierarchy.getUniqueName()).thenReturn("[Measures]");
        Level level = mock(Level.class);

        Position[] positions = new Position[count];
        for (int i = 0; i < count; i++) {
            Member member = mock(Member.class);
            lenient().when(member.getLevel()).thenReturn(level);
            lenient().when(member.getHierarchy()).thenReturn(hierarchy);
            lenient().when(member.getPropertyValue("MEMBER_UNIQUE_NAME")).thenReturn("[Measures].[M" + i + "]");
            lenient().when(member.getPropertyValue("MEMBER_CAPTION")).thenReturn("M" + i);
            lenient().when(member.getPropertyValue("LEVEL_UNIQUE_NAME")).thenReturn("[Measures].[MeasuresLevel]");
            lenient().when(member.getPropertyValue("LEVEL_NUMBER")).thenReturn(0);
            // primitive int in calculateDisplayInfo -> must not be null
            lenient().when(member.getPropertyValue("CHILDREN_CARDINALITY")).thenReturn(0);

            Position position = mock(Position.class);
            List<Member> members = List.of(member);
            lenient().when(position.getMembers()).thenReturn(members);
            lenient().when(position.iterator()).thenAnswer(inv -> members.iterator());
            positions[i] = position;
        }
        lenient().when(columnsAxis.getPositions()).thenReturn(List.of(positions));
        // null axis metadata -> converter falls back to its default properties
        lenient().when(columnsAxis.getAxisMetaData()).thenReturn(null);
    }

    private void mockCells(Cell... cells) {
        when(cellSet.getCell(anyList())).thenAnswer(inv -> {
            List<Integer> pos = inv.getArgument(0);
            return cells[pos.get(0)];
        });
    }

    private static Cell valueCell(Object value, String formatted) {
        Cell cell = mock(Cell.class);
        lenient().when(cell.isNull()).thenReturn(false);
        lenient().when(cell.getValue()).thenReturn(value);
        lenient().when(cell.getPropertyValue("VALUE")).thenReturn(value);
        lenient().when(cell.getPropertyValue("FORMATTED_VALUE")).thenReturn(formatted);
        return cell;
    }

    /**
     * A NULL cell that still carries a non-null (empty) FORMATTED_VALUE, the
     * way a formatted NULL measure is reported.
 */
    private static Cell formattedNullCell() {
        Cell cell = mock(Cell.class);
        lenient().when(cell.isNull()).thenReturn(true);
        lenient().when(cell.getValue()).thenReturn(null);
        lenient().when(cell.getPropertyValue("VALUE")).thenReturn(null);
        lenient().when(cell.getPropertyValue("FORMATTED_VALUE")).thenReturn("");
        return cell;
    }

    /** A NULL cell whose cell properties are all null. */
    private static Cell bareNullCell() {
        Cell cell = mock(Cell.class);
        lenient().when(cell.isNull()).thenReturn(true);
        lenient().when(cell.getValue()).thenReturn(null);
        lenient().when(cell.getPropertyValue("VALUE")).thenReturn(null);
        lenient().when(cell.getPropertyValue("FORMATTED_VALUE")).thenReturn(null);
        return cell;
    }

    private List<org.eclipse.daanse.xmla.api.mddataset.CellType> convert() {
        StatementResponse response = XmlaResponseConverter.toStatementResponseMddataset(cellSet, true, false);
        return response.mdDataSet().cellData().cell();
    }

    @Test
    @DisplayName("Regular double cell keeps value; NULL cell with formatted value is emitted without <Value>")
    void nullCellWithFormattedValueIsEmittedWithoutValue() {
        mockAxisPositions(2);
        mockCells(valueCell(42.5d, "42.5"), formattedNullCell());

        List<org.eclipse.daanse.xmla.api.mddataset.CellType> cells = convert();

        assertThat(cells).hasSize(2);

        CellTypeR value = (CellTypeR) cells.get(0);
        assertThat(value.cellOrdinal()).isZero();
        assertThat(value.value()).isNotNull();
        assertThat(value.value().value()).isEqualTo("42.5");

        // The NULL cell is NOT omitted (FORMATTED_VALUE is non-null), but the
        // VALUE property is skipped because cell.isNull() -> no <Value> element.
        CellTypeR nullCell = (CellTypeR) cells.get(1);
        assertThat(nullCell.cellOrdinal()).isEqualTo(1);
        assertThat(nullCell.value()).isNull();
        assertThat(nullCell.any()).anySatisfy(item -> {
            assertThat(item.tagName()).isEqualTo("FmtValue");
            assertThat(item.name()).isEmpty();
        });
    }

    @Test
    @DisplayName("NULL cell with all-null properties is omitted from CellData (MSAS style), except at ordinal 0")
    void bareNullCellIsOmittedExceptAtOrdinalZero() {
        mockAxisPositions(3);
        mockCells(valueCell(1.0d, "1"), bareNullCell(), valueCell(2.0d, "2"));

        List<org.eclipse.daanse.xmla.api.mddataset.CellType> cells = convert();

        // ordinal 1 is dropped entirely; remaining cells keep their ordinals
        assertThat(cells).hasSize(2);
        assertThat(cells).extracting(org.eclipse.daanse.xmla.api.mddataset.CellType::cellOrdinal)
                .containsExactly(0L, 2L);
    }

    @Test
    @DisplayName("NULL cell at ordinal 0 is always emitted, even with all-null properties")
    void bareNullCellAtOrdinalZeroIsEmitted() {
        mockAxisPositions(1);
        mockCells(bareNullCell());

        List<org.eclipse.daanse.xmla.api.mddataset.CellType> cells = convert();

        assertThat(cells).hasSize(1);
        CellTypeR nullCell = (CellTypeR) cells.get(0);
        assertThat(nullCell.cellOrdinal()).isZero();
        assertThat(nullCell.value()).isNull();
        assertThat(nullCell.any()).isEmpty();
    }

    @Test
    @DisplayName("Double.POSITIVE_INFINITY (the x/NULL result) is serialized as INF")
    void positiveInfinityIsSerializedAsInf() {
        mockAxisPositions(1);
        mockCells(valueCell(Double.POSITIVE_INFINITY, "Infinity"));

        List<org.eclipse.daanse.xmla.api.mddataset.CellType> cells = convert();

        assertThat(cells).hasSize(1);
        CellTypeR infCell = (CellTypeR) cells.get(0);
        assertThat(infCell.value()).isNotNull();
        assertThat(infCell.value().value()).isEqualTo("INF");
    }

    /**
     * Error cells: the evaluation error is stored as the cell VALUE (a
     * Throwable) and the formatted value carries the "#ERR:" text. The
     * converter has no error-specific branch: the Throwable falls through
     * ValueInfo's default case and is serialized as its toString() with type
     * xsd:string. Cell.getValue()/getPropertyValue must keep returning the
     * Throwable so this serialization stays stable.
 */
    @Test
    @DisplayName("Error cell serializes the Throwable's toString as xsd:string, FmtValue carries #ERR:")
    void errorCellSerializesThrowableAsString() {
        mockAxisPositions(1);
        RuntimeException failure = new RuntimeException("boom");
        Cell errorCell = mock(Cell.class);
        lenient().when(errorCell.isNull()).thenReturn(false);
        lenient().when(errorCell.getValue()).thenReturn(failure);
        lenient().when(errorCell.getPropertyValue("VALUE")).thenReturn(failure);
        lenient().when(errorCell.getPropertyValue("FORMATTED_VALUE")).thenReturn("#ERR: " + failure);
        mockCells(errorCell);

        List<org.eclipse.daanse.xmla.api.mddataset.CellType> cells = convert();

        assertThat(cells).hasSize(1);
        CellTypeR cell = (CellTypeR) cells.get(0);
        assertThat(cell.value()).isNotNull();
        assertThat(cell.value().value()).contains("boom");
        assertThat(cell.any()).anySatisfy(item -> {
            assertThat(item.tagName()).isEqualTo("FmtValue");
            assertThat(item.name()).startsWith("#ERR:");
        });
    }

}
