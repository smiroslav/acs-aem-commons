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
          javax.jcr.Node,
          org.apache.commons.lang.StringUtils,
          com.adobe.acs.commons.wcm.tags.wcmmode.WCMModeFunctions,
          com.day.cq.wcm.api.WCMMode,
          com.day.cq.commons.jcr.JcrUtil,
          java.util.List,
          java.util.HashMap"%>
<%@ page import="com.adobe.acs.commons.wcm.tags.wcmmode.WCMModeFunctions" %>
<%@ page import="com.day.jcr.vault.util.JcrConstants" %>

<%-- ONLY EXECUTE IN EDIT MODE --%>
<%
    if(!WCMModeFunctions.isEdit(pageContext)) { return; }

%><%
    final String text = properties.get("text", "");

    final ParagraphSystem paragraphSystem = ParagraphSystem.create(resource, slingRequest);
    final List<Paragraph> paragraphs = paragraphSystem.paragraphs();

    final String[] textParagraphs = StringUtils.splitByWholeSeparator(text, "</p>");

    final Session session = resourceResolver.adaptTo(Session.class);
    final String lastParagraphName = "inline-text-par-" + textParagraphs.length;

    Node lastParagraphNode = lastParagraph.getResource().adaptTo(Node.class);

    int modifications = 0;
    for(int i = paragraphs.size(); i >= 0; i--) {
        final Paragraph paragraph = paragraphs.get(i);
        final int paragraphIndex = Integer.parseInt(StringUtils.removeStart("inline-text-par-"));
        final Resource paragraphResource = paragraph.getResource();

        for(final Resource child : paragraphResource.getChildren()) {
            if(lastParagraphNode == null) {
                lastParagraphNode = JcrUtil.createPath(lastParagraph.getResource().getPath(),
                        false, JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, session, true);
            }

            final Node childNode = child.adaptTo(Node.class);
            JcrUtil.createUniqueNode(lastParagraphNode, child.getName(), JcrConstants.NT_UNSTRUCTURED, session);
            childNode.remove();
            modifications++;
        }
    }

    if(modifications < 0) {
       session.save();
    }
%> Cleaned up!