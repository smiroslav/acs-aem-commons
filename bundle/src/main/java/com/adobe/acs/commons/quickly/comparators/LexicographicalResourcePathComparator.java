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

package com.adobe.acs.commons.quickly.comparators;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class LexicographicalResourcePathComparator implements Comparator<Resource> {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(LexicographicalResourcePathComparator.class);

    @Override
    public int compare(final Resource r1, final Resource r2) {
        final String path1 = r1 != null ? r1.getPath() : "";
        final String path2 = r2 != null ? r2.getPath() : "";

        return path1.compareTo(path2);
    }
}
