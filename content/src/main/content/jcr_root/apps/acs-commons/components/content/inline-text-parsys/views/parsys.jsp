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
  --%>

<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false"
          import="com.day.cq.wcm.foundation.ParagraphSystem,
          com.day.cq.wcm.foundation.Paragraph,
          org.apache.commons.lang.StringUtils,
          java.util.HashMap"%><%
%><%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %><%

    final String delimiter = "</p>";
    final String text = properties.get("text", "");

    final ParagraphSystem paragraphSystem = ParagraphSystem.create(resource, slingRequest);

    final String[] textParagraphs = StringUtils.splitByWholeSeparator(text, delimiter);

    int index = 0;
    for(final String textParagraph : textParagraphs) { %>
        <cq:include path="<%= "inline-par" + index++ %>" resourceType="foundation/components/parsys"/><%
        %><%= textParagraph %><%= delimiter %><%
    }

%>


