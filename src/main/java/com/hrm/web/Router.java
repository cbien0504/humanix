package com.hrm.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal method + path router used inside each controller servlet, e.g.
 * <pre>{@code
 * r.get("/", this::list).get("/{id}", this::detail).post("/{id}/approve", this::approve);
 * }</pre>
 * Paths are relative to the servlet mapping ({@code request.getPathInfo()}); {@code {name}}
 * segments become path variables.
 */
public final class Router {

    private static final Pattern VARIABLE = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9]*)}");

    private record Route(String method, Pattern regex, List<String> variables, Handler handler) {
    }

    /** A matched route with its extracted path variables. */
    public record Match(Handler handler, Map<String, String> pathVariables) {
    }

    private final List<Route> routes = new ArrayList<>();

    public Router get(String path, Handler handler) {
        return add("GET", path, handler);
    }

    public Router post(String path, Handler handler) {
        return add("POST", path, handler);
    }

    public Router put(String path, Handler handler) {
        return add("PUT", path, handler);
    }

    public Router delete(String path, Handler handler) {
        return add("DELETE", path, handler);
    }

    public Router add(String method, String path, Handler handler) {
        List<String> variables = new ArrayList<>();
        Matcher m = VARIABLE.matcher(path);
        StringBuilder regex = new StringBuilder("^");
        int last = 0;
        while (m.find()) {
            regex.append(Pattern.quote(path.substring(last, m.start()))).append("([^/]+)");
            variables.add(m.group(1));
            last = m.end();
        }
        regex.append(Pattern.quote(path.substring(last))).append('$');
        routes.add(new Route(method, Pattern.compile(regex.toString()), variables, handler));
        return this;
    }

    public Optional<Match> match(String method, String path) {
        String normalized = normalize(path);
        for (Route route : routes) {
            if (!route.method().equals(method)) {
                continue;
            }
            Matcher m = route.regex().matcher(normalized);
            if (m.matches()) {
                Map<String, String> vars = new LinkedHashMap<>();
                for (int i = 0; i < route.variables().size(); i++) {
                    vars.put(route.variables().get(i), m.group(i + 1));
                }
                return Optional.of(new Match(route.handler(), vars));
            }
        }
        return Optional.empty();
    }

    /** True when some route matches the path with another HTTP method (→ 405 instead of 404). */
    public boolean matchesOtherMethod(String path) {
        String normalized = normalize(path);
        return routes.stream().anyMatch(r -> r.regex().matcher(normalized).matches());
    }

    static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
