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

    // Forked SlimeLauncher: adds TSRG srg-mcp generation for FG2-era FML forks (Cleanroom),
    // ELF arch filter for native extraction, and LWJGL/JInput librarypath properties.
    static final Tool SLIMELAUNCHER = Tool.of("slimelauncher", "net.zznty:slime-launcher:0.2.4.0", ZZNTY_MAVEN, 8, "net.minecraftforge.launcher.Main");

    // The multi-loader-capable Mavenizer fork, published under net.zznty on our maven.
    // Tool.of signature is (name, artifact, repoUrl, javaVersion, mainClass).
    static final Tool MAVENIZER = Tool.of("mavenizer", "net.zznty:minecraft-mavenizer:0.5.35", ZZNTY_MAVEN, 25, "net.minecraftforge.mcmaven.cli.Main");
}
