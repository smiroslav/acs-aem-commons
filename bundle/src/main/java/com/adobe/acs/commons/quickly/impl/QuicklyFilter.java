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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.adobe.acs.commons.quickly.impl;

import com.adobe.acs.commons.util.BufferingResponse;
import com.adobe.acs.commons.util.ResourceDataUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Quickly App HTML Injection Filter",
        description = "Injects the necessary HTML into the Request page.",
        metatype = false,
        policy = ConfigurationPolicy.OPTIONAL
)
@Properties({
        @Property(
                name = "sling.filter.scope",
                value = "request",
                propertyPrivate = true
        ),
        @Property(
                name = "filter.order",
                intValue = -1000,
                propertyPrivate = true
        )
})
@Service
public class QuicklyFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(QuicklyFilter.class);

    private static final String HTML_FILE = "/apps/acs-commons/components/utilities/quickly/template.html";
    private static final String CSS_INCLUDE = "<link rel=\"stylesheet\" href=\"/apps/acs-commons/components/utilities/quickly/clientlibs/app.css\"/>";
    private static final String JS_INCLUDE = "<script type=\"text/javascript\" src=\"/apps/acs-commons/components/utilities/quickly/clientlibs/app.js\"></script>";
    private static String APP_HTML = "";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof SlingHttpServletRequest)
                || !(servletResponse instanceof SlingHttpServletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) servletRequest;
        final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) servletResponse;

        if(!this.accepts(slingRequest)) {
            filterChain.doFilter(slingRequest, slingResponse);
            return;
        }

        final BufferingResponse capturedResponse = new BufferingResponse(slingResponse);

        filterChain.doFilter(slingRequest, capturedResponse);

        // Get contents
        final String contents = capturedResponse.getContents();

        if(contents != null) {
            if(StringUtils.contains(slingResponse.getContentType(), "html")) {

                final int bodyIndex = contents.indexOf("</body>");
                if (bodyIndex != -1) {

                    log.debug("Quickly injection: {}", slingRequest.getRequestURI());

                    final PrintWriter printWriter = slingResponse.getWriter();
                    //printWriter.write(contents.substring(0, headIndex));
                    //printWriter.write(CSS_INCLUDE);

                    printWriter.write(contents.substring(0, bodyIndex));
                    printWriter.write(APP_HTML);
                    printWriter.write(contents.substring(bodyIndex));
                    return;
                }
            }
        }

        if(contents != null) {
            slingResponse.getWriter().write(contents);
        }
    }

    @Override
    public void destroy() {

    }

    private boolean accepts(final SlingHttpServletRequest slingRequest) {
        final Resource resource = slingRequest.getResource();

        if(StringUtils.startsWith(resource.getPath(), "/system/")) {
            return false;
        }

        return true;
    }

    @Activate
    protected final void activate(final Map<String, String> config) throws IOException, RepositoryException, LoginException {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            APP_HTML = ResourceDataUtil.getNTFileAsString(HTML_FILE, resourceResolver);
        } finally {
            if(resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }
}
