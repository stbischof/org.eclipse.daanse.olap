/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2006-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.common;

import java.util.Set;

import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.query.component.MdxVisitorImpl;

public class IdentifierVisitor extends MdxVisitorImpl {
    private final Set<Id> identifiers;

    public IdentifierVisitor(Set<Id> identifiers) {
        this.identifiers = identifiers;
    }

    @Override
	public Object visitId(Id id) {
        identifiers.add(id);
        return null;
    }
}
