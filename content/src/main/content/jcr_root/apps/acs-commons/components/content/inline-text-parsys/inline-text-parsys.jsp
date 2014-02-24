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
          com.adobe.acs.commons.wcm.tags.wcmmode.WCMModeFunctions,
          com.day.cq.wcm.api.WCMMode,
          java.util.HashMap"%>
<%@ page import="com.adobe.acs.commons.wcm.tags.wcmmode.WCMModeFunctions" %><%
    final String selectors = slingRequest.getRequestPathInfo().getSelectorString();

    if(!WCMModeFunctions.isEdit(pageContext)) {
        %><sling:include replaceSelectors="views.main"/><%
    } else {
        if(StringUtils.equals("views.text", selectors)) {
            %><sling:include replaceSelectors="views.text"/><%
        } else {
            %><sling:include replaceSelectors="clean-up"/><%
            %><sling:include replaceSelectors="views.parsys"/><%
        }
    }
%>