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

package com.adobe.acs.commons.apps.quickly.operations.impl;

import com.adobe.acs.commons.apps.quickly.Command;
import com.adobe.acs.commons.apps.quickly.results.Result;
import com.adobe.acs.commons.apps.quickly.operations.AbstractOperation;
import com.adobe.acs.commons.apps.quickly.results.BasicResult;
import com.adobe.acs.commons.util.CookieUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

@Component(
        label = "ACS AEM Commons - Quickly - Back Operation"
)
@Properties({
        @Property(
                name = "cmd",
                value = BackOperationImpl.CMD,
                propertyPrivate = true
        )
})
@Service
public class BackOperationImpl extends AbstractOperation {
    private static final Logger log = LoggerFactory.getLogger(BackOperationImpl.class);

    public static final String CMD = "back";
    public static final String COOKIE_NAME = "acs_quickly_back";
    private static final String KEY_TITLE = "title";
    private static final String KEY_PATH = "uri";

    @Override
    public final boolean accepts(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return StringUtils.endsWithIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    protected final List<Result> withoutParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final List<Result> results = new ArrayList<Result>();
        final Cookie cookie = CookieUtil.getCookie(slingRequest, COOKIE_NAME);

        if (cookie == null || StringUtils.isBlank(cookie.getValue())) {
            return Collections.EMPTY_LIST;
        }

        try {
            final String cookieValue = URLDecoder.decode(cookie.getValue(), "UTF-8");

            final JSONArray history = new JSONArray(cookieValue);

            for (int i = 0; i < history.length(); i++) {
                final JSONObject entry = history.getJSONObject(i);

                results.add(new BasicResult(
                        entry.optString(KEY_TITLE),
                        entry.optString(KEY_PATH),
                        entry.optString(KEY_PATH)));

            }
        } catch (JSONException e) {
            log.error(e.getMessage());
            return Collections.EMPTY_LIST;
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
            return Collections.EMPTY_LIST;
        }

        return results;
    }

    @Override
    protected final List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return this.withoutParams(slingRequest, cmd);
    }
}