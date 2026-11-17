/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
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
package org.eclipse.daanse.olap.cellformatter;

import java.util.Locale;

import org.eclipse.daanse.olap.util.format.Format;

/**
 * A {@link FormatStringCellFormatter} over a {@link Locale}: the locale picks the
 * {@link Format} for a given format string. Shared per locale through
 * {@link FormatStringCellFormatter#forLocale(Locale)}.
 */
public class LocaleCellFormatter implements FormatStringCellFormatter {
    final Locale locale;

    /**
     * Creates a LocaleCellFormatter.
     *
     * @param locale Locale
     */
    public LocaleCellFormatter(Locale locale) {
        this.locale = locale;
    }

    @Override
    public String format(Object value, String formatString) {
        if (value instanceof Throwable) {
            return "#ERR: " + value.toString();
        }
        Format format = getFormat(formatString);
        return format.format(value);
    }

    private Format getFormat(String formatString) {
        return Format.get(formatString, locale);
    }
}
