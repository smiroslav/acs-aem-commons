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

package com.adobe.acs.commons.quickly;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

public interface    PathBasedResourceFinder {
    public static final int DEFAULT_QUERY_LIMIT = 100;

    List<Resource> findByAbsolutePath(ResourceResolver resourceResolver, String path);

    List<Resource> findByAbsolutePathPrefix(ResourceResolver resourceResolver, String path);


    List<Resource> findAll(ResourceResolver resourceResolver, String pathFragment,
                          int limit, String... nodeTypes);

    List<Resource> findByPathFragment(ResourceResolver resourceResolver, String pathFragment,
                                     int limit, String... nodeTypes);

    /**
     * Finds all resources whose absolute paths match the provided wildCardFragment
     *
     * * wildCardFragments that begin w "/" will match from resource root
     * * wildCardFragments that don't begin w "/" will match paths that contain the wildCardFragment
     *
     * @param resourceResolver ResourceResolver obj
     * @param wildCardFragment Regex expression to find path fragments
     * @param limit max # of results to return in the JCR query; -1 = all, but can have performance impacts.
     * @param nodeTypes acceptable result resource types
     * @return
     */
    List<Resource> findByWildCardFragment(ResourceResolver resourceResolver,
                                          String wildCardFragment,
                                          int limit, String... nodeTypes);

    /**
     * Finds all resources in the JCR whose node names
     *
     * @param resourceResolver
     * @param path
     * @param strict
     * @param limit
     * @param nodeTypes
     * @return
     */
    List<Resource> findByNodeName(ResourceResolver resourceResolver, String path, boolean strict,
                                  int limit, String... nodeTypes);
}
