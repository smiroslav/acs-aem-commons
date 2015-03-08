package com.adobe.acs.commons.workflow.audit.impl.sync;


import com.adobe.acs.commons.workflow.audit.WorkflowResourceSync;
import com.adobe.acs.commons.workflow.audit.impl.Constants;
import com.adobe.acs.commons.workflow.audit.impl.WorkflowAuditUtil;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

@Component
@Properties({
    @Property(
            name = WorkflowResourceSync.PROP_TYPE,
            value = "workflow-instance"
    )
})
@Service
public class WorkflowInstanceSyncImpl implements WorkflowResourceSync {
    private Logger log = LoggerFactory.getLogger(WorkflowInstanceSyncImpl.class);

    @Reference
    private WorkflowService workflowService;

    @Override
    public boolean accepts(final Resource resource) {
        return resource.isResourceType("cq/workflow/components/instance");
    }

    @Override
    public void added(final Resource auditResource, final Resource eventResource) {
        final ModifiableValueMap dest = auditResource.adaptTo(ModifiableValueMap.class);

        WorkflowAuditUtil.copyProperties(eventResource, auditResource);

        dest.put(SlingConstants.PROPERTY_RESOURCE_TYPE, Constants.RT_WORKFLOW_INSTANCE_AUDIT);

        /* Copy Workflow Properties */

        final String workflowId = Text.getAbsoluteParent(eventResource.getPath(), 4);

        final WorkflowSession workflowSession =
                workflowService.getWorkflowSession(eventResource.getResourceResolver().adaptTo(Session.class));

        try {
            final Workflow workflow = workflowSession.getWorkflow(workflowId);

            dest.put("payload", workflow.getWorkflowData().getPayload());
            dest.put("modelTitle", workflow.getWorkflowModel().getTitle());
        } catch (WorkflowException e) {
            log.warn("Could not find Workflow to collect the Model title");
        }
    }

    @Override
    public void changed(final Resource auditResource, final Resource eventResource) {
        final ModifiableValueMap dest = auditResource.adaptTo(ModifiableValueMap.class);

        WorkflowAuditUtil.copyProperties(eventResource, auditResource);
        dest.put(SlingConstants.PROPERTY_RESOURCE_TYPE, Constants.RT_WORKFLOW_INSTANCE_AUDIT);
    }
}
