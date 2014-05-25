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

package com.adobe.acs.commons.quickly.operations.impl;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.Result;
import com.adobe.acs.commons.quickly.operations.AbstractOperation;
import com.adobe.acs.commons.quickly.comparators.LexicographicalResourcePathComparator;
import com.adobe.acs.commons.quickly.results.CRXDEResult;
import com.adobe.acs.commons.quickly.PathBasedResourceFinder;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component(
        label = "ACS AEM Commons - Quickly - CRXDE Operation"
)
@Properties({
        @Property(
                name = "cmd",
                value = CRXDEOperationImpl.CMD,
                propertyPrivate = true
        )
})
@Service
public class CRXDEOperationImpl extends AbstractOperation {
    private static final Logger log = LoggerFactory.getLogger(CRXDEOperationImpl.class);

    public static final String CMD = "crxde";

    @Reference
    private PathBasedResourceFinder pathBasedResourceFinder;

    @Override
    public boolean accepts(final SlingHttpServletRequest slingRequest, final Command cmd) {
        return StringUtils.equalsIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final List<Result> results = new ArrayList<Result>();

        results.add(new CRXDEResult());

        return results;
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest slingRequest, final Command cmd) {
        final long start = System.currentTimeMillis();

        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();
        final List<Result> results = new ArrayList<Result>();

        final List<Resource> resources = pathBasedResourceFinder.findAll(resourceResolver,
               cmd.getParam(),
               PathBasedResourceFinder.DEFAULT_QUERY_LIMIT);


        // Sorting is Command specific; Sort by path in crxde

        Collections.sort(resources, new LexicographicalResourcePathComparator());

        if (resources.isEmpty()) {
            results.addAll(this.withoutParams(slingRequest, cmd));
        } else {
            for(final Resource resource : resources) {
                if(CRXDEResult.accepts(resource)) {
                    results.add(new CRXDEResult(resource));
                }
            }
        }

        log.debug("CRXDE w/ Params({}) >> Execution time: {} ms", cmd.getParam(), System.currentTimeMillis() - start);

        return results;
    }
}
