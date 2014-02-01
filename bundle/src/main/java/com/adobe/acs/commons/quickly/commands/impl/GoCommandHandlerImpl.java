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

package com.adobe.acs.commons.quickly.commands.impl;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.Result;
import com.adobe.acs.commons.quickly.commands.AbstractCommandHandler;
import com.adobe.acs.commons.quickly.results.GoResult;
import com.adobe.acs.commons.quickly.results.PathBasedResourceFinder;
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

import java.util.ArrayList;
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

    private static List<Result> shortcuts;


    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private PathBasedResourceFinder pathBasedResourceFinder;

    @Override
    public boolean accepts(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return StringUtils.equalsIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return shortcuts;
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final long start = System.currentTimeMillis();

        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();

        final List<Result> results = new ArrayList<Result>();

        /** Find Path-based Matching Resources **/

        final List<Resource> resources = pathBasedResourceFinder.findAll(resourceResolver,
                cmd.getParam(),
                PathBasedResourceFinder.DEFAULT_QUERY_LIMIT,
                NameConstants.NT_PAGE, DamConstants.NT_DAM_ASSET);

        /** Accepts **/

        for (final Resource resource : resources) {
            if(GoResult.accepts(resource)) {
                results.add(new GoResult(resource));
            }
        }

        log.debug("Go w/ Params({}) >> Execution time: {} ms", cmd.getParam(), System.currentTimeMillis() - start);

        return results;
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        shortcuts = new ArrayList<Result>();

        shortcuts.add(new GoResult("wcm",
                "Web page administration",
                "/siteadmin"));

        shortcuts.add(new GoResult("dam",
                "DAM administration",
                "/damadmin"));

        shortcuts.add(new GoResult("tags",
                "Tag administration",
                "/tagging"));

        shortcuts.add(new GoResult("wf",
                "Workflow administration",
                "/libs/cq/workflow/content/console.html"));

        shortcuts.add(new GoResult("tools",
                "AEM Tools",
                "/miscadmin"));

        shortcuts.add(new GoResult("inbox",
                "My Inbox",
                "/inbox"));

        shortcuts.add(new GoResult("users",
                "User and Group administration",
                "/useradmin"));

        shortcuts.add(new GoResult("campaigns",
                "Campaign administration",
                "/mcmadmin"));

        shortcuts.add(new GoResult("soco",
                "Soco administration",
                "/socoadmin"));

        shortcuts.add(new GoResult("publications",
                "DPS administration",
                "/publishingadmin"));

        shortcuts.add(new GoResult("manuscripts",
                "Manuscript administration",
                "/manuscriptsadmin"));

        shortcuts.add(new GoResult("crxde",
                "CRXDE Lite",
                "/crxde"));

        shortcuts.add(new GoResult("pack",
                "CRX package manager",
                "/crx/packmgr"));

        shortcuts.add(new GoResult("touch",
                "Touch UI",
                "/projects.html"));

        shortcuts.add(new GoResult("desktop",
                "Desktop UI",
                "/welcome"));

        /* Felix Console */

        shortcuts.add(new GoResult("system",
                "System Console",
                "/system/console",
                "_blank"));

        shortcuts.add(new GoResult("system/adapters",
                "System Console > Adapters",
                "/system/console/adapters",
                "_blank"));

        shortcuts.add(new GoResult("system/components",
                "System Console > Components",
                "/system/console/components",
                "_blank"));

        shortcuts.add(new GoResult("system/configmgr",
                "System Console > Config manager",
                "/system/console/configMgr",
                "_blank"));

        shortcuts.add(new GoResult("system/depfinder",
                "System Console > Dependency Finder",
                "/system/console/depfinder",
                "_blank"));

        shortcuts.add(new GoResult("system/events",
                "System Console > Events",
                "/system/console/events",
                "_blank"));

        shortcuts.add(new GoResult("system/jmx",
                "System Console > JMX",
                "/system/console/jmx",
                "_blank"));

        shortcuts.add(new GoResult("system/request",
                "System Console > Recent Requests",
                "/system/console/requests",
                "_blank"));
    }
}