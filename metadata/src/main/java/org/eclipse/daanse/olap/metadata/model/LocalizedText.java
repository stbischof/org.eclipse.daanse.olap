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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** A text with optional translations; {@code text} is what a client without a matching locale sees. */
public record LocalizedText(String text, Map<Locale, String> translations) {

    public LocalizedText {
        translations = Map.copyOf(translations);
    }

    public static LocalizedText of(String text) {
        return new LocalizedText(text, Map.of());
    }

    /** This text with one more translation. */
    public LocalizedText in(Locale locale, String translation) {
        Map<Locale, String> all = new LinkedHashMap<>(translations);
        all.put(locale, translation);
        return new LocalizedText(text, all);
    }

    /** The translation for the locale, falling back to its language and then to {@link #text()}. */
    public String resolve(Locale locale) {
        if (locale != null) {
            String exact = translations.get(locale);
            if (exact != null) {
                return exact;
            }
            String byLanguage = translations.get(Locale.of(locale.getLanguage()));
            if (byLanguage != null) {
                return byLanguage;
            }
        }
        return text;
    }
}
