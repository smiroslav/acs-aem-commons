package com.adobe.acs.commons.workflow.audit.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;

public class WorkflowAuditUtil {

    public static Resource getAuditWorkItemResource(final Resource auditResource, final Resource eventResource) {
        final String path = getAuditWorkItemPath(auditResource, eventResource);

        if (path != null) {
            return auditResource.getResourceResolver().getResource(path);
        }

        return null;
    }

    public static String getAuditWorkItemPath(final Resource auditResource,
                                              final Resource eventResource) {

        final String workItemPath = Text.getAbsoluteParent(eventResource.getPath(), 7);
        final Resource tmp = auditResource.getResourceResolver().getResource(workItemPath);

        if (tmp != null && tmp.isResourceType(Constants.RT_CQ_WORKITEM)) {
            return auditResource.getPath() + "/" + tmp.getParent().getName();
        } else {
            return null;
        }
    }

    public static String getAuditPath(final Resource eventResource) {
        final String workflowId = getWorkflowModelId(eventResource.getPath());
        final String nodeName = StringUtils.replace(workflowId, "-", "/");
        final String path = Constants.ROOT_PATH + "/" + nodeName;

        return path;
    }

    public static void copyProperties(final Resource srcResource, final Resource destResource) {
        final ValueMap src = srcResource.adaptTo(ValueMap.class);
        final ModifiableValueMap dest = destResource.adaptTo(ModifiableValueMap.class);

        for (final Map.Entry<String, Object> entry : src.entrySet()) {
            if (!StringUtils.startsWithAny(entry.getKey(), new String[]{ "jcr:", "sling:" })) {
                dest.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static final String getWorkflowModelId(final String path) {
        return Text.getName(Text.getAbsoluteParent(path, 3)) + "/"
                + Text.getName(Text.getAbsoluteParent(path, 4));
    }

    public static final Resource getWorkflowStacksResource(final ResourceResolver resourceResolver) throws
            RepositoryException {
        final Node node = JcrUtils.getOrCreateByPath("/etc/workflow/audit/workflow-stacks",
                "sling:Folder",
                resourceResolver.adaptTo(Session.class));

        return resourceResolver.getResource(node.getPath());
    }

}
