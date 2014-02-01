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
import com.adobe.acs.commons.quickly.results.OpenResult;
import com.adobe.acs.commons.quickly.results.PathBasedResourceFinder;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.search.QueryBuilder;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang.StringUtils;
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
import java.util.Collections;
import java.util.List;

@Component(
        label = "ACS AEM Commons - Quickly - Go Command Handler"
)
@Properties({
        @Property(
                name = "cmd",
                value = OpenCommandHandlerImpl.CMD,
                propertyPrivate = true
        )
})
@Service
public class OpenCommandHandlerImpl extends AbstractCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(OpenCommandHandlerImpl.class);

    public static final String CMD = "open";

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
        return Collections.EMPTY_LIST;
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();

        final List<Result> results = new ArrayList<Result>();

        /** Find Path-based Matching Resources **/

        final List<Resource> resources = pathBasedResourceFinder.findAll(resourceResolver,
                cmd.getParam(),
                PathBasedResourceFinder.DEFAULT_QUERY_LIMIT,
                NameConstants.NT_PAGE, DamConstants.NT_DAM_ASSET);

        /** Accepts **/

        for(final Resource resource : resources) {
            if(OpenResult.accepts(resource)) {
                results.add(new OpenResult(resource));
            }
        }

        return results;
    }
}