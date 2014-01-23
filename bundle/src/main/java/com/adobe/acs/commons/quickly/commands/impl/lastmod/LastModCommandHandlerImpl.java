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

package com.adobe.acs.commons.quickly.commands.impl.lastmod;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.Result;
import com.adobe.acs.commons.quickly.ResultHelper;
import com.adobe.acs.commons.quickly.commands.AbstractCommandHandler;
import com.adobe.acs.commons.quickly.results.BasicResult;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Quickly - Go Command Handler"
)
@Properties({
        @Property(
                name = "cmd",
                value = LastModCommandHandlerImpl.CMD,
                propertyPrivate = true
        )
})
@Service
public class LastModCommandHandlerImpl extends AbstractCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(LastModCommandHandlerImpl.class);

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy @ hh:mm aaa");

    public static final String CMD = "lastmod";

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
        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();

        final List<Result> results = new LinkedList<Result>();
        final Map<String, String> map = new HashMap<String, String>();

        map.put("type", "cq:Page");
        map.put("path", "/content");
        map.put("orderby", "@jcr:content/cq:lastModified");
        map.put("orderby.sort", "desc");
        map.put("p.limit", "50");

        final Query query = queryBuilder.createQuery(PredicateGroup.create(map),
                resourceResolver.adaptTo(Session.class));
        final SearchResult result = query.getResult();

        for (final Hit hit : result.getHits()) {
            try {
                final Resource resource = hit.getResource();
                final ValueMap properties = hit.getProperties();

                // Modified by User
                final String modifiedBy =
                        properties.get("cq:lastModifiedBy", properties.get("jcr:createdBy", "unknown"));

                // Modified by Date and Time
                final Calendar modifiedAt = properties.get("cq:lastModified", properties.get("jcr:created",
                        Calendar.class));
                String modifiedAtStr = "unknown";
                if(modifiedAt != null) {
                    modifiedAtStr = simpleDateFormat.format(modifiedAt.getTime());
                }

                final String description = resource.getPath()
                        + " by "
                        + modifiedBy
                        + " at "
                        + modifiedAtStr;

                results.add(new BasicResult(hit.getTitle(), description, resource.getPath()));
            } catch (RepositoryException e) {
                log.error("Could not access repository for hit: {}. Lucene index may be out of sync.", hit);
            }
        }

        return results;
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return this.withoutParams(slingRequest, cmd);
    }
}