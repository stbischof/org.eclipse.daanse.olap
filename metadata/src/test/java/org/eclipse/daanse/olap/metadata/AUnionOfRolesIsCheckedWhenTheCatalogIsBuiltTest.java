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
package org.eclipse.daanse.olap.metadata;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.daanse.olap.metadata.builder.CatalogBuilder;
import org.eclipse.daanse.olap.metadata.model.Access;
import org.junit.jupiter.api.Test;

/**
 * What the catalog says about a union of roles when it is built.
 *
 * <p>A union names roles the catalog declares, wherever in the catalog they are declared; a union that names
 * a role the catalog does not declare fails the build, naming the union and the role; and a name cannot be
 * both a role with grants of its own and a union.
 */
class AUnionOfRolesIsCheckedWhenTheCatalogIsBuiltTest {

    private static CatalogBuilder shop() {
        return Olap.catalog("Shop")
                .cube("Sales", c -> c.dimension("H", d -> d.hierarchy(h -> h.allMember("All H").level("H"))).measure("Sold"))
                .role("R1", r -> r.catalog(Access.Catalog.ALL));
    }

    /** A role declared after the union that names it is found all the same. */
    @Test
    void aRoleDeclaredAfterTheUnionIsFound() {
        assertThatCode(() -> shop().unionRole("U", "R1", "R2").role("R2", r -> r.catalog(Access.Catalog.ALL)).build())
                .doesNotThrowAnyException();
    }

    /** A union naming a role the catalog does not declare fails the build, naming union and role. */
    @Test
    void aUnionNamingAnUnknownRoleFailsTheBuild() {
        assertThatThrownBy(() -> shop().unionRole("U", "R1", "Ghost").build())
                .isInstanceOf(org.eclipse.daanse.olap.findings.FindingException.class).hasMessageContaining("U").hasMessageContaining("Ghost");
    }

    /** A name that is a role with grants of its own cannot be a union as well. */
    @Test
    void aRoleWithGrantsOfItsOwnCannotBeAUnionAsWell() {
        assertThatThrownBy(() -> shop().role("U", r -> r.catalog(Access.Catalog.ALL)).unionRole("U", "R1").build())
                .isInstanceOf(org.eclipse.daanse.olap.findings.FindingException.class).hasMessageContaining("U");
    }
}
