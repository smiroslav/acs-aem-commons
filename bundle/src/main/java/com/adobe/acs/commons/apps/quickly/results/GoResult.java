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

package com.adobe.acs.commons.apps.quickly.results;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;

public class GoResult extends AbstractResult {
    private static final String[] ACCEPT_PREFIXES = new String[]{ "/content", "/etc" };

    private static final String[] REJECT_PATH_SEGMENTS = new String[]{ "/jcr:content", "/rep:policy" };

    public GoResult(final String title, final String description, final String actionURI) {
        this(title, description, actionURI, TARGET_TOP);
    }

    public GoResult(final String title, final String description, final String actionURI, final String actionTarget) {
        this.setTitle(title);
        this.setDescription(description);
        this.setActionURI(actionURI);
        this.setActionTarget(actionTarget);
    }

    public GoResult(final Resource resource) {
        final String path = resource.getPath();

        this.setPath(path);
        this.setTitle(ResultUtil.getTitle(resource));

        if (ResultUtil.isDAMPath(path)) {

            this.setActionURI("/damadmin#" + path);

        } else if (ResultUtil.isUGCPath(path)) {

            this.setActionURI("/socoadmin#" + path);

        } else if (ResultUtil.isWCMPath(path)) {

            this.setActionURI("/siteadmin#" + path);

        } else if (ResultUtil.isToolsPath(path)) {

            this.setActionURI("/miscadmin#" + path);
        }

        this.setDescription(path);
    }

    public static boolean accepts(final Resource resource) {
        final String path = resource.getPath();

        if (!StringUtils.startsWithAny(path, ACCEPT_PREFIXES)) {
            return false;
        }

        for (final String rejectPathSegment : REJECT_PATH_SEGMENTS) {
            if (StringUtils.contains(path, rejectPathSegment)) {
                return false;
            }
        }

        return true;
    }
}
