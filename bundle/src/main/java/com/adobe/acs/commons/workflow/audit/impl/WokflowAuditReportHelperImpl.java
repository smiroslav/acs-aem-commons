package com.adobe.acs.commons.workflow.audit.impl;

import com.adobe.acs.commons.workflow.audit.WorkflowAuditReportHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@Service
public class WokflowAuditReportHelperImpl implements WorkflowAuditReportHelper {

    @Override
    public List<ValueMap> getWorkItems(final Resource workflowAuditResource) {

        final ResourceResolver resourceResolver = workflowAuditResource.getResourceResolver();

        List<ValueMap> workItems = new ArrayList<ValueMap>();

        final Iterator<Resource> resources = workflowAuditResource.listChildren();

        while (resources.hasNext()) {
            final Resource resource = resources.next();
            final ValueMap properties = resource.adaptTo(ValueMap.class);

            if (properties.get(Constants.PN_IS_CONTAINER, false)) {
                // Is a container; get that AuditResource
                final String nextPath = properties.get(Constants.PN_CONTAINEE_AUDIT_PATH, String.class);

                if (StringUtils.isNotBlank(nextPath)) {
                    workItems.addAll(this.getWorkItems(resourceResolver.getResource(nextPath)));
                }
            } else {
                // Normal workItem
                workItems.add(properties);
            }
        }

        return workItems;
    }
}
