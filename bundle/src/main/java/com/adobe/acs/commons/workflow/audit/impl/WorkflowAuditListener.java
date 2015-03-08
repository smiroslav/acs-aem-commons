package com.adobe.acs.commons.workflow.audit.impl;


import com.adobe.acs.commons.workflow.audit.WorkflowResourceSync;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        label = "ACS AEM Commons - Workflow Audit Event Handler",
        description = "Captures AEM Workflow events and stores them as custom Audit nodes.",
        immediate = true
)
@Properties({
        @Property(
                label = "Event Topics",
                value = { SlingConstants.TOPIC_RESOURCE_ADDED, SlingConstants.TOPIC_RESOURCE_CHANGED },
                name = EventConstants.EVENT_TOPIC,
                propertyPrivate = true
        ),
        @Property(
                label = "Event Filters",
                value = "(" + SlingConstants.PROPERTY_PATH + "=/etc/workflow/instances/*)",
                name = EventConstants.EVENT_FILTER,
                propertyPrivate = true
        )
})
@References({
        @Reference(
                name = "workflowResourceSyncers",
                referenceInterface = WorkflowResourceSync.class,
                policy = ReferencePolicy.DYNAMIC,
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
})
@Service
public class WorkflowAuditListener implements JobProcessor, EventHandler {
    private Logger log = LoggerFactory.getLogger(WorkflowAuditListener.class);

    private Map<String, WorkflowResourceSync> workflowResourceSyncs =
            new ConcurrentHashMap<String, WorkflowResourceSync>();

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void handleEvent(final Event event) {
        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);

        if (StringUtils.startsWith(path, "/etc/workflow/instances/")) {
            JobUtil.processJob(event, this);
        }
    }

    @Override
    synchronized public boolean process(final Event event) {
        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);

        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Resource eventResource = resourceResolver.getResource(path);

            Resource auditResource = null;

            for (final WorkflowResourceSync workflowResourceSync : this.workflowResourceSyncs.values()) {
                if (workflowResourceSync.accepts(eventResource)) {

                    if (auditResource == null) {
                        auditResource = this.getOrCreateAuditFolder(resourceResolver, eventResource);
                    }

                    if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(event.getTopic())) {
                        workflowResourceSync.added(auditResource, eventResource);
                    } else if (SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic())) {
                        workflowResourceSync.changed(auditResource, eventResource);
                    }
                }
            }

            save(resourceResolver);
        } catch (LoginException e) {
            log.error("Could not get Workflow Audit service account.", e);
            return false;
        } catch (PersistenceException e) {
            log.error("Could not save Audit entry for Workflow.", e);
            return false;
        } catch (RepositoryException e) {
            log.error("Could not create Audit entry for Workflow.", e);
            return false;
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }

        // Only return false if job processing failed and the job should be rescheduled
        return true;
    }

    private void save(final ResourceResolver resourceResolver) throws PersistenceException {
        if (resourceResolver.hasChanges()) {
            final long start = System.currentTimeMillis();
            resourceResolver.commit();
            log.debug("Saved Workflow Audit in [ {} ] ms", System.currentTimeMillis() - start);
        }
    }

    private Resource getOrCreateAuditFolder(final ResourceResolver resourceResolver,
                                            final Resource workflowResource) throws RepositoryException {

        final String path = WorkflowAuditUtil.getAuditPath(workflowResource);
        final Node node = JcrUtils.getOrCreateByPath(path, "sling:OrderedFolder", "sling:OrderedFolder",
                resourceResolver.adaptTo(Session.class), true);

        return resourceResolver.getResource(node.getPath());
    }

    /*
    private Resource createAuditItem(final ResourceResolver resourceResolver,
                                     final Resource parent) throws RepositoryException, PersistenceException {
        final Node node = JcrUtils.getOrCreateUniqueByPath(parent.getPath() + "/item",
                "nt:unstructured", resourceResolver.adaptTo(Session.class));

        return resourceResolver.getResource(node.getPath());
    }
    */

    public void order(final Resource resource) throws RepositoryException {
        final Set<String> names = new TreeSet<String>(Collections.reverseOrder());
        final Iterator<Resource> children = resource.getChildren().iterator();

        while (children.hasNext()) {
            names.add(children.next().getName());
        }

        JcrUtil.setChildNodeOrder(resource.adaptTo(Node.class),
                names.toArray(new String[names.size()]));

        if (Constants.ROOT_PATH.equals(resource.getPath())) {
            return;
        } else {
            this.order(resource.getParent());
        }
    }


    /* Workflow Resource Syncs */

    protected final void bindWorkflowResourceSync(final WorkflowResourceSync service,
                                                  final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(WorkflowResourceSync.PROP_TYPE), null);

        if (type != null) {
            log.debug("Adding workflow resource added: {}", type);
            this.workflowResourceSyncs.put(type, service);
        }
    }

    protected final void unbindWorkflowResourceSync(final WorkflowResourceSync service,
                                                    final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(WorkflowResourceSync.PROP_TYPE), null);

        if (type != null) {
            log.debug("Removing workflow resource added: {}", type);
            this.workflowResourceSyncs.remove(type);
        }
    }
}
