/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradle.internal;

import net.minecraftforge.gradleutils.shared.SharedUtil;
import net.minecraftforge.srgutils.MinecraftVersion;

import java.io.File;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

final class Util extends SharedUtil {
    private static final Logger LOGGER = Logging.getLogger(Util.class);

    static String checkMappingsParam(ForgeGradleProblems problems, @Nullable Object param, String name) {
        if (param == null || param.toString().isEmpty())
            throw problems.nullMappingsParam(name);

        return param.toString();
    }

    static boolean isPresent(String c) {
        return !c.isBlank();
    }

    static String dependencyToCamelCase(ModuleIdentifier dependency) {
        return dependencyToCamelCase(dependency.getGroup(), dependency.getName());
    }

    static String dependencyToCamelCase(@Nullable String group, String name) {
        var list = new ArrayList<String>(3);

        boolean isForge = "net.minecraftforge".equals(group) && "forge".equals(name);

        if (group != null && !isForge)
            list.addAll(Arrays.asList(group.split("\\.")));

        // TODO: [ForgeGradle] Add version distinction for run task names
        list.add(name);

        var builder = new StringBuilder(64);
        for (var s : list) {
            builder.append(StringGroovyMethods.capitalize(s));
        }
        return builder.toString();
    }

    static @Nullable SourceSet getSourceSet(NamedDomainObjectSet<Configuration> configurations, SourceSetContainer sourceSets, Dependency dependency) {
        for (var sourceSet : sourceSets) {
            if (contains(configurations, sourceSet, false, dependency)) {
                return sourceSet;
            }
        }

        return null;
    }

    /// A token whose value depends on arguments supplied in the template, e.g. {@code {source_roots,separator=path}}.
    ///
    /// The argument map contains every {@code key=value} pair parsed from the token (an argument with no {@code =}
    /// maps to an empty string). Implementations should treat a missing/blank argument as "use the default".
    @FunctionalInterface
    interface Token {
        String resolve(Map<String, String> args);
    }

    /// Resolves the {@code separator} token argument to an actual delimiter string.
    ///
    /// Accepts {@code path} (the platform path separator), {@code newline}/{@code line} (the platform line
    /// separator), {@code space}, {@code none}/{@code empty}, or a literal value. Returns {@code def} when the
    /// argument is absent or blank.
    static String resolveSeparator(Map<String, String> args, String def) {
        var sep = args.get("separator");
        if (sep == null || sep.isBlank())
            return def;
        return switch (sep) {
            case "path" -> File.pathSeparator;
            case "newline", "line" -> System.lineSeparator();
            case "space" -> " ";
            case "none", "empty" -> "";
            default -> sep;
        };
    }

    // Copied straight from FG6
    // Replace tokens in a string that are wrapped in {}
    // Supports escaping {} or \ using \
    // Tokens may carry comma-separated arguments: {name,key=value,flag}. These are parsed into a Map<String,String>
    // (a bare flag maps to "") and passed to any matching Token. The plain {name} form yields an empty argument map.
    static String replaceTokens(Map<String, ?> tokens, String value, @Nullable Set<String> unknown) {
        if (value.length() <= 2 || value.indexOf('{') == -1)
            return value;

        var buf = new StringBuilder();

        for (int x = 0; x < value.length(); x++) {
            char c = value.charAt(x);
            if (c == '\\') {
                if (x == value.length() - 1)
                    throw new IllegalArgumentException("Illegal pattern (Bad escape): " + value);
                buf.append(value.charAt(++x));
            } else if (c == '{' || c ==  '\'') {
                StringBuilder key = new StringBuilder();
                for (int y = x + 1; y <= value.length(); y++) {
                    if (y == value.length())
                        throw new IllegalArgumentException("Illegal pattern (Unclosed " + c + "): " + value);
                    char d = value.charAt(y);
                    if (d == '\\') {
                        if (y == value.length() - 1)
                            throw new IllegalArgumentException("Illegal pattern (Bad escape): " + value);
                        key.append(value.charAt(++y));
                    } else if (c == '{' && d == '}') {
                        //noinspection ReassignedVariable,SuspiciousNameCombination
                        x = y;
                        break;
                    } else if (c == '\'' && d == '\'') {
                        //noinspection ReassignedVariable,SuspiciousNameCombination
                        x = y;
                        break;
                    } else
                        key.append(d);
                }
                if (c == '\'')
                    buf.append(key);
                else {
                    var raw = key.toString();
                    var name = raw;
                    Map<String, String> args = Map.of();
                    var comma = raw.indexOf(',');
                    if (comma >= 0) {
                        name = raw.substring(0, comma);
                        args = parseTokenArgs(raw.substring(comma + 1));
                    }

                    Object v = tokens.get(name);
                    if (v instanceof Token token)
                        v = token.resolve(args);
                    else if (v instanceof Supplier)
                        v = ((Supplier<?>) v).get();

                    if (v == null) {
                        if (unknown != null)
                            unknown.add(name);
                        buf.append('{').append(raw).append('}');
                    } else {
                        buf.append(v);
                    }
                }
            } else {
                buf.append(c);
            }
        }

        return buf.toString();
    }

    private static Map<String, String> parseTokenArgs(String args) {
        var ret = new java.util.LinkedHashMap<String, String>();
        for (var part : args.split(",")) {
            if (part.isBlank()) continue;
            var eq = part.indexOf('=');
            if (eq < 0)
                ret.put(part.trim(), "");
            else
                ret.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
        }
        return ret;
    }

    private static final MinecraftVersion UNOBFED_START = MinecraftVersion.from("26.1-snapshot-1");
    public static boolean isObfuscated(String version) {
        try {
            return MinecraftVersion.from(version).compareTo(UNOBFED_START) < 0;
        } catch (Exception e) {
            LOGGER.info("Failed to parse MC Version: {} Defaulting to not obfuscated", version);
            return false;
        }
    }
}
