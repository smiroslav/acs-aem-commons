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

package com.adobe.acs.commons.apps.quickly.comparators;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class PathRelevanceComparator implements Comparator<Resource> {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(PathRelevanceComparator.class);

    private static final int WCM_PAGE = 1000;

    private static final int DAM = 900;

    private static final int WCM_CAMPAIGNS = 780;

    private static final int WCM_USER_GENERATED = 770;

    private static final int WCM_CATALOGS = 760;

    private static final int WCM_COMMUNITIES = 750;

    private static final int ETC_TAGS = 600;

    private static final int ETC_WORKFLOW = 300;

    private static final int ETC_WORKFLOW_DATA = 100;

    private static final int ETC_MISC = 250;

    private static final int CATCH_ALL = 250;

    @Override
    public final int compare(final Resource r1, final Resource r2) {
        final String path1 = r1 != null ? r1.getPath() : "";
        final String path2 = r2 != null ? r2.getPath() : "";

        int relevance1 = this.getRelevance(path1);
        int relevance2 = this.getRelevance(path2);

        if (relevance1 < relevance2) {
            return 1;
        } else if (relevance1 > relevance2) {
            return -1;
        } else {
            return new LexicographicalResourcePathComparator().compare(r1, r2);
        }
    }

    private int getRelevance(final String path) {

        /* Content */
        if (StringUtils.startsWith(path, "/content/")) {
            if (StringUtils.startsWith(path, "/content/dam/")) {
                return DAM;
            } else if (StringUtils.startsWith(path, "/content/campaigns/")) {
                return WCM_CAMPAIGNS;
            } else if (StringUtils.startsWith(path, "/content/usergenerated/")) {
                return WCM_USER_GENERATED;
            } else if (StringUtils.startsWith(path, "/content/communities/")) {
                return WCM_COMMUNITIES;
            } else if (StringUtils.startsWith(path, "/content/catalogs/")) {
                return WCM_CATALOGS;
            } else {
                return WCM_PAGE;
            }
        }

        /* Etc */
        if (StringUtils.startsWith(path, "/etc/")) {

            /* Workflow */
            if (StringUtils.startsWith(path, "/etc/workflow")) {
                if (StringUtils.startsWith(path, "/etc/workflow/instances")) {
                    return ETC_WORKFLOW_DATA;
                } else if (StringUtils.startsWith(path, "/etc/workflow/instances")) {
                    return ETC_WORKFLOW;
                }
                return ETC_MISC;
            } else if (StringUtils.startsWith(path, "/etc/tags/")) {
                return ETC_TAGS;
            }
        }

        return CATCH_ALL;
    }
}
