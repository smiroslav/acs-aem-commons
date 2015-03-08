package com.adobe.acs.commons.workflow.audit.impl.sync;


import com.adobe.acs.commons.workflow.audit.WorkflowResourceSync;
import com.adobe.acs.commons.workflow.audit.impl.Constants;
import com.adobe.acs.commons.workflow.audit.impl.WorkflowAuditUtil;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

@Component
@Properties({
        @Property(
                name = WorkflowResourceSync.PROP_TYPE,
                value = "workitem-metadata"
        )
})

@Service
public class WorkflowWorkItemMetadataSyncImpl implements WorkflowResourceSync {
    private Logger log = LoggerFactory.getLogger(WorkflowWorkItemMetadataSyncImpl.class);

    private static final String NN_METADATA = "metaData";

    @Override
    public boolean accepts(final Resource resource) {
        return NN_METADATA.equals(resource.getName())
                && resource.getParent().isResourceType(Constants.RT_CQ_WORKITEM);
    }

    @Override
    public void added(final Resource auditResource, final Resource eventResource) throws RepositoryException {
        final Resource auditWorkItemResource = WorkflowAuditUtil.getAuditWorkItemResource(auditResource, eventResource);

        JcrUtil.copy(eventResource.adaptTo(Node.class),
                auditWorkItemResource.adaptTo(Node.class), NN_METADATA);
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
