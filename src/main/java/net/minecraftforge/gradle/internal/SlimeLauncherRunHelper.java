/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradle.internal;

import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

class SlimeLauncherRunHelper {
    // Legacy replacement tokens. See https://github.com/MinecraftForge/ForgeGradle/issues/1048
    //
    // Multi-value tokens accept a `separator` template argument, e.g. {runtime_classpath,separator=path} or
    // {source_roots,separator=newline}. See Util#resolveSeparator for accepted values. Each token keeps the
    // historical default separator so existing templates are unchanged.
    static Map<String, Object> buildTokens(SlimeLauncherRunTask task, SlimeLauncherOptionsInternal options, List<SourceSetNested> defaultSourceSets, Function<SourceSetNested, Set<String>> sourceOutputs) {
        var ret = new HashMap<String, Object>();
        // Should be taken care of by SlimeLauncher
        ret.put("asset_index", (Supplier<String>) () -> "{asset_index}");
        ret.put("assets_root", (Supplier<String>) () -> "{assets_root}");
        ret.put("natives",  (Supplier<String>) () -> "{natives}");

        ret.put("mcp_mappings", (Supplier<String>) task.getMappingChannel().zip(task.getMappingVersion(), (c, v) -> c + '_' + v)::get);
        var minecraft = getClasspath(task.getMinecraftClasspath());
        var runtime = getClasspath(task.getRuntimeClasspath());
        var modules = getClasspath(task.getPatcherModules());
        // Classpaths default to the platform path separator.
        ret.put("minecraft_classpath", joined(minecraft, File.pathSeparator));
        ret.put("runtime_classpath", joined(runtime, File.pathSeparator));
        ret.put("modules", joined(modules, File.pathSeparator));
        // Classpath files default to one entry per line (historical behaviour).
        ret.put("minecraft_classpath_file", classpathFile(task, "minecraft", minecraft, System.lineSeparator()));
        ret.put("runtime_classpath_file", classpathFile(task, "runtime_" + task.getSourceSetName().get(), runtime, System.lineSeparator()));
        ret.put("mc_version", (Supplier<String>) task.getMinecraftVersion()::get);
        ret.put("mcp_version", (Supplier<String>) task.getMCPVersion()::get);
        // Source roots historically use the path separator between entries and `%%` between mods (FML).
        ret.put("source_roots", getSourceRoots(task, options, defaultSourceSets, sourceOutputs));
        // Despite the name this is set to createSrgToMcp.getOutput().get().getAsFile().getAbsolutePath() so.. Srg -> MCP .srg mapping file.
        // This is taken care of in SlimeLauncher, because I don't want to teach FG about SRG files.
        // So add a passthrough to make it not output a warning
        ret.put("mcp_to_srg", (Supplier<String>) () -> "{mcp_to_srg}");
        return ret;
    }

    private static Supplier<List<String>> getClasspath(FileCollection files) {
        return new Lazy<>(() -> {
            var ret = new ArrayList<String>(files.getFiles().size());
            for (var file : files.getFiles())
                ret.add(file.getAbsolutePath());
            return ret;
        });
    }

    /// A token that joins the given entries with a caller-selectable separator (default {@code def}).
    private static Util.Token joined(Supplier<List<String>> files, String def) {
        return args -> String.join(Util.resolveSeparator(args, def), files.get());
    }

    /// A token that writes the entries to a file (one per line by default, or joined by the requested
    /// separator) and resolves to that file's absolute path. The separator becomes part of the file name so
    /// different separators don't clobber each other.
    private static Util.Token classpathFile(SlimeLauncherRunTask task, String name, Supplier<List<String>> files, String def) {
        return args -> {
            var sep = Util.resolveSeparator(args, def);
            var suffix = sep.equals(File.pathSeparator) ? "_path"
                : sep.equals(System.lineSeparator()) ? ""
                : "_" + Integer.toHexString(sep.hashCode());
            var file = task.getLocalCacheDir().file(name + suffix + "_classpath.txt").get().getAsFile();
            try {
                Files.writeString(file.toPath(), String.join(sep, files.get()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error when writing classpath file: " + file.getAbsolutePath(), e);
            }
            return file.getAbsolutePath();
        };
    }

    static List<SourceSetNested> getDefaultSourceSets(SlimeLauncherRunTask task) {
        var java = task.getProject().getExtensions().findByType(JavaPluginExtension.class);
        if (java == null)
            return List.of();

        var ret = new ArrayList<SourceSetNested>();
        var main = java.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);
        if (main != null)
            ret.add(task.getProject().getObjects().newInstance(SourceSetNested.class, main));

        var taskName = task.getSourceSetName().getOrNull();
        if (taskName != null && !SourceSet.MAIN_SOURCE_SET_NAME.equals(taskName)) {
            var other = java.getSourceSets().findByName(taskName);
            if (other != null)
                ret.add(task.getProject().getObjects().newInstance(SourceSetNested.class, other));
        }

        return ret;
    }

    // Gets a list of all output directories for
    static Set<String> getOutputs(SourceSetNested sourceSet) {
        var ret = new LinkedHashSet<String>();
        if (sourceSet.getOutput().getResourcesDir() != null)
            ret.add(sourceSet.getOutput().getResourcesDir().getAbsolutePath());
        for (var file :  sourceSet.getOutput().getAsFileCollection())
            ret.add(file.getAbsolutePath());
        return ret;
    }

    /// Resolves the mod source roots.
    ///
    /// The plain {@code {source_roots}} form keeps the historical Forge/FML behaviour exactly: each entry is
    /// prefixed with {@code <modid>%%}, single-entry mods are duplicated, entries are joined with the path
    /// separator and mods are joined with a single path separator. Non-Forge loaders opt out via arguments.
    ///
    /// Template arguments:
    /// - {@code separator}: delimiter between entries within a single mod (default: path separator).
    /// - {@code group-separator}: delimiter between mods (default: single path separator). Fabric's
    ///   {@code fabric.classPathGroups} wants a doubled path separator: {@code group-separator=path-path}.
    /// - {@code prefix}: {@code mod-name} (default) prefixes each entry with {@code <modid>%%} (FML);
    ///   {@code none} disables the prefix (Fabric).
    /// - {@code pad-single}: {@code true} (default) duplicates a mod's single entry (FML requires >= 2);
    ///   {@code false} disables it (Fabric drops single-entry groups anyway).
    static Util.Token getSourceRoots(SlimeLauncherRunTask task, SlimeLauncherOptionsInternal options, List<SourceSetNested> defaults, Function<SourceSetNested, Set<String>> sourceOutputs) {
        return args -> {
            var entrySep = Util.resolveSeparator(args, File.pathSeparator);
            var groupSep = resolveGroupSeparator(args, File.pathSeparator);
            // Forge/FML defaults: mod-name '%%' prefix on, single-entry padding on. Loaders opt out explicitly.
            var modPrefix = !"none".equals(args.get("prefix"));
            var padSingle = !"false".equals(args.get("pad-single"));

            var mods = new TreeMap<String, List<SourceSetNested>>();

            // If there are no mods defined, try and get the main sourceset
            if (options.getMods().isEmpty()) {
                mods.put("", defaults);
            } else {
                for (var mod : options.getMods())
                    mods.put(mod.getName(), mod.getSources().isEmpty() ? defaults : mod.getSources());
            }

            var ret = new StringBuilder();

            for (var entry : mods.entrySet()) {
                var entries = new ArrayList<String>();
                var prefix = modPrefix && !entry.getKey().isEmpty() ? entry.getKey() + "%%" : "";
                for (var source : entry.getValue()) {
                    for (var dir : sourceOutputs.apply(source))
                        entries.add(prefix + dir);
                }

                if (padSingle && entries.size() == 1) // FML Requires at least 2 directories, so duplicate
                    entries.add(entries.get(0));

                if (!ret.isEmpty())
                    ret.append(groupSep);
                ret.append(String.join(entrySep, entries));
            }

            return ret.toString();
        };
    }

    /// Like {@link Util#resolveSeparator}, but for the {@code group-separator} argument and with support for
    /// the {@code path-path} alias (a doubled path separator, as required by Fabric class-path groups).
    private static String resolveGroupSeparator(Map<String, String> args, String def) {
        var sep = args.get("group-separator");
        if (sep == null || sep.isBlank())
            return def;
        return switch (sep) {
            case "path" -> File.pathSeparator;
            case "path-path", "double-path" -> File.pathSeparator + File.pathSeparator;
            case "newline", "line" -> System.lineSeparator();
            case "space" -> " ";
            case "none", "empty" -> "";
            default -> sep;
        };
    }

    static void configure(SlimeLauncherRunTask task, MinecraftDependencyInternal mcdep, FileCollection runtimeClasspath) {
        var inst = mcdep.getMavenizerInstance();
        task.getRuntimeClasspath().setFrom(runtimeClasspath); // main classpath gets polluted by Slimelauncher so keep a copy
        task.getMinecraftClasspath().setFrom(mcdep.getMinecraftDependencies());
        task.getPatcherModules().setFrom(mcdep.getPatcherModules());
        task.getMinecraftVersion().set(inst.getMinecraftVersion());
        task.getMCPVersion().set(inst.getMCPVersion());
        task.getMappingChannel().set(inst.getMappingChannel());
        task.getMappingVersion().set(inst.getMappingVersion());

        if (Util.isObfuscated(inst.getMinecraftVersion().get())) {
            task.getMcpToObf().fileProvider(inst.getToObfFile());
            task.getMcpToSrg().fileProvider(inst.getToSrgFile());
        }
    }
}
