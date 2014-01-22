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

package com.adobe.acs.commons.quickly.commands.impl.open;

import com.adobe.acs.commons.quickly.BasicResult;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;

public class OpenResult extends BasicResult {
    private static final String[] ACCEPT_PREFIXES = new String[]{"/content", "/etc"};

    public OpenResult(final Resource resource) {
        final String path = resource.getPath();

        if (StringUtils.startsWith(path, "/content/dam")) {
            this.setTitle(findAssetTitle(resource));
            this.setActionURI("/damadmin#" + path);
        } else if (StringUtils.startsWith(path, "/content")) {
            this.setTitle(findPageTitle(resource));
            this.setActionURI("/cf#" + path + ".html");
        } else if (StringUtils.startsWith(path, "/etc")) {
            this.setTitle(findPageTitle(resource));
            this.setActionURI(path + ".html");
        }

        this.setDescription(path);
    }

    public static boolean accepts(final Resource resource) {
        final String path = resource.getPath();
        return StringUtils.startsWithAny(path, ACCEPT_PREFIXES);
    }
}