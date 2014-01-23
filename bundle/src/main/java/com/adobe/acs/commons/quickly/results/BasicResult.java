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

import com.adobe.acs.commons.quickly.Result;
import com.adobe.acs.commons.util.TextUtil;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BasicResult implements Result {
    private static final Logger log = LoggerFactory.getLogger(BasicResult.class);

    private static final String DEFAULT_ACTION_URI = "#";
    private static final String DEFAULT_ACTION_METHOD = "get";
    private static final String DEFAULT_ACTION_TARGET = "_top";

    private String title;
    private String description;
    private String actionURI;
    private String actionMethod;
    private String actionTarget;
    private boolean actionAsynchronous = false;
    private Map<String, String> actionParams;

    public BasicResult() {

    }

    public BasicResult(final String title, final String description, final String actionURI) {
        this.setTitle(title);
        this.setDescription(description);
        this.setActionURI(actionURI);
    }

    public String getTitle() {
        return StringUtils.stripToNull(this.title);
    }

    public String getDescription() {
        return StringUtils.stripToNull(this.description);
    }

    @Override
    public String getActionURI() {
        if(StringUtils.isBlank(this.actionURI)) {
            return DEFAULT_ACTION_URI;
        } else {
            return this.actionURI;
        }
    }

    @Override
    public String getActionMethod() {
        if(StringUtils.isBlank(this.actionMethod)) {
            return DEFAULT_ACTION_METHOD;
        } else {
            return this.actionMethod;
        }
    }

    @Override
    public String getActionTarget() {
        if(StringUtils.isBlank(this.actionTarget)) {
            return DEFAULT_ACTION_TARGET;
        } else {
            return this.actionTarget;
        }     }

    @Override
    public Map<String, String> getActionParams() {
        if(actionParams == null) {
            return new HashMap<String, String>();
        } else {
            return actionParams;
        }
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setActionURI(final String actionURI) {
        this.actionURI = actionURI;
    }

    public void setActionMethod(final String actionMethod) {
        this.actionMethod = actionMethod;
    }

    public void setActionTarget(final String actionTarget) {
        this.actionTarget = actionTarget;
    }

    public void setActionAsynchronous(final boolean actionAsynchronous) {
        this.actionAsynchronous = actionAsynchronous;
    }

    public void setActionParams(final Map<String, String> actionParams) {
        this.actionParams = actionParams;
    }

    public boolean isActionAsynchronous() {
        return actionAsynchronous;
    }

    public boolean isValid() {
        return StringUtils.isNotBlank(this.getTitle());
    }

    public JSONObject toJSON() throws JSONException {
        final JSONObject result = new JSONObject();
        final JSONObject action = new JSONObject();
        final JSONObject actionParams = new JSONObject();

        result.put("title", this.getTitle());
        result.put("description", this.getDescription());

        // Action
        action.put("actionURI", this.getActionURI());
        action.put("method", this.getActionMethod());
        action.put("target", this.getActionTarget());
        action.put("xhr", this.isActionAsynchronous());

        for(final Map.Entry<String, String> param : this.getActionParams().entrySet()) {
            actionParams.put(param.getKey(), param.getValue());
        }

        action.put("params", actionParams);
        result.put("action", action);

        return result;
    }

    // Mixin methods

    protected String findPageTitle(final Resource resource) {
        final PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(resource);

        if(page != null) {
            return TextUtil.getFirstNonEmpty(page.getPageTitle(), page.getTitle(), page.getName());
        } else {
            return resource.getName();
        }
    }

    protected String findAssetTitle(final Resource resource) {
        if(DamUtil.isAsset(resource)) {
            return TextUtil.getFirstNonEmpty(
                    DamUtil.resolveToAsset(resource).getMetadataValue(DamConstants.DC_TITLE),
                    resource.getName());
        } else {
            return resource.getName();
        }
    }

    protected boolean isOfNodeType(final Resource resource, final String... nodeTypes) {
        for(final String nodeType :nodeTypes) {
            if(ResourceUtil.isA(resource, nodeType)) {
                return true;
            }
        }

        return false;
    }
}
