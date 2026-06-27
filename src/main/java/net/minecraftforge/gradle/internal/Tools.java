/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradle.internal;

import net.minecraftforge.gradleutils.shared.Tool;

final class Tools {
    private Tools() { }

    /// Our Reposilite maven, the default source for the (zznty-published) tool artifacts below.
    private static final String ZZNTY_MAVEN = "https://maven.zznty.ru/releases/";

    // SlimeLauncher is unchanged from upstream (we publish no fork of it), so it still comes from Forge maven.
    static final Tool SLIMELAUNCHER = Tool.ofForge("slimelauncher", "net.minecraftforge:slime-launcher:0.2.2", 8, "net.minecraftforge.launcher.Main");

    // The multi-loader-capable Mavenizer fork, published under net.zznty on our maven.
    static final Tool MAVENIZER = Tool.of("mavenizer", ZZNTY_MAVEN, "net.zznty:minecraft-mavenizer:0.5.26", 25, "net.minecraftforge.mcmaven.cli.Main");
}
