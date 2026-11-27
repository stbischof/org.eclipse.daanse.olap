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
package org.eclipse.daanse.olap.findings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The literals and their order are part of what is read elsewhere: log levels, rowsets, model files. */
class LayerTest {

    @Test
    void theLiteralsStandInTheirOrder() {
        assertThat(Layer.values()).as("the literals of Layer").containsExactly(Layer.MODEL, Layer.CONNECTOR,
                Layer.DESCRIPTION, Layer.BINDING, Layer.DATA, Layer.QUERY);
    }
}
