/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2005-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara
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


package org.eclipse.daanse.olap.api.type;

public class NumericType extends ScalarType {

	public static final NumericType INSTANCE = new NumericType();

	private NumericType() {
		this("NUMERIC");
	}

	protected NumericType(String digest) {
		super(digest);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof NumericType && toString().equals(obj.toString());
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Number || value instanceof Character;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
