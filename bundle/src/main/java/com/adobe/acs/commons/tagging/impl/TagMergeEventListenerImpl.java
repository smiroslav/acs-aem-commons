package com.adobe.acs.commons.tagging.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component(
        label = "ACS AEM Commons - Tag Merge Event Listener",
        description = "Sample implementation of a Custom Event Listener based on Sling",
        immediate = true,
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(
                label = "Event Topics",
                value = {SlingConstants.TOPIC_RESOURCE_ADDED, SlingConstants.TOPIC_RESOURCE_CHANGED},
                description = "[Required] Event Topics this event handler will to respond to.",
                name = EventConstants.EVENT_TOPIC,
                propertyPrivate = true
        )
})
@Service
public final class TagMergeEventListenerImpl implements EventHandler, TopologyEventListener {
    private static final Logger log = LoggerFactory.getLogger(TagMergeEventListenerImpl.class);
    private static final long TOO_LONG_IN_MS = 500;

    private boolean isLeader = false;


    private static final String[] DEFAULT_NODE_TYPES = new String[]{};
    private List<String> nodeTypes = new ArrayList<String>();
    @Property(label = "Node Types",
            description = "The node types to merge tags against. Leave blank for any.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_NODE_TYPES = "node-types";
    
    private static final String[] DEFAULT_RESOURCE_TYPES = new String[]{};
    private List<String> resourceTypes = new ArrayList<String>();
    @Property(label = "Resource Types",
            description = "The resource types to merge tags against. Leave blank for any.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_RESOURCE_TYPES = "resource-types";


    private static final String DEFAULT_DESTINATION_PROPERTY = "";
    private String destinationProperty = DEFAULT_DESTINATION_PROPERTY;
    @Property(label = "Destination Property",
            description = "The property to merge Tags into.",
            value = DEFAULT_DESTINATION_PROPERTY)
    public static final String PROP_DESTINATION_PROPERTY = "destination-property";


    private static final String[] DEFAULT_SOURCE_PROPERTIES = new String[]{};
    @Property(label = "Source Properties",
            description = "The properties to collect the tags for merging from.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_SOURCE_PROPERTIES = "source-properties";

    private List<String> sourceProperties = new ArrayList<String>();

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public final void handleEvent(final Event event) {
        if (!this.isLeader) {
            return;
        }

        final long start = System.currentTimeMillis();

        final String[] added = (String[]) event.getProperty(SlingConstants.PROPERTY_ADDED_ATTRIBUTES);
        final String[] changed = (String[]) event.getProperty(SlingConstants.PROPERTY_CHANGED_ATTRIBUTES);
        final String[] removed = (String[]) event.getProperty(SlingConstants.PROPERTY_REMOVED_ATTRIBUTES);

        final List<String> delta = new ArrayList<String>();

        if (ArrayUtils.isNotEmpty(added)) {
            delta.addAll(Arrays.asList(added));
        }

        if (ArrayUtils.isNotEmpty(changed)) {
            delta.addAll(Arrays.asList(changed));
        }

        if (ArrayUtils.isNotEmpty(removed)) {
            delta.addAll(Arrays.asList(removed));
        }
        
        if (CollectionUtils.containsAny(this.sourceProperties, delta)) {

            final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);

            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

                final Resource resource = resourceResolver.getResource(path);
                if(resource == null) {
                    log.error("Tag merge event handler attempting to work on a non-existing resource [ {} ]", path);
                    return; 
                }

                // Check Node Types
                
                boolean acceptNodeType = true;
                if (CollectionUtils.isNotEmpty(this.nodeTypes)) {
                    final Node node = resource.adaptTo(Node.class);
                    if (node == null) {
                        log.warn("Tag merge event handler attempting to work on a non-existing node [ {} ]", path);
                        return;
                    }

                    acceptNodeType = false;
                    
                    for (final String nodeType : this.nodeTypes) {
                        if (node.isNodeType(nodeType)) {
                            acceptNodeType = true;
                            break;
                        }
                    }
                }
                
                if(!acceptNodeType) {
                    log.debug("Rejecting tag merge for [ {} ] due to node type mismatch", path);
                    return;
                }

                // Check Resource Types

                boolean acceptResourceType = true;
                if (CollectionUtils.isNotEmpty(this.resourceTypes)) {
                    acceptResourceType = false;
                    
                    for (final String resourceType : this.resourceTypes) {
                        if (resource.isResourceType(resourceType)) {
                            acceptResourceType = true;
                            break;
                        }
                    }
                }

                if(!acceptResourceType) {
                    log.debug("Rejecting tag merge for [ {} ] due to resource type mismatch", path);
                    return;
                }

                this.merge(resource, this.destinationProperty, this.sourceProperties);

            } catch (LoginException e) {
                log.error("Could not obtain a ResourceResolver for tag merging", e);
            } catch (PersistenceException e) {
                log.error("Could not persist tag merging", e);
            } catch (RepositoryException e) {
                log.error("Could not check the Node Type of the resource for tag merging", e);
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }

                final long duration = System.currentTimeMillis() - start;
                
                if (duration > TOO_LONG_IN_MS) {
                    log.warn("Tag merge an alarming long time of {} ms. "
                                    + "Long running events may become blacklisted.",
                            duration);
                }
            }
        }

    }

    /**
     * @param resource
     * @param destination
     * @param sources
     */
    public final void merge(final Resource resource, final String destination, final List<String> sources) throws PersistenceException {

        final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
        final Set<String> collectedTagIds = new LinkedHashSet<String>();

        for (final String source : sources) {
            collectedTagIds.addAll(Arrays.asList(properties.get(source, new String[]{})));
        }

        final String[] existingTagIds = properties.get(destination, new String[]{});

        if (!CollectionUtils.isEqualCollection(Arrays.asList(existingTagIds),
                collectedTagIds)) {
            properties.put(destination, collectedTagIds.toArray(new String[collectedTagIds.size()]));

            if (resource.getResourceResolver().hasChanges()) {
                resource.getResourceResolver().commit();
                log.info("Tag merge performed at [ " + resource.getPath() + "@{} ] with tags {}", destination, collectedTagIds);
            }
        }
    }

    @Activate
    protected final void activate(final Map<String, String> config) {
        
        // Node Types
        
        this.nodeTypes = new ArrayList<String>();
        String[] tmp = PropertiesUtil.toStringArray(config.get(PROP_NODE_TYPES),
                DEFAULT_NODE_TYPES);
        
        for(final String t : tmp) {
            if(StringUtils.isNotBlank(t)) {
                this.nodeTypes.add(t);
            }
        }
        
        // Resource Types
        
        this.resourceTypes = new ArrayList<String>();
        tmp = PropertiesUtil.toStringArray(config.get(PROP_RESOURCE_TYPES),
                DEFAULT_RESOURCE_TYPES);
        
        for(final String t : tmp) {
            if(StringUtils.isNotBlank(t)) {
                this.resourceTypes.add(t);
            }
        }

        // Destination Property
        
        this.destinationProperty = PropertiesUtil.toString(config.get(PROP_DESTINATION_PROPERTY), DEFAULT_DESTINATION_PROPERTY);
        
        // Source Property 
        
        this.sourceProperties = Arrays.asList(PropertiesUtil.toStringArray(config.get(PROP_SOURCE_PROPERTIES),
                DEFAULT_SOURCE_PROPERTIES));

        if (CollectionUtils.isEmpty(this.nodeTypes)) {
            log.warn("Tag Merge is targeting all JCR Primary Types");
        }
        
        if (CollectionUtils.isEmpty(this.resourceTypes)) {
            log.warn("Tag Merge is targeting all Resource Types");
        }

        if (StringUtils.isBlank(this.destinationProperty)) {
            log.warn("Tag Merge destination property is Empty.");
        }

        if (CollectionUtils.isEmpty(this.sourceProperties)) {
            log.warn("Tag Merge source properties list is Empty.");
        }
    }

    @Override
    public void handleTopologyEvent(final TopologyEvent event) {
        if (event.getType() == TopologyEvent.Type.TOPOLOGY_CHANGED
                || event.getType() == TopologyEvent.Type.TOPOLOGY_INIT) {
            this.isLeader = event.getNewView().getLocalInstance().isLeader();
        }
    }
}
