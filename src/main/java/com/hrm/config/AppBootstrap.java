package com.hrm.config;

import com.hrm.web.controller.ErrorPageServlet;
import com.hrm.web.controller.HealthController;
import com.hrm.web.controller.HomeController;
import com.hrm.web.filter.CharacterEncodingFilter;
import com.hrm.web.filter.RequestContextFilter;
import com.hrm.web.filter.SecurityHeadersFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.EnumSet;

/**
 * Application entry point (declared in web.xml). Builds the {@link AppContext} and registers
 * filters and controllers programmatically, which gives
 * <ul>
 *   <li>constructor injection into servlets/filters (no service locator in controllers),</li>
 *   <li>a deterministic filter order (annotation-based filters have no defined order).</li>
 * </ul>
 */
public class AppBootstrap implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppBootstrap.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext sc = event.getServletContext();
        AppConfig config = AppConfig.load("db.properties", "app.properties");
        AppContext app = AppContext.create(config);
        sc.setAttribute(AppContext.ATTRIBUTE, app);
        sc.setAttribute("appName", config.get("app.name", "HRM System"));
        sc.setAttribute("appVersion", config.get("app.version", "dev"));
        sc.getSessionCookieConfig().setHttpOnly(true);
        sc.getSessionCookieConfig().setSecure(config.getBoolean("security.cookieSecure", false));

        registerFilters(sc);
        registerControllers(sc, app);
        log.info("{} {} started (context path '{}')", sc.getAttribute("appName"), sc.getAttribute("appVersion"),
                sc.getContextPath());
    }

    private void registerFilters(ServletContext sc) {
        // order matters: request id/MDC first, so every later log line carries it
        filter(sc, "requestContext", new RequestContextFilter(), "/*");
        filter(sc, "characterEncoding", new CharacterEncodingFilter(), "/*");
        filter(sc, "securityHeaders", new SecurityHeadersFilter(), "/*");
    }

    private void registerControllers(ServletContext sc, AppContext app) {
        servlet(sc, "health", new HealthController(app.jdbc(), app.clock(), app.config().get("app.version", "dev")),
                "/health");
        servlet(sc, "home", new HomeController(), "");
        servlet(sc, "error", new ErrorPageServlet(), "/error");
    }

    private static void filter(ServletContext sc, String name, Filter filter, String... patterns) {
        FilterRegistration.Dynamic reg = sc.addFilter(name, filter);
        reg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, patterns);
    }

    static ServletRegistration.Dynamic servlet(ServletContext sc, String name, HttpServlet servlet, String... mappings) {
        ServletRegistration.Dynamic reg = sc.addServlet(name, servlet);
        reg.addMapping(mappings);
        reg.setLoadOnStartup(1);
        return reg;
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object app = event.getServletContext().getAttribute(AppContext.ATTRIBUTE);
        if (app instanceof AppContext ctx) {
            ctx.close();
        }
        // deregister JDBC drivers loaded by this web app to avoid class-loader leaks on redeploy
        ClassLoader webappLoader = Thread.currentThread().getContextClassLoader();
        for (Driver driver : Collections.list(DriverManager.getDrivers())) {
            if (driver.getClass().getClassLoader() == webappLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (Exception e) {
                    log.warn("Could not deregister JDBC driver {}", driver, e);
                }
            }
        }
        log.info("HRM stopped");
    }
}
