/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
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
import java.util.Enumeration;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - HTTP Request Logger",
        description = "Logs data about incoming HTTP Requests to AEM",
        metatype = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(
                name = Constants.SERVICE_RANKING,
                intValue = Integer.MIN_VALUE,
                propertyPrivate = true
        ),
        @Property(
                label = "Path pattern",
                name = "pattern",
                value = ".*",
                propertyPrivate = false
        )
})
@Service
public class HttpRequestLogger implements Filter {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestLogger.class);

    private static final String LINE =
            "----------------------------------------------------------------------------------";

    private static final boolean DEFAULT_ENABLED = true;
    private boolean enabled = DEFAULT_ENABLED;
    @Property(label = "Enabled",
            boolValue = DEFAULT_ENABLED)
    public static final String PROP_ENABLED = "enabled";

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public final void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) servletRequest;

        if (accepts(request)) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);

            pw.println();

            pw.println(LINE);

            pw.printf("[ %s ] %s", request.getMethod().toUpperCase(), request.getRequestURI());
            pw.println();
            pw.println(request.getRequestURL().toString());

            printHeaders(request, pw);
            printParameters(request, pw);
            printMiscRequestInfo(request, pw);

            pw.println(LINE);

            log.trace(sw.toString());
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void printMiscRequestInfo(HttpServletRequest request, PrintWriter pw) {
        pw.println(LINE);

        pw.printf("Protocol: %s", request.getProtocol());
        pw.println();

        pw.printf("Method: %s", request.getMethod());
        pw.println();

        pw.printf("Scheme: %s", request.getScheme());
        pw.println();

        pw.printf("Server Name: %s", request.getServerName());
        pw.println();

        pw.printf("Server Port: %s", request.getServerPort());
        pw.println();

        pw.printf("Context Path: %s", request.getContextPath());
        pw.println();

        pw.printf("Path Info: %s", request.getPathInfo());
        pw.println();

        pw.printf("Path Translated: %s", request.getPathTranslated());
        pw.println();

        pw.printf("Query String: %s", request.getQueryString());
        pw.println();

        pw.printf("Auth Type: %s", request.getAuthType());
        pw.println();

        pw.printf("Character Encoding: %s", request.getCharacterEncoding());
        pw.println();

        pw.printf("Content Length: %s", request.getContentLength());
        pw.println();

        pw.printf("Servlet Path: %s", request.getServletPath());
        pw.println();

        pw.printf("Content Type: %s", request.getContentType());
        pw.println();

        if (request.getLocale() != null) {
            pw.printf("Locale: %s", request.getLocale().getDisplayName());
            pw.println();
        }

        pw.printf("Local Address: %s", request.getLocalAddr());
        pw.println();

        pw.printf("Local Port: %s", request.getLocalPort());
        pw.println();

        pw.printf("Remote Address: %s", request.getRemoteAddr());
        pw.println();

        pw.printf("Remote Host: %s", request.getRemoteHost());
        pw.println();

        pw.printf("Remote Port: %s", request.getRemotePort());
        pw.println();

        pw.printf("Remote User: %s", request.getRemoteUser());
        pw.println();
    }

    private void printParameters(HttpServletRequest request, PrintWriter pw) {
        if (request.getParameterNames().hasMoreElements()) {

            pw.println(LINE);
            pw.println("> REQUEST PARAMETERS");
            pw.println();

            for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
                final String param = params.nextElement();
                pw.printf("%s: %s", param, request.getParameter(param));
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
                pw.printf("%s: %s", header, request.getHeader(header));
                pw.println();
            }
        }
    }

    @Override
    public void destroy() {

    }

    private boolean accepts(final HttpServletRequest request) {
        return this.enabled && log.isTraceEnabled();
    }

    @Activate
    protected final void activate(final Map<String, String> config) {
        enabled = PropertiesUtil.toBoolean(config.get(PROP_ENABLED), DEFAULT_ENABLED);

        log.info("Enabled: {}", enabled);
    }
}
