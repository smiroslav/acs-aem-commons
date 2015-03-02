package com.adobe.acs.commons.workflow.audit.impl;


import com.adobe.acs.commons.workflow.audit.WorkflowAuditItemRecorder;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
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
import java.util.Map;
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
                description = "[Required] Event Topics this event handler will to respond to.",
                name = EventConstants.EVENT_TOPIC,
                propertyPrivate = true
        ),
        @Property(
                label = "Event Filters",
                value =   "(|(" + SlingConstants.PROPERTY_RESOURCE_TYPE+ "=cq/workflow/components/instance)("
                        + SlingConstants.PROPERTY_RESOURCE_TYPE + "=cq/workflow/components/workitem))",
                description = "[Optional] Event Filters used to further restrict this event handler; Uses LDAP expression against event properties.",
                name = EventConstants.EVENT_FILTER,
                propertyPrivate = true
        )
})
@References({
        @Reference(
                name = "workflowAuditRecorder",
                referenceInterface = WorkflowAuditItemRecorder.class,
                policy = ReferencePolicy.DYNAMIC,
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
})
@Service
public class WorkflowAuditListener implements JobProcessor, EventHandler {
    private Logger log = LoggerFactory.getLogger(WorkflowAuditListener.class);

    private Map<String, WorkflowAuditItemRecorder> workflowAuditItemRecorders = new ConcurrentHashMap<String,
                WorkflowAuditItemRecorder>();

    private static final String ROOT_PATH = "/etc/workflow/audit";

    @Reference
    private WorkflowService workflowService;

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

            final String modelId = Text.getName(Text.getAbsoluteParent(path, 3)) + "/"
                    + Text.getName(Text.getAbsoluteParent(path, 4));

            final Resource workflowAuditResource = this.getOrCreateAuditFolder(resourceResolver, modelId);
            final Resource eventResource = resourceResolver.getResource(path);

            if (eventResource.isResourceType("cq/workflow/components/instance"))  {
                if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(event.getTopic()))  {
                    this.recordWorkflowInstanceAdd(workflowAuditResource, eventResource);
                } else if (SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic()))  {
                    this.recordWorkflowInstanceModify(workflowAuditResource, eventResource);
                }
            } else if (eventResource.isResourceType("cq/workflow/components/workitem"))  {
                if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(event.getTopic()))  {
                    this.recordWorkItemAdd(workflowAuditResource, eventResource);
                } else if (SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic()))  {
                    this.recordWorkItemModify(workflowAuditResource, eventResource);
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

    private void recordWorkflowInstanceAdd(final Resource workflowAuditResource, final Resource eventResource) throws PersistenceException {
        final ValueMap src = eventResource.adaptTo(ValueMap.class);
        final ModifiableValueMap dest = workflowAuditResource.adaptTo(ModifiableValueMap.class);

        for (final Map.Entry<String, Object> entry : src.entrySet()) {
            if (!StringUtils.startsWithAny(entry.getKey(), new String[] {"jcr:", "sling:"})) {
                dest.put(entry.getKey(), entry.getValue());
            }
        }

        dest.put(SlingConstants.PROPERTY_RESOURCE_TYPE, Constants.RT_WORKFLOW_INSTANCE_AUDIT);

        final String workflowId = Text.getAbsoluteParent(eventResource.getPath(), 4);

            final WorkflowSession workflowSession =
                    workflowService.getWorkflowSession(eventResource.getResourceResolver().adaptTo(Session.class));

            Workflow workflow = null;
            try {
                workflow = workflowSession.getWorkflow(workflowId);

                dest.put("payload", workflow.getWorkflowData().getPayload());
                dest.put("modelTitle", workflow.getWorkflowModel().getTitle());
            } catch (WorkflowException e) {
                log.warn("Could not find Workflow to collect the Model title");
            }

        this.save(eventResource.getResourceResolver());
    }

    private void recordWorkflowInstanceModify(final Resource workflowAuditResource, final Resource eventResource) throws PersistenceException {
        final ModifiableValueMap dest = workflowAuditResource.adaptTo(ModifiableValueMap.class);

        this.copyProperties(eventResource, workflowAuditResource);
        dest.put(SlingConstants.PROPERTY_RESOURCE_TYPE, Constants.RT_WORKFLOW_INSTANCE_AUDIT);

        this.save(eventResource.getResourceResolver());
    }

    private void recordWorkItemAdd(final Resource workflowAuditResource, final Resource eventResource) throws RepositoryException {
        final Node src = eventResource.adaptTo(Node.class);
        final Node dest = workflowAuditResource.adaptTo(Node.class);

        final Node workItem = JcrUtils.getOrAddNode(dest, src.getName(), JcrConstants.NT_UNSTRUCTURED);
        this.copyProperties(eventResource, eventResource.getResourceResolver().getResource(workItem.getPath()));

        workItem.setProperty(SlingConstants.PROPERTY_RESOURCE_TYPE, Constants.RT_WORKFLOW_ITEM_AUDIT);
    }

    private void recordWorkItemModify(final Resource workflowAuditResource, final Resource eventResource) throws RepositoryException {
        final Node src = eventResource.adaptTo(Node.class);
        final Node dest = workflowAuditResource.adaptTo(Node.class);

        final Node workItem = JcrUtils.getOrAddNode(dest, src.getName(), JcrConstants.NT_UNSTRUCTURED);
        this.copyProperties(eventResource, eventResource.getResourceResolver().getResource(workItem.getPath()));

        workItem.setProperty(SlingConstants.PROPERTY_RESOURCE_TYPE, Constants.RT_WORKFLOW_ITEM_AUDIT);
    }

    private void copyProperties(final Resource srcResource, final Resource destResource) {
        final ValueMap src = srcResource.adaptTo(ValueMap.class);
        final ModifiableValueMap dest = destResource.adaptTo(ModifiableValueMap.class);

        for (final Map.Entry<String, Object> entry : src.entrySet()) {
            if (!StringUtils.startsWithAny(entry.getKey(), new String[]{ "jcr:", "sling:" })) {
                dest.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private void save(final ResourceResolver resourceResolver) throws PersistenceException {
        if (resourceResolver.hasChanges()) {
            final long start = System.currentTimeMillis();
            resourceResolver.commit();
            log.debug("Saved Workflow Audit in [ {} ] ms", System.currentTimeMillis() - start);
        }
    }

    private Resource getOrCreateAuditFolder(final ResourceResolver resourceResolver, final String workflowId) throws
            RepositoryException {
        final String nodeName = StringUtils.replace(workflowId, "-", "/");
        final String path = ROOT_PATH + "/" + nodeName;

        final Node node = JcrUtils.getOrCreateByPath(path, "sling:Folder", "sling:Folder",
                resourceResolver.adaptTo(Session.class), true);

        return resourceResolver.getResource(node.getPath());
    }

    private Resource createAuditItem(final ResourceResolver resourceResolver,
                                     final Resource parent) throws RepositoryException, PersistenceException {
        final Node node = JcrUtils.getOrCreateUniqueByPath(parent.getPath() + "/item" ,
                "nt:unstructured", resourceResolver.adaptTo(Session.class));

        return resourceResolver.getResource(node.getPath());
    }

    protected final void bindWorkflowAuditRecorder(final WorkflowAuditItemRecorder service,
                                           final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(WorkflowAuditItemRecorder.PROP_TYPE), null);

        if (type != null) {
            log.debug("Adding recorder: {}", type);
            this.workflowAuditItemRecorders.put(type, service);
        }
    }

    protected final void unbindWorkflowAuditRecorder(final WorkflowAuditItemRecorder service,
                                             final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(WorkflowAuditItemRecorder.PROP_TYPE), null);

        if (type != null) {
            log.debug("Removing recorder: {}", type);
            this.workflowAuditItemRecorders.remove(type);
        }
    }
}
