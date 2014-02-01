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

package com.adobe.acs.commons.quickly.results;

import com.adobe.acs.commons.quickly.ResultUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;

public class OpenResult extends AbstractResult {
    private static final String[] ACCEPT_PREFIXES = new String[]{"/content", "/etc"};
    private static final String[] REJECT_PATH_SEGMENTS = new String[]{ "/jcr:content", "/rep:policy" };

    public OpenResult(final Resource resource) {

        final String path = resource.getPath();

        this.setPath(path);
        this.setTitle(ResultUtil.getTitle(resource));

        if(ResultUtil.isDAMPath(path)) {

            // DAM Path

            if(ResultUtil.isManuscript(resource)) {
                this.setActionURI(path + ".copyeditor.html");
            } else {
                this.setActionURI("/damadmin#" + path);
            }

            if(!ResultUtil.isAsset(resource)) {
                this.setActionMethod(METHOD_NOOP);
            }


        } else if (ResultUtil.isWCMPath(path)) {

            // WCM Path

            this.setActionURI("/cf#" + path + ".html");

            if(!ResultUtil.isPage(resource)) {
                this.setActionMethod(METHOD_NOOP);
            }

        } else if (StringUtils.startsWith(path, "/etc")) {

            // Tools Path

            this.setActionURI(path + ".html");

            if(!ResultUtil.isPage(resource)) {
                this.setActionMethod(METHOD_NOOP);
            }
        }

        this.setDescription(path);
    }

    public static boolean accepts(final Resource resource) {
        final String path = resource.getPath();

        if(!StringUtils.startsWithAny(path, ACCEPT_PREFIXES)) {
            return false;
        }

        for(final String rejectPathSegment : REJECT_PATH_SEGMENTS) {
            if(StringUtils.contains(path, rejectPathSegment)) {
                return false;
            }
        }

        return true;
    }
}