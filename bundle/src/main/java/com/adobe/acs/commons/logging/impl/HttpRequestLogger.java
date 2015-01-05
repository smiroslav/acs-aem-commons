/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.adobe.acs.commons.logging.impl;

import com.adobe.acs.commons.requestwrappers.MultiReadHttpServletRequest;
import com.adobe.acs.commons.util.OsgiPropertyUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(
        label = "ACS AEM Commons - HTTP Request Logger",
        description = "Logs data about incoming HTTP Requests to AEM",
        metatype = true,
        policy = ConfigurationPolicy.REQUIRE
)
public class HttpRequestLogger implements Filter {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestLogger.class);

    private static final String LINE = StringUtils.repeat("-", 80);

    private static final boolean DEFAULT_ENABLED = true;
    private boolean enabled = DEFAULT_ENABLED;
    @Property(label = "Enabled",
            boolValue = DEFAULT_ENABLED)
    public static final String PROP_ENABLED = "enabled";

    private static final boolean DEFAULT_SHOW_RAW = false;
    private boolean showRaw = DEFAULT_SHOW_RAW;
    @Property(label = "Show Raw",
            boolValue = DEFAULT_SHOW_RAW)
    public static final String PROP_SHOW_RAW = "show-raw";

    private static final String[] DEFAULT_ACCEPT_HEADERS = new String[]{};
    private Map<String, Pattern> acceptHeaders = new HashMap<String, Pattern>();
    @Property(label = "Accept Header patterns",
            value = {})
    public static final String PROP_ACCEPT_HEADERS = "accept.headers";


    private static final String[] DEFAULT_ACCEPT_PATHS = new String[]{"/"};
    private String[] acceptPaths = DEFAULT_ACCEPT_PATHS;
    @Property(label = "Accept Header patterns",
            value = {"/"})
    public static final String PROP_ACCEPT_PATHS = "accept.paths";

    private static final String[] DEFAULT_BLACKLIST_HEADERS = new String[]{"Authorization"};
    private String[] blacklistHeaders = DEFAULT_BLACKLIST_HEADERS;
    @Property(label = "Blacklist Headers",
            value = {"Authorization"})
    public static final String PROP_BLACKLIST_HEADERS = "blacklist.headers";

    private static final String[] DEFAULT_BLACKLIST_PARAMETERS = new String[]{"j_password", "password"};
    private String[] blacklistParameters = DEFAULT_BLACKLIST_PARAMETERS;
    @Property(label = "Blacklist Params",
            value = {"j_password", "password"})
    public static final String PROP_BLACKLIST_PARAMETERS = "blacklist.parameters";

    private ServiceRegistration filterRegistration;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public final void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {

        if (servletRequest instanceof HttpServletRequest) {

            final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

            if (accepts(httpServletRequest)) {

                // Use the Multi-Read Http Servlet Request wrapper as this Filter (can) read
                // POST parameters which will make them inaccessible further down the filter chain
                final MultiReadHttpServletRequest request =
                        new MultiReadHttpServletRequest((HttpServletRequest) servletRequest);

                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);

                pw.println();

                pw.println(LINE);

                pw.printf("[ %s ] %s", request.getMethod().toUpperCase(), request.getRequestURI()).println();
                pw.println(request.getRequestURL().toString());

                printHeaders(request, pw);
                printParameters(request, pw);
                printMiscRequestInfo(request, pw);

                if (this.showRaw) {
                    printRaw(request, pw);
                }

                pw.println(LINE);

                log.trace(sw.toString());

                // Call the filter chain w the Multi-Readable Http Servlet Request
                filterChain.doFilter(request, servletResponse);
                return;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void printMiscRequestInfo(HttpServletRequest request, PrintWriter pw) {
        pw.println(LINE);

        pw.printf("Protocol: %s", request.getProtocol()).println();

        pw.printf("Method: %s", request.getMethod()).println();

        pw.printf("Scheme: %s", request.getScheme()).println();

        pw.printf("Server Name: %s", request.getServerName()).println();

        pw.printf("Server Port: %s", request.getServerPort()).println();

        pw.printf("Context Path: %s", request.getContextPath()).println();

        pw.printf("Path Info: %s", request.getPathInfo()).println();

        pw.printf("Path Translated: %s", request.getPathTranslated()).println();

        pw.printf("Query String: %s", request.getQueryString()).println();

        pw.printf("Auth Type: %s", request.getAuthType()).println();

        pw.printf("Character Encoding: %s", request.getCharacterEncoding()).println();

        pw.printf("Content Length: %s", request.getContentLength()).println();

        pw.printf("Servlet Path: %s", request.getServletPath()).println();

        pw.printf("Content Type: %s", request.getContentType()).println();

        if (request.getLocale() != null) {
            pw.printf("Locale: %s", request.getLocale().getDisplayName()).println();
        }

        pw.printf("Local Address: %s", request.getLocalAddr()).println();

        pw.printf("Local Port: %s", request.getLocalPort()).println();

        pw.printf("Remote Address: %s", request.getRemoteAddr()).println();

        pw.printf("Remote Host: %s", request.getRemoteHost()).println();

        pw.printf("Remote Port: %s", request.getRemotePort()).println();

        pw.printf("Remote User: %s", request.getRemoteUser()).println();
    }

    private void printParameters(HttpServletRequest request, PrintWriter pw) {
        // If super.getInputStream() is called before super.getParameterMap() is called,
        // then super.getParameterMap() is empty (there is no open IS left to populate it)

        if (request.getParameterMap().size() > 0) {
            pw.println(LINE);
            pw.println("> REQUEST PARAMETERS");
            pw.println();

            for (final String key : (Set<String>) request.getParameterMap().keySet()) {
                if (ArrayUtils.contains(blacklistParameters, key)) {
                    pw.printf("%s: %s", key, "**********");
                } else {
                    pw.printf("%s: %s", key, Arrays.toString(request.getParameterValues(key)));
                }

                pw.println();
            }
        }
    }

    private void printHeaders(HttpServletRequest request, PrintWriter pw) {
        if (request.getHeaderNames().hasMoreElements()) {
            pw.println(LINE);
            pw.println("> REQUEST HEADERS");
            pw.println();

            for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements();) {
                final String header = headers.nextElement();

                if (ArrayUtils.contains(blacklistHeaders, header)) {
                    pw.printf("%s: %s", header, "**********");
                } else {
                    pw.printf("%s: %s", header, request.getHeader(header));
                }
                pw.println();
            }
        }
    }


    private void printRaw(HttpServletRequest request, PrintWriter pw) {
        pw.println(LINE);
        pw.println("> RAW REQUEST");
        pw.println();

        try {
            pw.println(IOUtils.toString(request.getInputStream()));
        } catch (IOException e) {
            pw.printf("Error reading the request's InputStream: %s", e.getMessage());
        }
    }


    @Override
    public void destroy() {

    }

    private boolean accepts(final HttpServletRequest request) {
        // Perform fastest checks first

        // Enabled state
        if (!this.enabled) {
            return false;
        }

        // Paths
        if (ArrayUtils.isNotEmpty(acceptPaths)
                && !StringUtils.startsWithAny(request.getRequestURI(), acceptPaths)) {
            return false;
        }

        // Headers
        if (!acceptHeaders.isEmpty()) {
            boolean foundMatchingHeader = false;

            for (Map.Entry<String, Pattern> entry : acceptHeaders.entrySet()) {
                final String headerValue = request.getHeader(entry.getKey());

                if (StringUtils.isNotBlank(headerValue)) {
                    final Matcher matcher = entry.getValue().matcher(headerValue);
                    if (matcher.matches()) {
                        foundMatchingHeader = true;
                        break;
                    }
                }
            }

            if (!foundMatchingHeader) {
                return false;
            }
        }

        return true;

    }

    @Activate
    protected final void activate(final ComponentContext ctx) {
        Dictionary<?, ?> config = ctx.getProperties();

        // Enabled
        enabled = PropertiesUtil.toBoolean(config.get(PROP_ENABLED), DEFAULT_ENABLED);

        // Enabled
        showRaw = PropertiesUtil.toBoolean(config.get(PROP_SHOW_RAW), DEFAULT_SHOW_RAW);

        // Accept Paths
        acceptPaths = PropertiesUtil.toStringArray(config.get(PROP_ACCEPT_PATHS), DEFAULT_ACCEPT_PATHS);

        // Accept Headers
        Map<String, String> tmp =
                OsgiPropertyUtil.toMap(PropertiesUtil.toStringArray(
                        config.get(PROP_ACCEPT_HEADERS), DEFAULT_ACCEPT_HEADERS), "=");

        for (Map.Entry<String, String> entry : tmp.entrySet()) {
            final Pattern pattern = Pattern.compile(entry.getValue());
            acceptHeaders.put(entry.getKey(), pattern);
        }

        // Blacklist Headers
        blacklistHeaders = PropertiesUtil.toStringArray(config.get(PROP_BLACKLIST_HEADERS),
                DEFAULT_BLACKLIST_HEADERS);

        // Blacklist Parameters
        blacklistParameters = PropertiesUtil.toStringArray(config.get(PROP_BLACKLIST_PARAMETERS),
                DEFAULT_BLACKLIST_PARAMETERS);

        if (enabled) {
            Dictionary<String, String> filterProps = new Hashtable<String, String>();
            filterProps.put(Constants.SERVICE_RANKING, String.valueOf(Integer.MIN_VALUE));
            filterProps.put("pattern", ".*");
            filterRegistration = ctx.getBundleContext().registerService(Filter.class.getName(), this, filterProps);
        }

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        pw.println();
        pw.printf("Enabled: %s", enabled).println();
        pw.printf("Show Raw: %s", showRaw).println();
        pw.printf("Path Prefixes: %s", Arrays.toString(acceptPaths)).println();

        for (final Map.Entry<String, Pattern> entry : acceptHeaders.entrySet()) {
            pw.printf("Accept Header ~> %s: %s", entry.getKey(), entry.getValue().pattern());
        }

        pw.printf("Blacklist Headers: %s", Arrays.toString(blacklistHeaders)).println();
        pw.printf("Blacklist Parameters: %s", Arrays.toString(blacklistParameters)).println();

        log.info(sw.toString());
    }

    @Deactivate
    protected final void deactivate(final Map<String, String> config) {
        if (filterRegistration != null) {
            filterRegistration.unregister();
            filterRegistration = null;
        }

        enabled = false;
        acceptHeaders.clear();
        acceptPaths = DEFAULT_ACCEPT_PATHS;
        blacklistParameters = DEFAULT_BLACKLIST_PARAMETERS;
        blacklistHeaders = DEFAULT_BLACKLIST_HEADERS;
    }
}