package com.adobe.acs.commons.workflow.audit.impl.sync;


import com.adobe.acs.commons.workflow.audit.WorkflowResourceSync;
import com.adobe.acs.commons.workflow.audit.impl.WorkflowAuditUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component
@Properties({
        @Property(
                name = WorkflowResourceSync.PROP_TYPE,
                value = "workflow-stack"
        )
})
@Service
public class WorkflowStackSyncImpl implements WorkflowResourceSync {
    private Logger log = LoggerFactory.getLogger(WorkflowStackSyncImpl.class);

    private static final String NN_METADATA = "metaData";

    @Override
    public boolean accepts(final Resource resource) {
        return resource.isResourceType("cq:WorkflowStack");
    }

    @Override
    public void added(final Resource auditResource, final Resource eventResource) throws RepositoryException {
        final Resource workflowStacks =
                WorkflowAuditUtil.getWorkflowStacksResource(auditResource.getResourceResolver());

        final ValueMap properties = eventResource.adaptTo(ValueMap.class);
        final String containeeInstanceId = properties.get("containeeInstanceId", String.class);
        final String parentInstanceId = properties.get("parentInstanceId", String.class);

        final ResourceResolver resourceResolver =  auditResource.getResourceResolver();
        final Session session = resourceResolver.adaptTo(Session.class);
        final Node node = JcrUtils.getOrCreateByPath(workflowStacks.getPath() + containeeInstanceId, "sling:Folder",
                session);

        node.setProperty("parentInstanceId", parentInstanceId);

    }

    @Override
    public void changed(final Resource auditResource, final Resource eventResource) {
        final Resource auditWorkItemResource = WorkflowAuditUtil.getAuditWorkItemResource(auditResource, eventResource);
        final Resource metadataResource = auditWorkItemResource.getChild(NN_METADATA);

        if (metadataResource != null) {
            WorkflowAuditUtil.copyProperties(eventResource, metadataResource);
        }
    }
}
