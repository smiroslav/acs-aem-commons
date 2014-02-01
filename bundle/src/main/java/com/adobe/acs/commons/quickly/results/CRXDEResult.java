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

import org.apache.sling.api.resource.Resource;

public class CRXDEResult extends AbstractResult {
    public CRXDEResult() {
        this.setTitle("CRXDE Lite");
        this.setDescription("/crx/de/index.jsp");
        this.setActionURI("/crx/de/index.jsp");
        this.setActionTarget(TARGET_BLANK);
    }

    public CRXDEResult(final Resource resource) {
        this.setPath(resource.getPath());
        this.setTitle(resource.getName());
        this.setDescription(resource.getPath());
        this.setActionURI("/crx/de/index.jsp#" + resource.getPath());
        this.setActionTarget(TARGET_BLANK);
    }

    public static boolean accepts(final Resource resource) {
        return resource != null;
    }
}