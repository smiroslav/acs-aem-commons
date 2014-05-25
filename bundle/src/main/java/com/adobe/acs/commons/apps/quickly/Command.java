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

package com.adobe.acs.commons.apps.quickly;

import com.adobe.acs.commons.util.CookieUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import javax.servlet.http.Cookie;

public class Command {
    public enum AuthoringMode {
        TOUCH,
        CLASSIC
    }

    private String raw;
    private String operation;
    private String param;
    private String[] options;
    private AuthoringMode authoringMode = null;

    public Command(final SlingHttpServletRequest request) {
        this(request.getParameter("cmd"));

        final Cookie cookie = CookieUtil.getCookie(request, "cq-authoring-mode");

        if (cookie != null) {
            this.authoringMode = AuthoringMode.valueOf(cookie.getValue());
        }
    }

    public Command(final String raw) {
        this.raw = StringUtils.stripToEmpty(raw);
        this.operation = StringUtils.lowerCase(StringUtils.substringBefore(this.raw, " "));

        if (StringUtils.substringAfter(this.raw, " ").contains(" ")) {
            this.options = StringUtils.substringBeforeLast(StringUtils.substringAfter(this.raw, " "), " ").split(" ");
        } else {
            this.options = new String[0];
        }

        this.param = StringUtils.substringAfterLast(raw, " ");

        // TODO Defaulting to TouchUI
        this.authoringMode = AuthoringMode.TOUCH;

    }

    public final String getOp() {
        return this.operation;
    }

    public final String getParam() {
        return this.param;
    }

    public final String toString() { return this.raw; }

    public final String[] getOptions() { return this.options; }

    public final AuthoringMode getAuthoringMode() { return this.authoringMode; }
}
