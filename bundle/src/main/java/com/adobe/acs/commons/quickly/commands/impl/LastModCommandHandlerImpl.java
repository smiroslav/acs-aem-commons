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
import java.util.ArrayList;
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
    private PathBasedResourceFinder pathBasedResourceFinder;

    @Override
    public boolean accepts(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return StringUtils.equalsIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return this.withParams(slingRequest, cmd);
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();

        final List<Result> results = new ArrayList<Result>();
        final Map<String, String> map = new HashMap<String, String>();

        final String relativeDateRange = this.getRelativeDateRangeLowerBound(cmd);

        map.put("10_group.p.or", "true");

        map.put("10_group.11_group.p.or", "false");
        map.put("10_group.11_group.1_type", "cq:Page");
        map.put("10_group.11_group.2_relativedaterange.property", "@jcr:content/cq:lastModified");
        map.put("10_group.11_group.2_relativedaterange.lowerBound", relativeDateRange);

        map.put("10_group.12_group.p.or", "false");
        map.put("10_group.12_group.1_type", "dam:Asset");
        map.put("10_group.12_group.2_relativedaterange.property", "@jcr:content/jcr:lastModified");
        map.put("10_group.12_group.2_relativedaterange.lowerBound", relativeDateRange);

        map.put("path", "/content");

        map.put("1_orderby", "@jcr:content/cq:lastModified");
        map.put("2_orderby", "@jcr:content/jcr:lastModified");

        map.put("1_orderby.sort", "desc");
        map.put("2_orderby.sort", "desc");

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

                // Page last modified
                final Calendar cqLastModified = properties.get("cq:lastModified", Calendar.class);
                // DAM Asset last modified
                final Calendar jcrLastModified = properties.get("jcr:lastModified", Calendar.class);

                // Normalized value
                Calendar lastModified = null;

                if(cqLastModified != null && jcrLastModified == null) {
                    lastModified = cqLastModified;
                } else if(cqLastModified == null && jcrLastModified != null) {
                    lastModified = cqLastModified;
                } else if(cqLastModified != null && jcrLastModified != null) {
                    if(cqLastModified.after(jcrLastModified)) {
                        lastModified = cqLastModified;
                    } else {
                        lastModified = jcrLastModified;
                    }
                }

                String modifiedAtStr = "unknown";
                if(lastModified != null) {
                    modifiedAtStr = simpleDateFormat.format(lastModified.getTime());
                }

                final String description = resource.getPath()
                        + " by "
                        + modifiedBy
                        + " at "
                        + modifiedAtStr;

                final OpenResult openResult = new OpenResult(hit.getResource());
                openResult.setDescription(description);
                results.add(openResult);
            } catch (RepositoryException e) {
                log.error("Could not access repository for hit: {}. Lucene index may be out of sync.", hit);
            }
        }

        return results;
    }

    private String getRelativeDateRangeLowerBound(final Command cmd) {
        final String defaultParam = "-1d";
        final String param = StringUtils.stripToNull(cmd.getParam());

        if(StringUtils.isNotBlank(param)
            && param.matches("\\d+[s|m|h|d|w|M|y]{1}")) {
            return "-" + param;
        }

        return defaultParam;
    }
}