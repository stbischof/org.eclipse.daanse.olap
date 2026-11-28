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
package org.eclipse.daanse.olap.metadata;

import java.util.function.Supplier;

import org.eclipse.daanse.olap.metadata.model.CatalogDef;

/**
 * Hands out the description of a catalog: the seam through which a context gets what it serves, whoever wrote it --
 * the builders in code or a connector that translates another catalog format. Each call may build the description
 * anew; a description is a value, so two calls for an unchanged source give equal content.
 */
@FunctionalInterface
public interface CatalogDescriptionSupplier extends Supplier<CatalogDef> {

    /** A supplier of one description built once. */
    static CatalogDescriptionSupplier of(CatalogDef catalog) {
        return () -> catalog;
    }

    /**
     * The identity of the source the description is made from, where it has one: contexts whose sources have the same
     * identity (and the same database) share one pooled catalog. A translator passes its source's identity on; it is
     * no fingerprint of the description, whose tables and columns compare by identity. Empty: the context keeps a
     * catalog of its own for this supplier.
     */
    default java.util.Optional<byte[]> contentSha256() {
        return java.util.Optional.empty();
    }
}
