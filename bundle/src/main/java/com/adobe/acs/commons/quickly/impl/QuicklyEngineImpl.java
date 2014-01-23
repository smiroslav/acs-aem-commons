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

package com.adobe.acs.commons.quickly.impl;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.QuicklyEngine;
import com.adobe.acs.commons.quickly.commands.CommandHandler;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component
@Reference(
        name = "commandHandlers",
        referenceInterface = CommandHandler.class,
        policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
)
@Service
public class QuicklyEngineImpl implements QuicklyEngine {
    private static final Logger log = LoggerFactory.getLogger(QuicklyEngineImpl.class);

    @Reference(target = "(cmd=" + CommandHandler.DEFAULT_CMD + ")")
    private CommandHandler defaultCommand;

    private Map<String, CommandHandler> commandHandlers = new HashMap<String, CommandHandler>();

    @Override
    public JSONObject execute(final SlingHttpServletRequest slingRequest, final Command cmd) throws JSONException {
        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();

        for (Map.Entry<String, CommandHandler> commandHandler : commandHandlers.entrySet()) {
            if (commandHandler.getValue().accepts(slingRequest, cmd)) {
                return commandHandler.getValue().getResults(slingRequest, cmd);
            }
        }

        final Command defaultCmd = new Command(CommandHandler.DEFAULT_CMD + " " + cmd.getParam());
        return defaultCommand.getResults(slingRequest, defaultCmd);
    }


    // Bind
    protected void bindCommandHandlers(final CommandHandler service, final Map<Object, Object> props) {
        final String cmd = PropertiesUtil.toString(props.get(CommandHandler.PROP_CMD), null);

        if (cmd != null) { // && !StringUtils.equalsIgnoreCase(CommandHandler.DEFAULT_CMD, cmd)) {
            commandHandlers.put(cmd, service);
        }
    }

    // Unbind
    protected void unbindCommandHandlers(final CommandHandler service, final Map<Object, Object> props) {
        final String cmd = PropertiesUtil.toString(props.get(CommandHandler.PROP_CMD), null);

        if (cmd != null) { // &&_ !StringUtils.equalsIgnoreCase(CommandHandler.DEFAULT_CMD, cmd)) {
            commandHandlers.remove(cmd);
        }
    }
}
