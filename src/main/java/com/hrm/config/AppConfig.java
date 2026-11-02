package com.hrm.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable application configuration.
 * <p>
 * Values are read from classpath properties files ({@code db.properties}, {@code app.properties})
 * and may contain placeholders {@code ${NAME:default}}. A placeholder is resolved from the JVM
 * system property {@code NAME}, then the environment variable {@code NAME}, then the default.
 * This keeps credentials out of the repository (NFR-05) while giving sensible local defaults.
 */
public final class AppConfig {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)(?::([^}]*))?}");

    private final Map<String, String> values;

    AppConfig(Properties raw, Function<String, String> lookup) {
        Map<String, String> resolved = new HashMap<>();
        for (String key : raw.stringPropertyNames()) {
            resolved.put(key, resolve(raw.getProperty(key), lookup));
        }
        this.values = Collections.unmodifiableMap(resolved);
    }

    /**
     * Loads and merges the given classpath resources (later files override earlier ones).
     *
     * @throws IllegalStateException if a resource is missing or unreadable
     */
    public static AppConfig load(String... resources) {
        Properties merged = new Properties();
        for (String resource : resources) {
            try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("Configuration resource not found: " + resource);
                }
                merged.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read configuration resource " + resource, e);
            }
        }
        return new AppConfig(merged, AppConfig::systemOrEnv);
    }

    /** Creates a configuration from an in-memory map (tests, CLI tools). */
    public static AppConfig of(Map<String, String> entries) {
        Properties p = new Properties();
        p.putAll(entries);
        return new AppConfig(p, AppConfig::systemOrEnv);
    }

    private static String systemOrEnv(String name) {
        String value = System.getProperty(name);
        return value != null ? value : System.getenv(name);
    }

    static String resolve(String value, Function<String, String> lookup) {
        if (value == null) {
            return null;
        }
        Matcher m = PLACEHOLDER.matcher(value);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String replacement = lookup.apply(m.group(1));
            if (replacement == null) {
                replacement = m.group(2) != null ? m.group(2) : "";
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString().trim();
    }

    public Optional<String> find(String key) {
        String v = values.get(key);
        return v == null || v.isEmpty() ? Optional.empty() : Optional.of(v);
    }

    public String get(String key, String defaultValue) {
        return find(key).orElse(defaultValue);
    }

    /** @throws IllegalStateException when the key is missing or blank */
    public String require(String key) {
        return find(key).orElseThrow(() -> new IllegalStateException("Missing required configuration: " + key));
    }

    public int getInt(String key, int defaultValue) {
        return find(key).map(v -> parse(key, v, Integer::parseInt)).orElse(defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return find(key).map(v -> parse(key, v, Long::parseLong)).orElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return find(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    public LocalTime getTime(String key, LocalTime defaultValue) {
        return find(key).map(v -> parse(key, v, LocalTime::parse)).orElse(defaultValue);
    }

    public ZoneId getZone(String key, ZoneId defaultValue) {
        return find(key).map(v -> parse(key, v, ZoneId::of)).orElse(defaultValue);
    }

    private static <T> T parse(String key, String value, Function<String, T> parser) {
        try {
            return parser.apply(value);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid value for configuration " + key + ": '" + value + "'", e);
        }
    }
}
