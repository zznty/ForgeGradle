/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradle.internal;

import net.minecraftforge.gradle.MinecraftAccessTransformersContainer;
import org.gradle.api.provider.Property;

interface MinecraftAccessTransformersContainerInternal extends MinecraftAccessTransformersContainer {
    boolean hasAccessTransformersPlugin();

    Property<String> getAccessTransformerPath();

    @Override
    default void setAccessTransformer(String accessTransformer) {
        this.getAccessTransformerPath().set(accessTransformer);
    }

    @Override
    default void setAccessTransformer(boolean accessTransformer) {
        if (accessTransformer)
            this.getAccessTransformerPath().set(DEFAULT_PATH);
        else
            this.getAccessTransformerPath().unset();
    }

    // ─── Access Widener ───

    Property<String> getAccessWidenerPath();

    @Override
    default void setAccessWidener(String accessWidener) {
        this.getAccessWidenerPath().set(accessWidener);
    }

    @Override
    default void setAccessWidener(boolean accessWidener) {
        if (accessWidener)
            this.getAccessWidenerPath().set(DEFAULT_ACCESS_WIDENER_PATH);
        else
            this.getAccessWidenerPath().unset();
    }
}
