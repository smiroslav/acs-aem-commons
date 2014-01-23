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

package com.adobe.acs.commons.quickly.commands.impl.go;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.Result;
import com.adobe.acs.commons.quickly.ResultHelper;
import com.adobe.acs.commons.quickly.commands.AbstractCommandHandler;
import com.adobe.acs.commons.quickly.results.CRXDEResult;
import com.adobe.acs.commons.quickly.results.GoResult;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.search.QueryBuilder;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Quickly - Go Command Handler"
)
@Properties({
        @Property(
                name = "cmd",
                value = GoCommandHandlerImpl.CMD,
                propertyPrivate = true
        )
})
@Service
public class GoCommandHandlerImpl extends AbstractCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(GoCommandHandlerImpl.class);

    public static final String CMD = "go";

    private static List<Result> DEFAULT_RESULTS;


    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private ResultHelper resultHelper;

    @Override
    public boolean accepts(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return StringUtils.equalsIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return DEFAULT_RESULTS;
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();
        final List<Result> results = new LinkedList<Result>();

        for(final Result result : DEFAULT_RESULTS) {
            if(StringUtils.startsWith(result.getTitle(), cmd.getParam())) {
                results.add(result);
            }
        }

        final Resource paramResource = resultHelper.matchFullPath(resourceResolver, cmd.getParam());;
        if(paramResource != null) {
            results.add(new GoResult(paramResource));
        }

        final List<Resource> startsWithResources = resultHelper.startsWith(resourceResolver, cmd.getParam());
        for(final Resource startsWithResource : startsWithResources) {
            if(CRXDEResult.accepts(startsWithResource)) {
                results.add(new GoResult(startsWithResource));
            }
        }

        final List<Resource> resources = resultHelper.findByPathFragment(resourceResolver,
                cmd.getParam(),
                ResultHelper.DEFAULT_QUERY_LIMIT,
                NameConstants.NT_PAGE, DamConstants.NT_DAM_ASSET);

        for (final Resource resource : resources) {
            log.debug("go resource; {}", resource.getPath());

            if(GoResult.accepts(resource)) {
                results.add(new GoResult(resource));
            }
        }

        return results;
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        DEFAULT_RESULTS = new LinkedList<Result>();

        DEFAULT_RESULTS.add(new GoResult("wcm", "Web page administration", "/siteadmin"));
        DEFAULT_RESULTS.add(new GoResult("dam", "DAM administration", "/damadmin"));
        DEFAULT_RESULTS.add(new GoResult("tags", "Tag administration", "/tagging"));
        DEFAULT_RESULTS.add(new GoResult("wf", "Workflow administration", "/libs/cq/workflow/content/console.html"));
        DEFAULT_RESULTS.add(new GoResult("tools", "AEM Tools", "/miscadmin"));
        DEFAULT_RESULTS.add(new GoResult("inbox", "My Inbox", "/inbox"));
        DEFAULT_RESULTS.add(new GoResult("users", "User and Group administration", "/useradmin"));
        DEFAULT_RESULTS.add(new GoResult("campaigns", "Campaign administration", "/mcmadmin"));
        DEFAULT_RESULTS.add(new GoResult("soco", "Soco administration", "/socoadmin"));
        DEFAULT_RESULTS.add(new GoResult("crxde", "CRXDE Lite", "/crxde"));
        DEFAULT_RESULTS.add(new GoResult("pack", "CRX package manager", "/crx/packmgr"));
        DEFAULT_RESULTS.add(new GoResult("touch", "Touch UI", "/projects.html"));
        DEFAULT_RESULTS.add(new GoResult("desktop", "Desktop UI", "/welcome"));
    }
}