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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.daanse.olap.metadata.model.ElementInfo;
import org.eclipse.daanse.olap.metadata.model.LocalizedText;
import org.eclipse.daanse.olap.metadata.model.MetaItem;

/** What every builder of a named element offers: caption, description, visibility, annotations. */
public abstract class ElementBuilder<B extends ElementBuilder<B>> {

    final String name;
    private LocalizedText caption;
    private LocalizedText description;
    private boolean visible = true;
    private final List<MetaItem> meta = new ArrayList<>();

    ElementBuilder(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    final B self() {
        return (B) this;
    }

    public B caption(String text) {
        // the base caption keeps the translations given before it
        this.caption = caption == null ? LocalizedText.of(text) : new LocalizedText(text, caption.translations());
        return self();
    }

    /** Adds a translation of the caption; the caption itself defaults to the name. */
    public B caption(Locale locale, String text) {
        this.caption = (caption == null ? LocalizedText.of(name) : caption).in(locale, text);
        return self();
    }

    public B description(String text) {
        this.description = LocalizedText.of(text);
        return self();
    }

    public B description(Locale locale, String text) {
        this.description = (description == null ? LocalizedText.of("") : description).in(locale, text);
        return self();
    }

    /** Clients do not list the element; MDX still reaches it. */
    public B hidden() {
        this.visible = false;
        return self();
    }

    public B meta(String key, String value) {
        meta.add(new MetaItem(key, value));
        return self();
    }

    final ElementInfo info() {
        return new ElementInfo(caption, description, visible, meta);
    }
}
