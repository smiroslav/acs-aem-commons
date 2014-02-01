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


import com.adobe.acs.commons.quickly.results.ResultHelper;
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
import java.util.Collection;
import java.util.HashMap;
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
    public Resource findByAbsolutePathPrefix(final ResourceResolver resourceResolver, final String path) {
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
            for(final Resource child : parent.getChildren()) {
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
    public List<Resource> findByPathFragment(final ResourceResolver resourceResolver, final String pathFragment,
                                             final int limit, final String... nodeTypes) {
        final List<Resource> results = new LinkedList<Resource>();

        if(StringUtils.startsWith(pathFragment, "/")) {
            // Only handle things that DONT look like absolute paths
            return results;
        }

        final String[] segments = StringUtils.split(pathFragment, "/");
        if(segments.length == 0) {
            // Empty input; empty results
            return results;
        }

        final List<Resource> fragmentRoots = this.findByNodeName(resourceResolver,
                segments[0], (segments.length > 1), limit, JcrConstants.NT_BASE);

        if(segments.length == 1) {
            // Single path segment; find anything that matches this nodeName*
            return this.getChildrenResults(true, fragmentRoots, null);
        }

        final String relUltimatePath = StringUtils.join(segments, "/", 1, segments.length);

        results.addAll(this.getChildrenResults(true, fragmentRoots, relUltimatePath));
        if(!results.isEmpty()) {
            return results;
        }

        // At least 2 segments; full-node-name/../possible-fragment

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
                    if(this.isValidFragmentResource(fragment, child)) {
                        results.add(child);
                    }
                }
            }
        }

        return results;
    }

    private boolean isValidFragmentResource(final String fragmentSegment, final Resource resource) {

        if(StringUtils.startsWith(resource.getName(), fragmentSegment)) {
            return true;
        }

        return false;
    }


    @Override
    public List<Resource> findByNodeName(final ResourceResolver resourceResolver, final String nodeName,
                                         final boolean strict, final int limit, String... nodeTypes) {
        final List<Resource> results = new LinkedList<Resource>();
        final Map<String, String> map = new HashMap<String, String>();

        if(nodeTypes != null && nodeTypes.length > 0) {
            map.put("group.p.or", "true");

            for(int i = 0; i < nodeTypes.length; i++) {
                map.put("group." + (i + 1) + "_type", nodeTypes[i]);
            }
        }

        if(strict) {
            map.put("nodename", nodeName);
        } else {
            map.put("nodename", nodeName + "*");
        }

        map.put("p.limit", String.valueOf(limit));

        map.put("1_orderby", "@jcr:content/cq:lastModified");
        map.put("2_orderby", "@jcr:content/jcr:lastModified");
        map.put("3_orderby", "@jcr:content/jcr:created");
        map.put("4_orderby", "@jcr:content/jcr:title");

        map.put("1_orderby.sort", "desc");
        map.put("2_orderby.sort", "desc");
        map.put("3_orderby.sort", "desc");
        map.put("4_orderby.sort", "asc");

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
        return !StringUtils.startsWithAny(resource.getPath(), PATH_PREFIX_BLACKLIST);
    }


    public List<Resource> getChildrenResults(final boolean includeParent, final Resource parent,
                                             final String relPath) {
        final List<Resource> results = new LinkedList<Resource>();
        Resource relParent = parent;
        if(!StringUtils.isBlank(relPath)) {
            relParent = parent.getChild(relPath);
        }

        if(relParent == null) {
            return results;
        }

        if(includeParent) {
            results.add(relParent);
        }

        for(final Resource child : relParent.getChildren()) {
            results.add(child);
        }

        return results;
    }

    public List<Resource> getChildrenResults(final boolean includeParents, final Collection<Resource> parents,
                                             final String relPath) {
        final List<Resource> results = new LinkedList<Resource>();

        for(final Resource parent : parents) {
            results.addAll(this.getChildrenResults(includeParents, parent, relPath));
        }

        return results;
    }
}