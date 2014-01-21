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


import com.adobe.acs.commons.quickly.ResultHelper;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
@Service
public class ResultHelperImpl implements ResultHelper {
    private static final Logger log = LoggerFactory.getLogger(ResultHelperImpl.class);

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public Resource matchFullPath(final ResourceResolver resourceResolver, final String path) {
        return resourceResolver.getResource(path);
    }

    @Override
    public List<Resource> startsWith(final ResourceResolver resourceResolver, final String path) {
        List<Resource> results = new LinkedList<Resource>();

        Resource parent = resourceResolver.getResource(path);
        if (parent == null) {
            String parentPath = StringUtils.substringBeforeLast(path, "/");
            if(StringUtils.isBlank(parentPath)) {
                parentPath = "/";
            }
            parent = resourceResolver.getResource(parentPath);
        }

        if (parent != null) {
            final Iterator<Resource> children = parent.listChildren();
            while (children.hasNext()) {
                final Resource child = children.next();
                if (StringUtils.startsWith(child.getPath(), path)) {
                    results.add(child);
                }
            }
        }

        return results;
    }

    @Override
    public List<Resource> matchNodeName(final ResourceResolver resourceResolver, final String path, String nodeType,
                                        int limit) {
        final List<Resource> results = new LinkedList<Resource>();
        final Map<String, String> map = new HashMap<String, String>();

        if(nodeType == null) {
            nodeType = JcrConstants.NT_BASE;
        }

        map.put("type", nodeType);
        map.put("nodename", path + "*");
        map.put("p.limit", String.valueOf(limit));

        final Query query = queryBuilder.createQuery(PredicateGroup.create(map),
                resourceResolver.adaptTo(Session.class));
        final SearchResult result = query.getResult();

        for (final Hit hit : result.getHits()) {
            try {
                results.add(hit.getResource());
            } catch (RepositoryException e) {
                log.error("Could not access repository for hit: {}. Lucene index may be out of sync.", hit);
            }
        }

        return results;
    }
}