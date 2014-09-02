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

package com.adobe.acs.commons.content.properties.bulk.impl.servlets;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class RemovePropertyServletTest {

    @Test
    public void testExecute() throws Exception {
        RemovePropertyServlet servlet = new RemovePropertyServlet();

        Resource resource = mock(Resource.class);
        ModifiableValueMap mvm = new ModifiableValueMapDecorator(new HashMap<String, Object>());
        mvm.put("myProp", "anything");


        final ValueMap params = new ValueMapDecorator(new HashMap<String, Object>());
        params.put("name", "myProp");

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(mvm);
        servlet.execute(resource, params);

        Assert.assertFalse(mvm.keySet().contains("myProp"));
    }
}