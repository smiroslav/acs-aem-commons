package com.adobe.acs.commons.workflow.audit;


import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.Workflow;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

public interface WorkflowAuditItemRecorder {
    String PROP_TYPE = "type";

    ValueMap getData(ResourceResolver resourceResolver, Workflow workflow, WorkItem workItem);
}
