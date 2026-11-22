/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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

package org.eclipse.daanse.olap.calc.base.cache;

import org.eclipse.daanse.olap.api.cache.ExpCacheDescriptor;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;

/**
 * Calculation which retrieves the value of an underlying calculation from
 * cache.
 *
 * @author jhyde
 * @since Oct 10, 2005
 */
public class CacheCalc extends AbstractNestedUnknownCalc {
	private final ExpCacheDescriptor key;

	public CacheCalc(Type type, ExpCacheDescriptor key) {
		super(type);
		this.key = key;
	}

	@Override
	public Object evaluateInternal(Evaluator evaluator) {
		return evaluator.getCachedResult(key);
	}

	@Override
	public Calc<?>[] getChildCalcs() {
		return new Calc[] { key.getCalc() };
	}
}
