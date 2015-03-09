package com.adobe.acs.commons.workflow.audit.impl.sync;


import com.adobe.acs.commons.workflow.audit.WorkflowResourceSync;
import com.adobe.acs.commons.workflow.audit.impl.Constants;
import com.adobe.acs.commons.workflow.audit.impl.WorkflowAuditUtil;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.model.WorkflowModel;
import com.day.cq.workflow.model.WorkflowNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
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
                value = "workitem"
        )
})

@Service
public class WorkflowWorkItemSyncImpl implements WorkflowResourceSync {
    private Logger log = LoggerFactory.getLogger(WorkflowWorkItemSyncImpl.class);

    @Reference
    private WorkflowService workflowService;


    @Override
    public boolean accepts(final Resource resource) {
        return resource.isResourceType(Constants.RT_CQ_WORKITEM);
    }

    @Override
    public void added(final Resource auditResource, final Resource eventResource) throws RepositoryException {
        final Node node = JcrUtils.getOrAddNode(auditResource.adaptTo(Node.class),
                eventResource.getParent().getName(),
                JcrConstants.NT_UNSTRUCTURED);
        final Resource dest = eventResource.getResourceResolver().getResource(node.getPath());

        WorkflowAuditUtil.copyProperties(eventResource, dest);

        final ModifiableValueMap mvm = dest.adaptTo(ModifiableValueMap.class);
        mvm.put(SlingConstants.PROPERTY_RESOURCE_TYPE, Constants.RT_WORKFLOW_ITEM_AUDIT);
        mvm.put("createdAt", eventResource.getParent().adaptTo(ValueMap.class).get("date", Calendar.class));


        final WorkflowSession workflowSession = workflowService.getWorkflowSession(auditResource.getResourceResolver()
                .adaptTo(Session.class));

        try {
            final Workflow workflow = workflowSession.getWorkflow(mvm.get("workflowId", String.class));

            if (workflow != null) {
                final WorkflowModel workflowModel = workflow.getWorkflowModel();
                final WorkflowNode workflowNode = workflowModel.getNode(mvm.get("nodeId", String.class));

                if (workflowNode != null) {
                    mvm.put("workflowStepTitle", workflowNode.getTitle());
                    mvm.put("workflowStepDescription", workflowNode.getDescription());

                    // This may change from parent node based on Container steps
                    mvm.put("modelTitle", workflowModel.getTitle());
                    mvm.put("modelVersion", workflowModel.getVersion());
                }
            }
        } catch (WorkflowException e) {
            log.error("");
        }
    }

    @Override
    public void changed(final Resource auditResource, final Resource eventResource) throws RepositoryException {
        final Node workItem = JcrUtils.getOrAddNode(auditResource.adaptTo(Node.class),
                eventResource.getName(),
                JcrConstants.NT_UNSTRUCTURED);

        WorkflowAuditUtil.copyProperties(eventResource, eventResource.getResourceResolver().getResource(workItem.getPath()));
    }
}
