/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradle;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;

public interface MinecraftAccessTransformersContainer {
    /// The default path, from the source set's [resources][org.gradle.api.tasks.SourceSet#getResources()], for the
    /// AccessTransformers config to be located in.
    String DEFAULT_PATH = "META-INF/accesstransformer.cfg";
    /// The default path for the AccessWidener config file.
    String DEFAULT_ACCESS_WIDENER_PATH = "META-INF/${mod_id}.accesswidener";

    /// Gets the AccessTransformer configuration files to use.
    ///
    /// @return The property for the configuration file to use
    /// @see #getAccessTransformers()
    ConfigurableFileCollection getAccessTransformer();

    /// Gets the AccessTransformer configuration files to use.
    ///
    /// @return The property for the configuration file to use
    default ConfigurableFileCollection getAccessTransformers() {
        return getAccessTransformer();
    }

    /// Sets the path, relative to this dependency's [org.gradle.api.tasks.SourceSet#getResources()], to the
    /// AccessTransformers config file to use.
    ///
    /// The default location for the AccessTransformer config file will be in
    /// [org.gradle.api.tasks.SourceSet#getResources()] -> first directory of
    /// [org.gradle.api.file.SourceDirectorySet#getSrcDirs()] -> `META-INF/accesstransformer.cfg`. If the source set,
    /// for whatever reason, does not have any resources directories set, ForgeGradle will make the best guess of
    /// {@code src/}{@link org.gradle.api.tasks.SourceSet#getName() name}{@code
    /// /resources/META-INF/accesstransformer.cfg}.
    ///
    /// @param accessTransformer The path to the config file to use
    /// @apiNote Using [#getAccessTransformer()] -> [RegularFileProperty#set] is strongly recommended if your config
    /// file is in a strict location.
    void setAccessTransformer(String accessTransformer);

    /// Sets if this dependency should use AccessTransformers.
    ///
    /// If `true`, this calls [#setAccessTransformer(String)] using [#DEFAULT_PATH] as the path. If `false`, this will
    /// force this dependency to *not use* AccessTransformers, even if the convention is set to do so from the Minecraft
    /// extension. This can be used to opt-out of AccessTransformer for a single Minecraft dependency if it is enabled
    /// globally for the rest of them in a project.
    ///
    /// @param accessTransformer If this dependency should use AccessTransformers
    /// @see #setAccessTransformer(String)
    void setAccessTransformer(boolean accessTransformer);

    /// Uses the default configuration path for AccessTransformers.
    ///
    /// @see #setAccessTransformer(boolean)
    default void useDefaultAccessTransformer() {
        this.setAccessTransformer(true);
    }

    // ─── Access Widener (Fabric-originated, cross-loader) ───

    /// Gets the AccessWidener configuration file to use.
    ///
    /// The Access Widener is a Fabric-originated format (file header `accessWidener v2 named`) that declares which
    /// Minecraft classes/members should have their access widened. For Fabric it is consumed natively by the Fabric
    /// Loader. For Forge and NeoForge the Mavenizer converts it to an Access Transformer (.cfg) during deobfuscation
    /// of the Minecraft jar, so the same file works across all loaders.
    ///
    /// @return The property for the configuration file to use
    ConfigurableFileCollection getAccessWidener();

    /// Convenience alias for [#getAccessWidener()].
    default ConfigurableFileCollection getAccessWideners() {
        return getAccessWidener();
    }

    /// Sets the path, relative to this dependency's [org.gradle.api.tasks.SourceSet#getResources()], to the
    /// AccessWidener config file to use. The file must be in `accessWidener v2 named` format (the Fabric standard).
    ///
    /// Unlike the AT default, there is no universally-standard path for AW files — the path is typically
    /// `modid.accesswidener` at the root of resources, or `META-INF/modid.accesswidener`.
    ///
    /// @param accessWidener The path to the config file to use
    void setAccessWidener(String accessWidener);

    /// Sets if this dependency should use AccessWideners.
    ///
    /// If `true`, this calls [#setAccessWidener(String)] using [#DEFAULT_ACCESS_WIDENER_PATH] as the path.
    ///
    /// @param accessWidener If this dependency should use AccessWideners
    void setAccessWidener(boolean accessWidener);

    /// Uses the default configuration path for AccessWideners.
    ///
    /// @see #setAccessWidener(boolean)
    default void useDefaultAccessWidener() {
        this.setAccessWidener(true);
    }
}
