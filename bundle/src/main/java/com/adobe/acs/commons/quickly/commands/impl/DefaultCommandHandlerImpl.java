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

package com.adobe.acs.commons.quickly.commands.impl;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.Result;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component(
        label = "ACS AEM Commons - Quickly - Go Command Handler"
)
@Properties({
        @Property(
                name = "cmd",
                value = DefaultCommandHandlerImpl.CMD,
                propertyPrivate = true
        )
})
@Service
public class DefaultCommandHandlerImpl extends GoCommandHandlerImpl {
    private static final Logger log = LoggerFactory.getLogger(DefaultCommandHandlerImpl.class);

    public static final String CMD = "default";

    @Override
    public boolean accepts(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return false;
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return this.withParams(slingRequest, cmd);
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final Command wrappedCmd = new Command(GoCommandHandlerImpl.CMD + " " + cmd.toString());
        log.debug(wrappedCmd.toString());
        return super.withParams(slingRequest, wrappedCmd);
    }
}