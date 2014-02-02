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


import com.adobe.acs.commons.quickly.ResultUtil;
import com.adobe.acs.commons.quickly.PathBasedResourceFinder;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Service
public class PathBasedResourceFinderImpl implements PathBasedResourceFinder {
    private static final Logger log = LoggerFactory.getLogger(PathBasedResourceFinderImpl.class);

    private static final String[] PATH_PREFIX_BLACKLIST = new String[]{
            "/var",
            "/jcr:system",
            "/tmp",
            "/index",
            "/login",
            "/system"
    };

    private static final int DEFAULT_MIN_NODE_NAME_LOOKUP_LENGTH = 2;
    private int minNodeNameLookupLength = DEFAULT_MIN_NODE_NAME_LOOKUP_LENGTH;
    @Property(label = "Min node name look-up length",
            description = "Minimum number of characters required to allow findByNodeName to search for node name.",
            intValue = DEFAULT_MIN_NODE_NAME_LOOKUP_LENGTH)
    public static final String PROP_MIN_NODE_NAME_LOOKUP_LENGTH = "min-node-name-lookup-length";


    @Reference
    private QueryBuilder queryBuilder;


    @Override
    public List<Resource> findAll(final ResourceResolver resourceResolver, final String pathFragment, final int limit,
                                  final String... nodeTypes) {
        final long start = System.currentTimeMillis();
        List<Resource> resources = new ArrayList<Resource>();

        if(looksLikeAbsolutePath(pathFragment)) {
            /** Looks like an absolute path **/

            /**
             * Look for resource at this exact path; if exists get it and its children
             **/
            resources = ResultUtil.mergeAndDeDupe(resources,
                    this.findByAbsolutePath(resourceResolver, pathFragment));

            /**
             * Absolute path prefix with
             *
             * At this point we don't know if they want the "real" resource, or a resource w the same
             * abs path prefix; so find any of these as well.
             **/
            resources = ResultUtil.mergeAndDeDupe(resources,
                    this.findByAbsolutePathPrefix(resourceResolver, pathFragment));

        } else {
            /** Looks like a path fragment **/

            /** Path Fragment **/

            resources = ResultUtil.mergeAndDeDupe(resources,
                    this.findByPathFragment(resourceResolver,
                            pathFragment,
                            PathBasedResourceFinder.DEFAULT_QUERY_LIMIT,
                            nodeTypes));


            /** WildCard Path Fragment **/

            resources = ResultUtil.mergeAndDeDupe(resources,
                    this.findByWildCardFragment(resourceResolver,
                            pathFragment,
                            PathBasedResourceFinder.DEFAULT_QUERY_LIMIT,
                            nodeTypes));
        }

        log.debug("findAll >> Execution time: {} ms -- {} results.",
                System.currentTimeMillis() - start,
                resources.size());

        return resources;
    }


    @Override
    public List<Resource> findByAbsolutePath(final ResourceResolver resourceResolver, final String path) {
        final long start = System.currentTimeMillis();

        List<Resource> resources = new ArrayList<Resource>();

        final Resource resource =  resourceResolver.getResource(path);

        if(resource != null) {
            // Include parent in resources
            resources = this.getChildren(true, resource, null);
        }

        log.debug("findByAbsolutePath >> Execution time: {} ms -- {} results.",
                System.currentTimeMillis() - start,
                resources.size());
        return resources;
    }

    @Override
    public List<Resource> findByAbsolutePathPrefix(final ResourceResolver resourceResolver, final String path) {
        final long start = System.currentTimeMillis();

        final List<Resource> resources = new ArrayList<Resource>();

        if(!this.looksLikeAbsolutePath(path)) {
            // Only handle things that look like absolute paths
            return resources;
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
                        resources.add(child);
                    }
                }
            }
        }

        log.debug("findByAbsolutePathPrefix >> Execution time: {} ms -- {} results.",
                System.currentTimeMillis() - start,
                resources.size());
        return resources;
    }

    @Override
    public List<Resource> findByPathFragment(final ResourceResolver resourceResolver, final String pathFragment,
                                             final int limit, final String... nodeTypes) {
        final long start = System.currentTimeMillis();

        List<Resource> resources = new ArrayList<Resource>();

        if(this.looksLikeAbsolutePath(pathFragment)) {
            // Only handle things that DONT look like absolute paths
            log.debug("findByPathFragment(1) >> Looks like an absolute path. Immediately returning.");
            return resources;
        }

        final String[] segments = StringUtils.split(pathFragment, "/");
        if(segments.length == 0) {
            // Empty input; empty results
            log.debug("findByPathFragment(2) >> pathFragment is empty. Immediately returning.");
            return resources;
        }

        final List<Resource> fragmentRoots = this.findByNodeName(resourceResolver,
                segments[0], (segments.length > 1), limit, JcrConstants.NT_BASE);

        if(segments.length == 1) {
            // Single path segment; find anything that matches this nodeName*
            resources =  this.getChildren(true, fragmentRoots, null);

            log.debug("findByPathFragment(3) >> Execution time: {} ms -- {} results.",
                    System.currentTimeMillis() - start,
                    resources.size());

            return resources;
        }

        final String relUltimatePath = StringUtils.join(segments, "/", 1, segments.length);

        resources = this.getChildren(true, fragmentRoots, relUltimatePath);

        if(!resources.isEmpty()) {

            log.debug("findByPathFragment(4) >> Execution time: {} ms -- {} results.",
                    System.currentTimeMillis() - start,
                    resources.size());

            return resources;
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
                        resources.add(child);
                    }
                }
            }
        }

        log.debug("findByPathFragment(5) >> Execution time: {} ms -- {} results.",
                System.currentTimeMillis() - start,
                resources.size());

        return resources;
    }

    @Override
    public List<Resource> findByWildCardFragment(final ResourceResolver resourceResolver,
                                                 final String wildCardFragment,
                                             final int limit, final String... nodeTypes) {
        final long start = System.currentTimeMillis();

        List<Resource> resources = new ArrayList<Resource>();

        final String[] segments = StringUtils.split(wildCardFragment, "/");
        if(segments.length == 0) {
            // Empty input; empty results
            return resources;
        }

        final List<Resource> fragmentLeaves = this.findByNodeName(resourceResolver,
                segments[segments.length -1], false, limit, JcrConstants.NT_BASE);

        final Pattern pattern = this.getWildCardFragmentPattern(wildCardFragment);

        for(final Resource fragmentLeaf : fragmentLeaves) {
            final Matcher matcher = pattern.matcher(fragmentLeaf.getPath());

            if(matcher.matches()) {
                // Add leaf and its children
                resources = this.getChildren(true, fragmentLeaf, null);
            }
        }

        log.debug("findByWildCardFragment >> Execution time: {} ms -- {} results.",
                System.currentTimeMillis() - start,
                resources.size());
        return resources;
    }

    @Override
    public List<Resource> findByNodeName(final ResourceResolver resourceResolver, final String nodeName,
                                         final boolean strict, final int limit, String... nodeTypes) {
        final long start = System.currentTimeMillis();

        final List<Resource> resources = new ArrayList<Resource>();
        final Map<String, String> map = new HashMap<String, String>();

        if(!strict && StringUtils.length(nodeName) < minNodeNameLookupLength) {
            return resources;
        }

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

        final Query query = queryBuilder.createQuery(PredicateGroup.create(map),
                resourceResolver.adaptTo(Session.class));
        final SearchResult result = query.getResult();

        for (final Hit hit : result.getHits()) {
            try {
                final Resource resource = hit.getResource();
                log.debug("findNodeByName hit: {}", hit.getResource());
                resources.add(resource);
            } catch (RepositoryException e) {
                log.error("Could not access repository for hit: {}. Lucene index may be out of sync.", hit);
            }
        }

        log.debug("findByNodeName >> Execution time: {} ms -- {} results.",
                System.currentTimeMillis() - start,
                resources.size());

        return resources;
    }

    public List<Resource> getChildren(final boolean includeParent, final Resource parent,
                                      final String relPath) {
        final List<Resource> results = new ArrayList<Resource>();
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

    public List<Resource> getChildren(final boolean includeParents, final Collection<Resource> parents,
                                      final String relPath) {
        final List<Resource> results = new ArrayList<Resource>();

        for(final Resource parent : parents) {
            results.addAll(this.getChildren(includeParents, parent, relPath));
        }

        return results;
    }

    private Pattern getWildCardFragmentPattern(String wildCardFragment) {
        String wildCardRegex = wildCardFragment;

        if(StringUtils.startsWith(wildCardFragment, "/")) {
            wildCardRegex = "^" + wildCardFragment + "([^/]*)";
        } else if (!StringUtils.startsWith(wildCardFragment, "^")) {
            wildCardRegex = "(.*)" + wildCardFragment + "([^/]*)";
        }

        return Pattern.compile(wildCardRegex);
    }

    private boolean isValidResource(final Resource resource) {
        return !StringUtils.startsWithAny(resource.getPath(), PATH_PREFIX_BLACKLIST);
    }

    private boolean isValidFragmentResource(final String fragmentSegment, final Resource resource) {
        if(StringUtils.startsWith(resource.getName(), fragmentSegment)) {
            return true;
        }

        return false;
    }

    private boolean looksLikeAbsolutePath(final String path) {
        return StringUtils.startsWith(path, "/");
    }


    @Activate
    protected void activate(final Map<String, String> config) {
        minNodeNameLookupLength = PropertiesUtil.toInteger(config.get(PROP_MIN_NODE_NAME_LOOKUP_LENGTH),
                DEFAULT_MIN_NODE_NAME_LOOKUP_LENGTH);
    }
}