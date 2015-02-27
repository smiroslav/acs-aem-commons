package com.adobe.acs.commons.workflow.audit.impl;


import com.adobe.acs.commons.workflow.audit.WorkflowAuditItemRecorder;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.Workflow;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;

@Component(
        label = "ACS AEM Commons - Workflow Audit - Comment Recorder"
)
@Properties({
        @Property(
                name = WorkflowAuditItemRecorder.PROP_TYPE,
                value = CommentRecorderImpl.TYPE,
                propertyPrivate = true
        )
})
@Service
public class CommentRecorderImpl implements WorkflowAuditItemRecorder {
    private static final Logger log = LoggerFactory.getLogger(CommentRecorderImpl.class);
    private static final String PN_COMMENT = "comment";

    public static final String TYPE = "comment";

    @Override
    public ValueMap getData(final ResourceResolver resourceResolver, final Workflow workflow, final WorkItem workItem) {
        log.debug("Processing Comment Recorder");

        final ValueMap data = new ValueMapDecorator(new HashMap<String, Object>());

        log.debug(Arrays.toString(workItem.getMetaDataMap().keySet().toArray(new String[]{})));
        log.debug(Arrays.toString(workItem.getMetaDataMap().values().toArray(new String[]{})));

        final String comment = workItem.getMetaDataMap().get("comment", String.class);

        if (StringUtils.isNotBlank(comment)) {
            log.debug("Found comment: {}", comment);
            data.put(PN_COMMENT, comment);
        }

        return data;
    }
}
