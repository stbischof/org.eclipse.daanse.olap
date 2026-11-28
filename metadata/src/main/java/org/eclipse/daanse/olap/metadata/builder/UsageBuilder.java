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

/**
 * What a usage of a shared dimension says of itself: a caption, a description, its visibility. The
 * dimension shows these instead of the shared dimension's own.
 */
public final class UsageBuilder extends ElementBuilder<UsageBuilder> {

    UsageBuilder(String usageName) {
        super(usageName);
    }

    org.eclipse.daanse.olap.metadata.model.ElementInfo described() {
        return info();
    }
}
