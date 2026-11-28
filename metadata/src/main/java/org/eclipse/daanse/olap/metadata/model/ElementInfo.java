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

/**
 * What every named element carries besides its name.
 *
 * @param caption     what a client shows; {@code null} means the name
 * @param description {@code null} when there is none
 * @param visible     whether clients list the element
 * @param meta        free annotations
 */
public record ElementInfo(LocalizedText caption, LocalizedText description, boolean visible, List<MetaItem> meta) {

    public static final ElementInfo DEFAULT = new ElementInfo(null, null, true, List.of());

    public ElementInfo {
        meta = List.copyOf(meta);
    }
}
