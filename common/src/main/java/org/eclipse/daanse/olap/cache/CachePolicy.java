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
package org.eclipse.daanse.olap.cache;

import java.util.OptionalInt;

import org.eclipse.daanse.olap.api.ContextConfig;
import org.eclipse.daanse.olap.api.element.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-cube cache policy from the {@code daanse:cache.*} tags, resolved once
 * at load. Precedence: disableCaching kill switch, cube tag, catalog
 * {@code daanse:cache.mode}, global default; unparsable values warn and fall
 * back. {@link #membersFor} and {@link #levelPreCacheThresholdFor} resolve
 * the hierarchy-level tags against a cube policy.
 */
public record CachePolicy(boolean members, boolean cells, boolean nativeSets,
        OptionalInt levelPreCacheThreshold) {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachePolicy.class);

    public static final String TAG_MODE = "daanse:cache.mode";
    public static final String TAG_MEMBERS = "daanse:cache.members";
    public static final String TAG_CELLS = "daanse:cache.cells";
    public static final String TAG_NATIVE = "daanse:cache.native";
    public static final String TAG_LEVEL_PRECACHE_THRESHOLD = "daanse:cache.level-precache-threshold";

    public static CachePolicy resolve(MetaData catalogMeta, MetaData cubeMeta, ContextConfig config) {
        if (config.disableCaching()) {
            return new CachePolicy(false, false, false, OptionalInt.empty());
        }
        boolean catalogDefault = !"off".equals(stringTag(catalogMeta, TAG_MODE));
        boolean members = boolTag(cubeMeta, TAG_MEMBERS,
                catalogDefault && config.enableRolapCubeMemberCache());
        boolean cells = boolTag(cubeMeta, TAG_CELLS, catalogDefault);
        boolean nativeSets = boolTag(cubeMeta, TAG_NATIVE, catalogDefault);
        return new CachePolicy(members, cells, nativeSets, intTag(cubeMeta, TAG_LEVEL_PRECACHE_THRESHOLD));
    }

    /**
     * Effective members caching for one hierarchy: the hierarchy's
     * {@code daanse:cache.members} tag overrides the cube policy; absent or
     * unparsable values fall back to it.
     */
    public static boolean membersFor(MetaData hierarchyMeta, CachePolicy cubePolicy) {
        return boolTag(hierarchyMeta, TAG_MEMBERS, cubePolicy.members());
    }

    /**
     * Effective level-precache-threshold override for one hierarchy: the
     * hierarchy tag wins over the cube tag; empty means the global config
     * value applies at the call site.
     */
    public static OptionalInt levelPreCacheThresholdFor(MetaData hierarchyMeta, CachePolicy cubePolicy) {
        OptionalInt own = intTag(hierarchyMeta, TAG_LEVEL_PRECACHE_THRESHOLD);
        return own.isPresent() ? own : cubePolicy.levelPreCacheThreshold();
    }

    private static String stringTag(MetaData meta, String tag) {
        Object value = meta == null ? null : meta.get(tag);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean boolTag(MetaData meta, String tag, boolean defaultValue) {
        String value = stringTag(meta, tag);
        if (value == null) {
            return defaultValue;
        }
        return switch (value) {
        case "on" -> true;
        case "off" -> false;
        default -> {
            LOGGER.warn("tag {} carries '{}' (expected on/off); using default {}", tag, value, defaultValue);
            yield defaultValue;
        }
        };
    }

    private static OptionalInt intTag(MetaData meta, String tag) {
        String value = stringTag(meta, tag);
        if (value == null) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            LOGGER.warn("tag {} carries '{}' (expected an integer); ignoring", tag, value);
            return OptionalInt.empty();
        }
    }
}
