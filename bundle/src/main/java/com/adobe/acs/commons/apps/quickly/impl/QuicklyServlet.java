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

package com.adobe.acs.commons.apps.quickly.impl;

import com.adobe.acs.commons.apps.quickly.Command;
import com.adobe.acs.commons.apps.quickly.operations.Operation;
import com.adobe.acs.commons.apps.quickly.results.Result;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@SlingServlet(
        label = "ACS AEM Commons - Quickly Servlet",
        paths = "/bin/quickly-v1.json"
)
@Reference(
        name = "operations",
        referenceInterface = Operation.class,
        policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
)
public class QuicklyServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(QuicklyServlet.class);

    private static final String KEY_RESULTS = "results";

    @Reference(target = "(cmd=" + Operation.DEFAULT_CMD + ")")
    private Operation defaultOperation;

    private Map<String, Operation> operations = new HashMap<String, Operation>();

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Command cmd = new Command(request);

        response.setHeader("Content-Type", " application/json; charset=UTF-8");

        try {
            response.getWriter().append(this.execute(request, cmd).toString());

        } catch (JSONException e) {
            response.sendError(response.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private JSONObject execute(final SlingHttpServletRequest slingRequest, final Command cmd) throws JSONException {
        for (Map.Entry<String, Operation> operation : operations.entrySet()) {
            if (operation.getValue().accepts(slingRequest, cmd)) {
                return this.getJSONResults(operation.getValue().getResults(slingRequest, cmd));
            }
        }

        return this.getJSONResults(defaultOperation.getResults(slingRequest, cmd));
    }

    private JSONObject getJSONResults(final Collection<Result> results) throws JSONException {
        final JSONObject json = new JSONObject();

        json.put(KEY_RESULTS, new JSONArray());

        for (final Result result : results) {
            if (result.isValid()) {
                json.accumulate(KEY_RESULTS, result.toJSON());
            }
        }

        return json;
    }


    protected final void bindOperations(final Operation service, final Map<Object, Object> props) {
        final String cmd = PropertiesUtil.toString(props.get(Operation.PROP_CMD), null);

        if (cmd != null && !StringUtils.equalsIgnoreCase(Operation.DEFAULT_CMD, cmd)) {
            operations.put(cmd, service);
        }
    }

    protected final void unbindOperations(final Operation service, final Map<Object, Object> props) {
        final String cmd = PropertiesUtil.toString(props.get(Operation.PROP_CMD), null);

        if (cmd != null && !StringUtils.equalsIgnoreCase(Operation.DEFAULT_CMD, cmd)) {
            operations.remove(cmd);
        }
    }

}
