package com.adobe.acs.commons.workflow.audit;

import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;


public interface WorkflowResourceSync {
    String PROP_TYPE = "type";

    boolean accepts(Resource resource);

    void added(Resource auditResource, Resource eventResource) throws RepositoryException;

    void changed(Resource auditResource, Resource eventResource) throws RepositoryException;

}
