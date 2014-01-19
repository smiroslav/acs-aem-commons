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

package com.adobe.acs.commons.quickly.commands.impl.open;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.Result;
import com.adobe.acs.commons.quickly.commands.AbstractCommandHandler;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Quickly - Go Command Handler"
)
@Properties({
        @Property(
                name = "cmd",
                value = OpenCommandHandlerImpl.CMD,
                propertyPrivate = true
        )
})
@Service
public class OpenCommandHandlerImpl extends AbstractCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(OpenCommandHandlerImpl.class);

    public static final String CMD = "open";

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public boolean accepts(final ResourceResolver resourceResolver, final Command cmd) {
        return StringUtils.equalsIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    protected List<Result> withoutParams(final ResourceResolver resourceResolver, final Command cmd) {
        return new LinkedList<Result>();
    }

    @Override
    protected List<Result> withParams(final ResourceResolver resourceResolver,
                                      final Command cmd) {
        final List<Result> results = new LinkedList<Result>();

        final Map<String, String> map = new HashMap<String, String>();
        map.put("type", "cq:Page");
        map.put("nodename", cmd.getParam() + "*");
        map.put("p.limit", "20");

        final Query query = queryBuilder.createQuery(PredicateGroup.create(map),
                resourceResolver.adaptTo(Session.class));
        final SearchResult result = query.getResult();

        for (final Hit hit : result.getHits()) {
            try {
                final String path = hit.getPath();
                if (StringUtils.startsWith(path, "/content/dam")) {
                    results.add(new OpenResult(hit.getTitle(), hit.getPath(), "/damadmin#" + hit.getPath()));
                } else if (StringUtils.startsWith(path, "/content")) {
                    results.add(new OpenResult(hit.getTitle(), hit.getPath(), "/cf#" + hit.getPath() + ".html"));
                } else if (StringUtils.startsWith(path, "/etc")) {
                    results.add(new OpenResult(hit.getTitle(), hit.getPath(), hit.getPath() + ".html"));
                }
            } catch (RepositoryException e) {
                log.error("Could not access repository for hit: {}. Lucene index may be out of sync.", hit);
            }
        }

        return results;
    }
}