package com.adobe.acs.commons.workflow.audit;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.util.List;

public interface WorkflowAuditReportHelper {

    List<ValueMap> getWorkItems(Resource workflowAuditResource);

}
