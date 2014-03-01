<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2014 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%><%@page session="false"
        import="java.util.HashSet,
                java.util.Set,
                com.day.cq.commons.jcr.JcrConstants,
                com.day.cq.wcm.api.WCMMode,
                com.day.cq.wcm.api.components.IncludeOptions,
                com.day.cq.wcm.foundation.Paragraph,
                com.day.cq.wcm.foundation.ParagraphSystem" %><%
%><%@include file="/libs/foundation/global.jsp"%>

<div style="border: solid 1px red"><%

    final ParagraphSystem paragraphSystem =
            ParagraphSystem.create(resource, slingRequest);

    if(paragraphSystem.paragraphs().size() > 0) {

        for (final Paragraph paragraph: paragraphSystem.paragraphs()) {
            if (editContext != null) {editContext.setAttribute("currentResource", paragraph); }

            IncludeOptions.getOptions(request, true).getCssClassNames().add("section");

            %><sling:include resource="<%= paragraph %>"/><%
        }

    } else {

        final String newType = resource.getResourceType() + "/new";

            // Only show if parsys is empty
        if (editContext != null) {
            editContext.setAttribute("currentResource", null);

            IncludeOptions.getOptions(request, true).getCssClassNames().add("section");

            %><cq:include path="*" resourceType="<%= newType %>"/><%
        }
    }
%>
</div>