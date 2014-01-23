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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
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

    private static final String[] PATH_PREFIX_BLACKLIST = new String[]{
            "/var",
            "/jcr:system",
            "/tmp",
            "/index",
            "/login",
            "/system"
    };

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public Resource matchFullPath(final ResourceResolver resourceResolver, final String path) {
        return resourceResolver.getResource(path);
    }

    @Override
    public List<Resource> startsWith(final ResourceResolver resourceResolver, final String path) {
        List<Resource> results = new LinkedList<Resource>();

        if(!StringUtils.startsWith(path, "/")) {
            // Only handle things that look like absolute paths
            return results;
        }

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
                    if(this.isValidResource(child)) {
                        results.add(child);
                    }
                }
            }
        }

        return results;
    }


    @Override
    public List<Resource> matchPathFragment(final ResourceResolver resourceResolver, final String pathFragment,
                                            final String... nodeTypes) {
        final List<Resource> results = new LinkedList<Resource>();

        final String[] segments = StringUtils.split(pathFragment, "/");
        if(segments.length == 0) {
            // Empty input; empty results
            return results;
        }

        final List<Resource> fragmentRoots = this.matchNodeName(resourceResolver, segments[0], "nt:base");
        if(segments.length == 1) {
            // Single path segment; find anything that matches this nodeName*
            return fragmentRoots;
        }

        // Atleast 2 segments; full-node-name/../possible-fragment

        // Get the middle path segments excluding the first and the last
        // This should be a valid relPath from the any matching 0th resource
        // The last segment could be a fragment though
        final String relPenultimatePath = StringUtils.join(segments, "/", 1, segments.length - 1);

        // The last segment is always considered a fragment
        final String fragment = segments[segments.length - 1];

        for(final Resource fragmentRoot : fragmentRoots) {
            final Resource penultimateResource = fragmentRoot.getChild(relPenultimatePath);

            if(penultimateResource != null) {
                for(final Resource child : penultimateResource.getChildren()) {
                    if(this.isValidFragmentResource(fragment, child, nodeTypes)) {
                        results.add(child);
                    }
                }
            }
        }

        return results;
    }

    private boolean isValidFragmentResource(final String fragmentSegment, final Resource resource,
                                            final String[] nodeTypes) {
        if(StringUtils.startsWith(resource.getName(), fragmentSegment)) {
            if(ArrayUtils.isEmpty(nodeTypes)) {
                return true;
            }

            for(final String nodeType :nodeTypes) {
                if(ResourceUtil.isA(resource, nodeType)) {
                    return true;
                }
            }
        }

        return false;
    }


    @Override
    public List<Resource> matchNodeName(final ResourceResolver resourceResolver, final String nodeName,
                                        String... nodeTypes) {
        final List<Resource> results = new LinkedList<Resource>();
        final Map<String, String> map = new HashMap<String, String>();

        if(nodeTypes != null && nodeTypes.length > 0) {
            map.put("group.p.or", "true");

            for(int i = 0; i < nodeTypes.length; i++) {
                map.put("group." + (i + 1) + "_type", nodeTypes[i]);
            }
        }

        map.put("nodename", nodeName + "*");
        map.put("p.limit", "100");

        final Query query = queryBuilder.createQuery(PredicateGroup.create(map),
                resourceResolver.adaptTo(Session.class));
        final SearchResult result = query.getResult();

        for (final Hit hit : result.getHits()) {
            try {
                final Resource resource = hit.getResource();
                if(this.isValidResource(resource)) {
                    results.add(resource);
                }
            } catch (RepositoryException e) {
                log.error("Could not access repository for hit: {}. Lucene index may be out of sync.", hit);
            }
        }

        return results;
    }


    public boolean isValidResource(final Resource resource) {
        final String path = resource.getPath();

        if(StringUtils.startsWithAny(path, PATH_PREFIX_BLACKLIST)) {
            return false;
        }

        return true;
    }
}