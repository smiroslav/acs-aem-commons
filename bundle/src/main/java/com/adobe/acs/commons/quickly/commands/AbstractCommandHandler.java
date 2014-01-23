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

package com.adobe.acs.commons.quickly.commands;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.Result;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(AbstractCommandHandler.class);

    private static String KEY_RESULTS = "results";

    public static final List<Result> EMPTY_RESULTS = new LinkedList<Result>();

    @Override
    public JSONObject getResults(final SlingHttpServletRequest slingRequest, final Command cmd) throws JSONException {
        final JSONObject json = new JSONObject();
        final List<Result> results;

        if (StringUtils.isBlank(cmd.getParam())) {
            results = this.withoutParams(slingRequest, cmd);
        } else {
            results = this.withParams(slingRequest, cmd);
        }

        json.put(KEY_RESULTS, new JSONArray());

        for (final Result result : results) {
            if(result.isValid()) {
                json.accumulate(KEY_RESULTS, result.toJSON());
            }
        }

        return json;
    }

    protected abstract List<Result> withoutParams(final SlingHttpServletRequest slingRequest, final Command cmd);

    protected abstract List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd);
}
