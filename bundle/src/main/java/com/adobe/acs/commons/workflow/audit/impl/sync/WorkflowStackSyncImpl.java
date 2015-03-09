package com.adobe.acs.commons.workflow.audit.impl.sync;


import com.adobe.acs.commons.workflow.audit.WorkflowResourceSync;
import com.adobe.acs.commons.workflow.audit.impl.Constants;
import com.adobe.acs.commons.workflow.audit.impl.WorkflowAuditUtil;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Calendar;

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

    @Override
    public boolean accepts(final Resource resource) {
        return resource.isResourceType("cq:WorkflowStack");
    }

    @Override
    public void added(final Resource auditResource, final Resource eventResource) throws RepositoryException {
        final ValueMap stackProperties = eventResource.adaptTo(ValueMap.class);

        final String containeeInstanceId = stackProperties.get(Constants.PN_CONTAINEE_INSTANCE_ID, String.class);
        final String parentInstanceId = stackProperties.get(Constants.PN_PARENT_INSTANCE_ID, String.class);

        final ResourceResolver resourceResolver =  auditResource.getResourceResolver();

        /* Mark the new containee worflowAuditInstance with the Parent worflowAuditInstance path */
        final Resource containeeAuditResource = resourceResolver.getResource(
                WorkflowAuditUtil.getAuditPath(containeeInstanceId));

        final ModifiableValueMap containeeMVM = containeeAuditResource.adaptTo(ModifiableValueMap.class);
        containeeMVM.put("parentAuditPath", WorkflowAuditUtil.getAuditPath(parentInstanceId));
        containeeMVM.put(Constants.PN_IS_CONTAINER, true);

        /* Mark the new parent worflowAuditInstance with the containee worflowAuditInstance path */

        final Resource parentAuditResource = resourceResolver.getResource(
                WorkflowAuditUtil.getAuditPath(parentInstanceId));

        final Node containerNode = JcrUtils.getOrCreateByPath(
                parentAuditResource.getPath() + "/" + System.currentTimeMillis(),
                JcrConstants.NT_UNSTRUCTURED, resourceResolver.adaptTo(Session.class));

        final ModifiableValueMap parentMVM =
                resourceResolver.getResource(containerNode.getPath()).adaptTo(ModifiableValueMap.class);

        parentMVM.put(Constants.PN_IS_CONTAINER, true);
        parentMVM.put(Constants.PN_CONTAINEE_AUDIT_PATH, WorkflowAuditUtil.getAuditPath(containeeInstanceId));
        parentMVM.put(Constants.PN_CREATED_AT, Calendar.getInstance());
    }

    @Override
    public void changed(final Resource auditResource, final Resource eventResource) {
        log.warn("Workflow Stack Changed but not tracked");
    }
}
