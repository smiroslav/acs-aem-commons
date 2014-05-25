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

import com.adobe.acs.commons.util.TextUtil;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;

import java.util.List;

public class ResultUtil {

    public final static String getTitle(final Resource resource) {

        if(isAsset(resource)) {
            // DAM Assets

            return TextUtil.getFirstNonEmpty(
                    DamUtil.resolveToAsset(resource).getMetadataValue(DamConstants.DC_TITLE),
                    resource.getName());
        } else if (isPage(resource)) {
            // CQ Page

            final Page page = resource.adaptTo(Page.class);

            if(page != null) {
                return TextUtil.getFirstNonEmpty(page.getPageTitle(),
                        page.getTitle(),
                        page.getName());
            }
        }

        // Not DAM Asset OR CQ Pages

        return resource.getName();
    }

    public final static boolean isPage(final Resource resource) {
        return resource.isResourceType(NameConstants.NT_PAGE);
    }

    public final static boolean isAsset(final Resource resource) {
        return DamUtil.isAsset(resource);
    }

    public final static boolean isManuscript(final Resource resource) {
        return isAsset(resource)
                && StringUtils.endsWith(resource.getName(), ".txt");
    }

    public final static boolean isWCMPath(final String path) {
        return StringUtils.startsWith(path, "/content/")
                && !isDAMPath(path)
                && !isUGCPath(path);
    }

    public final static boolean isDAMPath(final String path) {
        return StringUtils.startsWith(path, "/content/dam/");
    }

    public final static boolean isUGCPath(final String path) {
        return StringUtils.startsWith(path, "/content/usergenerated/");
    }

    public final static boolean isToolsPath(final String path) {
        return StringUtils.startsWith(path, "/etc/");
    }

    public final static boolean isNodeType(final Resource resource, final String... nodeTypes) {
        for(final String nodeType : nodeTypes) {
            if(resource.isResourceType(nodeType)) {
                return true;
            }
        }

        return false;
    }

    public final static List<Resource> mergeAndDeDupe(final List<Resource> to, final List<Resource> from) {
        if(to.isEmpty() && !from.isEmpty()) {
            return from;
        } else if (!to.isEmpty() && from.isEmpty())  {
            return to;
        }

        for(final Resource fromResource : from) {
            boolean found = false;
            for(final Resource toResource : to) {
                if(StringUtils.equals(toResource.getPath(), fromResource.getPath())) {
                    found = true;
                    break;
                }
            }

            if(!found) {
                to.add(fromResource);
            }
        }

        return to;
    }

}
