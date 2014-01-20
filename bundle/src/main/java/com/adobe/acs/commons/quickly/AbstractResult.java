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
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractResult implements Result {
    private static final Logger log = LoggerFactory.getLogger(AbstractResult.class);

    protected String title;

    protected String description;

    protected String actionURI;

    public String getTitle() {
        return StringUtils.stripToNull(this.title);
    }

    public String getDescription() {
        return StringUtils.stripToNull(this.description);
    }

    protected String getPageTitle(final Resource resource) {
        final PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(resource);

        if(page != null) {
            return TextUtil.getFirstNonEmpty(page.getPageTitle(), page.getTitle(), page.getName());
        } else {
            return resource.getName();
        }
    }

    protected String getAssetTitle(final Resource resource) {
        if(DamUtil.isAsset(resource)) {
            return TextUtil.getFirstNonEmpty(
                    DamUtil.resolveToAsset(resource).getMetadataValue("dc:title"),
                    resource.getName());
        } else {
            return resource.getName();
        }
    }

    public Map<String, String> toMap() throws IllegalStateException {
        final Map<String, String> map = new HashMap<String, String>();

        if (StringUtils.isBlank(this.title)) {
            log.warn("Result title must have a value");
            return map;
        }

        map.put("title", StringUtils.strip(this.title));
        map.put("description", StringUtils.stripToEmpty(this.description));
        map.put("action", StringUtils.stripToEmpty(this.actionURI));

        return map;
    }

    public JSONObject toJSON() throws JSONException {
        return new JSONObject(this.toMap());
    }
}
