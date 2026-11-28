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
package org.eclipse.daanse.olap.metadata.builder;

import org.eclipse.daanse.olap.metadata.model.NamedSetDef;

/**
 * What a named set says of itself besides its formula: a folder, a caption, a description and annotations.
 */
public final class NamedSetBuilder extends ElementBuilder<NamedSetBuilder> {

    private final String formula;
    private org.eclipse.daanse.olap.metadata.model.LocalizedText displayFolder;

    NamedSetBuilder(String setName, String formula) {
        super(setName);
        this.formula = formula;
    }

    /** The folder clients show the set in. */
    public NamedSetBuilder displayFolder(String folder) {
        this.displayFolder = folder == null ? null : org.eclipse.daanse.olap.metadata.model.LocalizedText.of(folder);
        return this;
    }

    NamedSetDef build() {
        return new NamedSetDef(name, formula, displayFolder, info());
    }
}
