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
*/
package org.eclipse.daanse.olap.query.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.model.api.SelectStatement;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.ccc.CCCMdxParserProvider;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.query.component.CellProperty;
import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.Refresh;
import org.eclipse.daanse.olap.api.query.component.Subcube;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Statements and parts that the MDX parser reads but the engine does not run are refused with a message that names
 * them, never dropped; the forms next to them convert as they did.
 */
class UnsupportedStatementConversionTest {

    private static final String SUBSELECT = "SELECT {[Port].[Port].[Kiel]} ON 0 FROM [Coast]";

    private static final String MEMBER_AND_SET_STATEMENTS =
            "CREATE MEMBER, CREATE SET, DROP MEMBER and DROP SET statements are not supported";

    private static final String SUBCUBE_STATEMENTS = "CREATE SUBCUBE and DROP SUBCUBE statements are not supported";

    private final QueryProviderImpl provider = new QueryProviderImpl();

    private static MdxStatement parse(String mdx) throws MdxParserException {
        return new CCCMdxParserProvider().newParser(mdx, Set.of()).parseMdxStatement();
    }

    private void assertRefused(String mdx, String message) throws MdxParserException {
        MdxStatement statement = parse(mdx);

        assertThatThrownBy(() -> provider.createQuery(null, statement, false))
                .isInstanceOf(UnsupportedOperationException.class).hasMessage(message);
    }

    @Test
    void createSubcubeIsRefused() throws MdxParserException {
        assertRefused("CREATE SUBCUBE [Coast] AS " + SUBSELECT, SUBCUBE_STATEMENTS);
    }

    @Test
    void createSubcubeNonVisualIsRefused() throws MdxParserException {
        assertRefused("CREATE SUBCUBE [Coast] AS NON VISUAL (" + SUBSELECT + ")", SUBCUBE_STATEMENTS);
    }

    @Test
    void dropSubcubeIsRefused() throws MdxParserException {
        assertRefused("DROP SUBCUBE [Coast]", SUBCUBE_STATEMENTS);
    }

    @Test
    void callIsRefused() throws MdxParserException {
        assertRefused("CALL PortTools.Reload([Port].[Port].[Kiel]) ON [Coast]", "CALL statements are not supported");
    }

    @Test
    void callWithBangQualifiedProcedureIsRefused() throws MdxParserException {
        assertRefused("CALL PortTools!Reload(1)", "CALL statements are not supported");
    }

    @Test
    void refreshStillConverts() throws MdxParserException {
        QueryComponent component = provider.createQuery(null, parse("REFRESH CUBE [Coast]"), false);

        assertThat(component).isInstanceOf(Refresh.class);
        assertThat(((Refresh) component).getCubeName()).isEqualTo("Coast");
    }

    @Test
    void positionalCreateIsRefusedAsCreateMemberIs() throws MdxParserException {
        assertRefused("CREATE [Measures].[Twice] = [Measures].[Tonnes] * 2", MEMBER_AND_SET_STATEMENTS);
    }

    @Test
    void createMemberIsRefusedAsBefore() throws MdxParserException {
        assertRefused("CREATE MEMBER [Coast].[Measures].[Twice] AS [Measures].[Tonnes] * 2",
                MEMBER_AND_SET_STATEMENTS);
    }

    @Test
    void nonVisualSubselectIsRefused() throws MdxParserException {
        assertRefused("SELECT {[Measures].[Tonnes]} ON 0 FROM NON VISUAL (" + SUBSELECT + ")",
                "NON VISUAL before a subselect is not supported");
    }

    @Test
    void nonVisualInnerSubselectIsRefused() throws MdxParserException {
        assertRefused("SELECT {[Measures].[Tonnes]} ON 0 FROM (SELECT {[Port].[Port].[Rostock]} ON 0 FROM NON VISUAL ("
                + SUBSELECT + "))", "NON VISUAL before a subselect is not supported");
    }

    @Test
    void subselectStillConverts() throws MdxParserException {
        SelectStatement select = (SelectStatement) parse("SELECT {[Measures].[Tonnes]} ON 0 FROM (" + SUBSELECT + ")");

        Subcube subcube = MdxToQueryConverter.convertCubeClause(select.selectCubeClause());

        assertThat(subcube.getCubeName()).isEqualTo("Coast");
        assertThat(subcube.getAxisExps()).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Key([$Port].[Port])|Key",
        "Key([$Port].[Port], 2)|Key",
        "Name([$Port].[Port])|Name",
        "Caption([$Port].[Port])|Caption",
        "[$Port].[Region], Name([$Port].[Port])|Name" })
    void drillthroughReturnWrappedColumnIsRefused(String returnList, String function) throws MdxParserException {
        MdxStatement statement = parse(
                "DRILLTHROUGH SELECT {[Measures].[Tonnes]} ON 0 FROM [Coast] RETURN " + returnList);
        QueryProviderImpl answering = answeringWith(mock(Query.class));

        assertThatThrownBy(() -> answering.createQuery(null, statement, false))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("DRILLTHROUGH RETURN with a column wrapped in " + function + " is not supported");
    }

    @Test
    void drillthroughReturnBareColumnStillConverts() throws MdxParserException {
        List<List<Segment>> looked = new ArrayList<>();
        CatalogReader reader = mock(CatalogReader.class);
        when(reader.lookupCompound(any(), any(), anyBoolean(), any())).thenAnswer(invocation -> {
            looked.add(invocation.getArgument(1));
            return mock(OlapElement.class);
        });
        Query query = mock(Query.class);
        when(query.getCatalogReader(true)).thenReturn(reader);

        QueryComponent component = answeringWith(query).createQuery(null,
                parse("DRILLTHROUGH SELECT {[Measures].[Tonnes]} ON 0 FROM [Coast] RETURN [$Port].[Port]"), false);

        assertThat(component).isInstanceOf(DrillThrough.class);
        assertThat(((DrillThrough) component).getReturnList()).hasSize(1);
        assertThat(looked).hasSize(1);
        assertThat(looked.get(0)).map(segment -> ((NameSegment) segment).getName()).containsExactly("$Port", "Port");
    }

    /** The select of a DRILLTHROUGH is converted, but answered by the given query instead of one over a catalog. */
    private static QueryProviderImpl answeringWith(Query query) {
        return new QueryProviderImpl() {
            @Override
            public Query createQuery(Statement statement, Formula[] formula, QueryAxis[] axes, Subcube subcube,
                    QueryAxis slicerAxis, CellProperty[] cellProps, boolean strictValidation) {
                return query;
            }
        };
    }
}
