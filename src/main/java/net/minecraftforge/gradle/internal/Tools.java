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

    // Forked SlimeLauncher: TSRG srg-mcp for FG2-era FML, ELF filter for LWJGL2 natives.
    // Does not set org.lwjgl.librarypath (LWJGL3 self-extracts from classpath natives jars).
    static final Tool SLIMELAUNCHER = Tool.of("slimelauncher", "net.zznty:slime-launcher:0.2.5.0", ZZNTY_MAVEN, 8, "net.minecraftforge.launcher.Main");

    // Multi-loader Mavenizer fork (LWJGL3 :natives-<os> via Patcher.forAllLibraries).
    // Tool.of signature is (name, artifact, repoUrl, javaVersion, mainClass).
    static final Tool MAVENIZER = Tool.of("mavenizer", "net.zznty:minecraft-mavenizer:0.5.42", ZZNTY_MAVEN, 25, "net.minecraftforge.mcmaven.cli.Main");
}
