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

public interface ResultHelper {
    public static final int DEFAULT_QUERY_LIMIT = 100;

    Resource findByAbsolutePathPrefix(ResourceResolver resourceResolver, String path);

    List<Resource> startsWith(ResourceResolver resourceResolver, String path);

    List<Resource> findByPathFragment(ResourceResolver resourceResolver, String pathFragment,
                                     int limit, String... nodeType);

    List<Resource> findByName(ResourceResolver resourceResolver, String path, boolean strict,
                              int limit, String... nodeTypes);
}
