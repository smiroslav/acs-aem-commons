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

package com.adobe.acs.commons.quickly.commands.impl.docs;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.Result;
import com.adobe.acs.commons.quickly.commands.AbstractCommandHandler;
import com.adobe.acs.commons.quickly.results.BasicResult;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@Component(
        label = "ACS AEM Commons - Quickly - Docs Command Handler"
)
@Properties({
        @Property(
                name = "cmd",
                value = DocsCommandHandlerImpl.CMD,
                propertyPrivate = true
        )
})
@Service
public class DocsCommandHandlerImpl extends AbstractCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(DocsCommandHandlerImpl.class);

    public static final String CMD = "docs";

    @Override
    public boolean accepts(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return StringUtils.endsWithIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final List<Result> results = new LinkedList<Result>();

        final BasicResult result = new BasicResult("dev.day.com",
                "dev.day.com",
                "http://dev.day.com");
        result.setActionTarget("_blank");

        results.add(result);

        return results;
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final List<Result> results = new LinkedList<Result>();

        final BasicResult result = new BasicResult(
                "Search AEM documentation",
                "Search for: " + cmd.getParam(),
                "https://duckduckgo.com");

        final Map<String,String> params = new HashMap<String, String>();
        params.put("q", "site:dev.day.com AND " + cmd.getParam());
        result.setActionParams(params);

        result.setActionTarget("_blank");
        results.add(result);

        return results;
    }
}
