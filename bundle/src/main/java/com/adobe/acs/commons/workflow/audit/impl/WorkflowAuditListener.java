package com.adobe.acs.commons.workflow.audit.impl;


import com.adobe.acs.commons.workflow.audit.WorkflowAuditItemRecorder;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.event.WorkflowEvent;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.exec.WorkflowData;
import org.apache.commons.lang.ArrayUtils;
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
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
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
import java.util.Calendar;
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
                value = { WorkflowEvent.EVENT_TOPIC },
                description = "[Required] Event Topics this event handler will to respond to.",
                name = EventConstants.EVENT_TOPIC,
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

    private static final String[] SUPPORTED_WORKFLOW_EVENTS = new String[] {
            WorkflowEvent.WORKFLOW_ABORTED_EVENT,
            WorkflowEvent.WORKFLOW_COMPLETED_EVENT,
            WorkflowEvent.WORKITEM_DELEGATION_EVENT,
            WorkflowEvent.WORKFLOW_RESUMED_EVENT,
            WorkflowEvent.WORKFLOW_STARTED_EVENT,
            WorkflowEvent.WORKFLOW_SUSPENDED_EVENT,
            WorkflowEvent.NODE_TRANSITION_EVENT
    };

    private Map<String, WorkflowAuditItemRecorder> workflowAuditItemRecorders = new ConcurrentHashMap<String,
                WorkflowAuditItemRecorder>();


    private static final String ROOT_PATH = "/etc/workflow/audit";

    private static final String NT_FOLDER = "sling:Folder";
    private static final String NT_DATA = "nt:unstructured";

    private static final String PN_PAYLOAD = "payload";
    private static final String PN_USER = "user";
    private static final String PN_CREATED = "jcr:created";
    private static final String PN_WORKFLOW_ID = "workflowId";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private WorkflowService workflowService;

    @Override
    public void handleEvent(final Event event) {
        final String eventType = (String) event.getProperty(WorkflowEvent.EVENT_TYPE);

        if (eventType != null && ArrayUtils.contains(SUPPORTED_WORKFLOW_EVENTS, eventType)) {
            JobUtil.processJob(event, this);
        } else {
            log.debug("Skipping event type [ {} ]", eventType);
        }
    }

    @Override
    synchronized public boolean process(final Event event) {

        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            final WorkflowSession workflowSession =
                    workflowService.getWorkflowSession(resourceResolver.adaptTo(Session.class));

            final String eventType = (String) event.getProperty(WorkflowEvent.EVENT_TYPE);
            final String instanceId = (String) event.getProperty(WorkflowEvent.WORKFLOW_INSTANCE_ID);
            final Workflow workflow = workflowSession.getWorkflow(instanceId);

            final Resource audit = this.getOrCreateAuditFolder(resourceResolver,
                    StringUtils.removeStart(workflow.getId(), "/etc/workflow/instances/"));

            // Audit the Workflow
            this.auditLogWorkflow(workflow, audit);

            final Resource item = this.createAuditItem(resourceResolver, audit);

            if (WorkflowEvent.WORKFLOW_STARTED_EVENT.equals(eventType)) {
                this.auditLogItem("START", workflow, item);
            } else if (WorkflowEvent.WORKFLOW_COMPLETED_EVENT.equals(eventType)) {
                this.auditLogItem("COMPLETED", workflow, item);
            } else if (WorkflowEvent.WORKFLOW_ABORTED_EVENT.equals(eventType)) {
                this.auditLogItem("ABORTED",  workflow, item);
            } else if (WorkflowEvent.WORKFLOW_RESUMED_EVENT.equals(eventType)) {
                this.auditLogItem("RESUMED",  workflow, item);
            } else if (WorkflowEvent.WORKFLOW_SUSPENDED_EVENT.equals(eventType)) {
                this.auditLogItem("SUSPENDED",  workflow, item);
            } else if (WorkflowEvent.WORKITEM_DELEGATION_EVENT.equals(eventType)) {
                this.auditLogItem("DELEGATED",  workflow, item);
            } else if (WorkflowEvent.NODE_TRANSITION_EVENT.equals(eventType)) {
                this.auditLogItem("TRANSITION",  workflow, item);
            }

            save(resourceResolver);
        } catch (LoginException e) {
            log.error("Could not get Workflow Audit service account.", e);
            return false;
        } catch (WorkflowException e) {
            log.error("Could not get Workflow.", e);
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

    private void auditLogWorkflow(final Workflow workflow, final Resource resource) throws RepositoryException,
            PersistenceException {

        final WorkflowData workflowData = workflow.getWorkflowData();

        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

        mvm.put("workflowId", StringUtils.defaultIfEmpty(workflow.getId(), "Unknown"));
        mvm.put("payload", StringUtils.defaultIfEmpty(workflowData.getPayload().toString(), "Unknown"));
        mvm.put("initiatedBy", StringUtils.defaultIfEmpty(workflow.getInitiator(), "Unknown"));
        mvm.put("modelId", StringUtils.defaultIfEmpty(workflow.getWorkflowModel().getId(), "Unknown"));
        mvm.put("modelTitle", StringUtils.defaultIfEmpty(workflow.getWorkflowModel().getTitle(), "Unknown"));
        mvm.put("state", StringUtils.defaultIfEmpty(workflow.getState(), "Unknown"));
        mvm.put("active", workflow.isActive());
        mvm.put("sling:resourceType", "acs-commmons/components/utilities/workflow-audit/audit-entry");

        if (workflow.getTimeStarted() != null) {
            final Calendar cal = Calendar.getInstance();
            cal.setTime(workflow.getTimeStarted());
            mvm.put("startedAt", cal);
        }

        if (workflow.getTimeEnded() != null) {
            final Calendar cal = Calendar.getInstance();
            cal.setTime(workflow.getTimeStarted());
            mvm.put("endedAt", cal);
        }

        log.info("Audit logging of [ {} ] for [ {} ]", workflow.getId(), workflowData.getPayload().toString());
    }



    private void auditLogItem(final String eventType, final Workflow workflow, final Resource resource)
            throws RepositoryException, PersistenceException {


        WorkItem workItem = null;
        log.debug("Work Items: {}", workflow.getWorkItems().size());

        if (workflow.getWorkItems().size() == 1) {
            workItem = workflow.getWorkItems().get(0);
        }

        final WorkflowData workflowData = workflow.getWorkflowData();
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

        mvm.put("eventType", eventType);
        mvm.put("workflowId", StringUtils.defaultIfEmpty(workflow.getId(), "Unknown"));
        mvm.put("payload", StringUtils.defaultIfEmpty(workflowData.getPayload().toString(), "Unknown"));

        if (workItem != null) {

            mvm.put("itemId", workItem.getId());
            mvm.put("assignee", workItem.getCurrentAssignee());
            mvm.put("title", workItem.getNode().getTitle());
            mvm.put("description", workItem.getNode().getDescription());

            if (workItem.getTimeStarted() != null) {
                final Calendar cal = Calendar.getInstance();
                cal.setTime(workItem.getTimeStarted());
                mvm.put("startedAt", cal);
            }

            if (workItem.getTimeEnded() != null) {
                final Calendar cal = Calendar.getInstance();
                cal.setTime(workItem.getTimeStarted());
                mvm.put("endedAt", cal);
            }
        }

        mvm.put("sling:resourceType", "acs-commmons/components/utilities/workflow-audit/audit-item");


        log.debug("recorders size: {}", workflowAuditItemRecorders.size());
        for (final Map.Entry<String, WorkflowAuditItemRecorder> entry : workflowAuditItemRecorders.entrySet()) {
            log.debug("Processing recorder [ {} ]", entry.getKey());
            mvm.putAll(entry.getValue().getData(resource.getResourceResolver(), workflow, workItem));
        }

        log.info("Audit logging of [ {} ] for [ {} ]", workflow.getId(), workflowData.getPayload().toString());
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
