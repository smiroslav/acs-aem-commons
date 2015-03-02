package com.adobe.acs.commons.workflow.audit.impl;


import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;

@SlingServlet(
        label = "ACS AEM Commons - Workflow Audit Data",
        paths = { "/bin/workflow-audit.json" },
        methods = { "GET" }
)
public class WorkflowAuditDataServlet extends SlingSafeMethodsServlet {
    private static final String ROOT_PATH = "/etc/workflow/audit";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        final JSONArray table = new JSONArray();

        final Resource root = request.getResourceResolver().getResource("/etc/workflow/audit");
        final AuditVisitor auditVisitor = new AuditVisitor(table);

        auditVisitor.accept(root);

        response.getWriter().print(table.toString());
    }


    private class AuditVisitor extends AbstractResourceVisitor {
        private final Logger log = LoggerFactory.getLogger(AuditVisitor.class);

        private final JSONArray table;

        private JSONObject row = null;

        public AuditVisitor(JSONArray table) {
            this.table = table;
        }

        public JSONArray getTable() {
            return this.table;
        }

        @Override
        protected void visit(final Resource resource) {
            final ValueMap properties = resource.adaptTo(ValueMap.class);
            final String resourceType = properties.get(SlingConstants.PROPERTY_RESOURCE_TYPE, String.class);

            try {

                if (StringUtils.equals(Constants.RT_WORKFLOW_INSTANCE_AUDIT, resourceType)) {
                    row = new JSONObject();

                    // New Row

                    row.put("path", resource.getPath());
                    row.put("status", properties.get("status", "?"));
                    row.put("modelTitle", properties.get("modelTitle", "?"));
                    row.put("modelVersion", properties.get("modelVersion", "1.0"));
                    row.put("initiator", properties.get("initiator", "?"));
                    row.put("payload", properties.get("payload", "?"));

                    if (properties.get("startTime", Date.class) != null) {
                        row.put("startTime", properties.get("startTime", Date.class));
                    }

                    if (properties.get("endTime", Date.class) != null) {
                        row.put("endTime", properties.get("endTime", Date.class));
                    }

                    table.put(row);

                } else if (StringUtils.equals(Constants.RT_WORKFLOW_ITEM_AUDIT, resourceType)) {
                    final JSONObject item = new JSONObject();

                    item.put("assignee", properties.get("assignee", "?"));
                    item.put("status", properties.get("status", "?"));

                    if (properties.get("startTime", Date.class) != null) {
                        item.put("startTime", properties.get("startTime", Date.class));
                    }

                    if (properties.get("endTime", Date.class) != null) {
                        item.put("endTime", properties.get("endTime", Date.class));
                    }

                    item.put("comment", properties.get("metaData/comment", ""));

                    row.accumulate("items", item);
                }

            } catch (JSONException e) {
                log.error("Cannot build audit JSON response", e);
            }
        }
    }

}