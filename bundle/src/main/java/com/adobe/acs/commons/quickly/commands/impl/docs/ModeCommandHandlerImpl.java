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
import com.adobe.acs.commons.quickly.results.GoResult;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
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
        label = "ACS AEM Commons - Quickly - WCMMode Command Handler"
)
@Properties({
        @Property(
                name = "cmd",
                value = ModeCommandHandlerImpl.CMD,
                propertyPrivate = true
        )
})
@Service
public class ModeCommandHandlerImpl extends AbstractCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(ModeCommandHandlerImpl.class);

    public static final String CMD = "mode";

    private static List<Result> modes = new LinkedList<Result>();

    @Override
    public boolean accepts(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return StringUtils.endsWithIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return modes;
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final List<Result> results = new LinkedList<Result>();

        for(final Result mode : modes) {
            if(StringUtils.startsWithIgnoreCase(mode.getTitle(), cmd.getParam())) {
                results.add(mode);
            }
        }

        if(results.isEmpty()) {
            return modes;
        } else {
            return results;
        }
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        modes = new LinkedList<Result>();

        modes.add(this.buildResult("edit", "Edit mode"));
        modes.add(this.buildResult("design", "Design mode"));
        modes.add(this.buildResult("preview", "Preview mode"));
        modes.add(this.buildResult("disabled", "Disabled mode (Publish view)"));
    }


    private Result buildResult(String title, String description) {
        final Map<String, String> params = new HashMap<String, String>();

        final GoResult result = new GoResult(title, description, "#");

        params.put("wcmmmode", StringUtils.lowerCase(title));
        result.setActionParams(params);

        return result;
    }
}
