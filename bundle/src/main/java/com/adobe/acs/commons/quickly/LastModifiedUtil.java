/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.adobe.acs.commons.quickly;

import com.day.cq.wcm.api.NameConstants;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class LastModifiedUtil {
    private static final Logger log = LoggerFactory.getLogger(LastModifiedUtil.class);

    private static final String UNKNOWN_MODIFIER = "unknown";
    private static final Date ZERO_DATE = new Date(0);

    public static final long getLastModifiedTimestamp(final ResourceResolver resourceResolver, final String path) {
        final Resource resource = resourceResolver.getResource(path);

        return getLastModifiedTimestamp(resource);
    }

    public static final long getLastModifiedTimestamp(final Resource resource) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);

        final long cqLastModified = properties.get(NameConstants.PN_PAGE_LAST_MOD, ZERO_DATE).getTime();
        final long jcrLastModified = properties.get(JcrConstants.JCR_LASTMODIFIED, ZERO_DATE).getTime();

        final long cqCreated = properties.get(NameConstants.PN_CREATED, ZERO_DATE).getTime();
        final long jcrCreated = properties.get(JcrConstants.JCR_CREATED, ZERO_DATE).getTime();

        return findMax(cqLastModified, jcrLastModified, cqCreated, jcrCreated);
    }

    public static final String getLastModifiedBy(final ResourceResolver resourceResolver, final String path) {
        final Resource resource = resourceResolver.getResource(path);
        return getLastModifiedBy(resource);
    }

    public static final String getLastModifiedBy(final Resource resource) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);

        final long lastModifiedTimestamp = getLastModifiedTimestamp(resource);

        final long cqLastModified = properties.get(NameConstants.PN_PAGE_LAST_MOD, ZERO_DATE).getTime();
        final long jcrLastModified = properties.get(JcrConstants.JCR_LASTMODIFIED, ZERO_DATE).getTime();
        final long cqCreated = properties.get(NameConstants.PN_CREATED, ZERO_DATE).getTime();
        final long jcrCreated = properties.get(JcrConstants.JCR_CREATED, ZERO_DATE).getTime();

        if(lastModifiedTimestamp == cqLastModified) {
            return properties.get(NameConstants.PN_LAST_MOD_BY, UNKNOWN_MODIFIER);
        } else if (lastModifiedTimestamp == jcrLastModified) {
            return properties.get(JcrConstants.JCR_LAST_MODIFIED_BY, UNKNOWN_MODIFIER);
        } else if (lastModifiedTimestamp == cqCreated) {
            return properties.get(NameConstants.PN_CREATED_BY, UNKNOWN_MODIFIER);
        } else if (lastModifiedTimestamp == jcrCreated) {
            return properties.get(JcrConstants.JCR_CREATED_BY, UNKNOWN_MODIFIER);
        } else {
            return UNKNOWN_MODIFIER;
        }
    }

    private static final long findMax(final long... values) {
        long max = 0;
        for(final long value : values) {
            max = Math.max(max, value);
        }

        return max;
    }
}
