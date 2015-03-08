package com.adobe.acs.commons.workflow.audit.impl.sync;


import com.adobe.acs.commons.workflow.audit.WorkflowResourceSync;
import com.adobe.acs.commons.workflow.audit.impl.Constants;
import com.adobe.acs.commons.workflow.audit.impl.WorkflowAuditUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
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
    }

    @Override
    public void changed(final Resource auditResource, final Resource eventResource) throws RepositoryException {
        final Node workItem = JcrUtils.getOrAddNode(auditResource.adaptTo(Node.class),
                eventResource.getName(),
                JcrConstants.NT_UNSTRUCTURED);

        WorkflowAuditUtil.copyProperties(eventResource, eventResource.getResourceResolver().getResource(workItem.getPath()));
    }
}
