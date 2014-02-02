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
import com.adobe.acs.commons.quickly.LastModifiedUtil;
import com.adobe.acs.commons.quickly.PathBasedResourceFinder;
import com.adobe.acs.commons.quickly.Result;
import com.adobe.acs.commons.quickly.ResultUtil;
import com.adobe.acs.commons.quickly.commands.AbstractCommandHandler;
import com.adobe.acs.commons.quickly.comparators.LastModifiedComparator;
import com.adobe.acs.commons.quickly.results.InfoResult;
import com.adobe.acs.commons.quickly.results.OpenResult;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private static final int MAX_QUERY_RESULTS = 25;

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
        final List<Result> results = new ArrayList<Result>();

        final InfoResult infoResult = new InfoResult(
                "lastmod [ 1s | 2m | 3h | 4d | 5w | 6M | 7y ]",
                "Defaults to: 1d");

        results.add(infoResult);

        results.addAll(this.withParams(slingRequest, cmd));

        return results;
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final long start = System.currentTimeMillis();

        final List<Result> results = new ArrayList<Result>();
        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();

        final List<Resource> pages = this.getLastModifiedPages(resourceResolver, cmd);
        log.debug("LastModified pages -- [ {} ] results", pages.size());
        final List<Resource> assets = this.getLastModifiedAssets(resourceResolver, cmd);
        log.debug("LastModified assets -- [ {} ] results", assets.size());
        final List<Resource> resources = ResultUtil.mergeAndDeDupe(pages, assets);
        log.debug("LastModified combined resources -- [ {} ] results", resources.size());

        Collections.sort(resources, new LastModifiedComparator());

        for (final Resource resource : resources) {

            final long lastModifiedTimestamp = LastModifiedUtil.getLastModifiedTimestamp(resource);
            final String lastModifiedBy = LastModifiedUtil.getLastModifiedBy(resource);


            final String description = resource.getPath()
                    + " by "
                    + lastModifiedBy
                    + " at "
                    + simpleDateFormat.format(lastModifiedTimestamp);

            final OpenResult openResult = new OpenResult(resource);
            openResult.setDescription(description);
            results.add(openResult);
        }

        log.debug("Lastmod >> Execution time: {} ms",
                System.currentTimeMillis() - start);

        return results;
    }


    private List<Resource> getLastModifiedPages(final ResourceResolver resourceResolver, final Command cmd) {
        final String relativeDateRange = this.getRelativeDateRangeLowerBound(cmd);

        return this.getLastModifiedQuery(resourceResolver, relativeDateRange,
                NameConstants.NT_PAGE,  "@jcr:content/" + NameConstants.PN_LAST_MOD, MAX_QUERY_RESULTS);
    }

    private List<Resource> getLastModifiedAssets(final ResourceResolver resourceResolver, final Command cmd) {
        final String relativeDateRange = this.getRelativeDateRangeLowerBound(cmd);

        return this.getLastModifiedQuery(resourceResolver, relativeDateRange,
                DamConstants.NT_DAM_ASSET,  "@jcr:content/" + JcrConstants.JCR_LASTMODIFIED, MAX_QUERY_RESULTS);
    }

    private List<Resource> getLastModifiedQuery(final ResourceResolver resourceResolver,
                                                final String relativeDateRange,
                                           final String nodeType, final String dateProperty, final int limit) {

        final List<Resource> resources = new ArrayList<Resource>();
        final Map<String, String> map = new HashMap<String, String>();

        map.put("path", "/content");
        map.put("type", nodeType);

        map.put("relativedaterange.property", dateProperty);
        map.put("relativedaterange.lowerBound", relativeDateRange);

        map.put("orderby", dateProperty);
        map.put("orderby.sort", "desc");

        map.put("p.limit", String.valueOf(limit));

        final Query query = queryBuilder.createQuery(PredicateGroup.create(map),
                resourceResolver.adaptTo(Session.class));
        final SearchResult result = query.getResult();

        for (final Hit hit : result.getHits()) {
            try {
                resources.add(hit.getResource());
            } catch (RepositoryException e) {
                log.error("Error resolving Hit to Resource [ {} ]. "
                        + "Likely issue with lucene index being out of sync.", hit.toString());
            }
        }

        return resources;
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