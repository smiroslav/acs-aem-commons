<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2013 Adobe
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
  --%><%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false"
          import="org.apache.sling.api.resource.Resource,
                 org.apache.sling.api.resource.ValueMap,
                 java.util.ArrayList,
                 java.util.Iterator, java.util.List" %><%

    String suffix = slingRequest.getRequestPathInfo().getSuffix();
    Resource wfResource = resourceResolver.getResource(suffix);
    ValueMap wfProperties = wfResource.adaptTo(ValueMap.class);

    List<ValueMap> wiProperties = new ArrayList<ValueMap>();
    Iterator<Resource> children = wfResource.listChildren();

    while (children.hasNext()) {
        Resource tmp = children.next();
        wiProperties.add(tmp.adaptTo(ValueMap.class));
    }

    pageContext.setAttribute("workflow", wfProperties);
    pageContext.setAttribute("workItems", wiProperties);

%>

<h3>Workflow Details</h3>

<ul>
    <li>Workflow: ${workflow.modelTitle}</li>
    <li>Initiator: ${workflow.initiator}</li>
    <li>Workflow: ${workflow.modelVersion}</li>
    <li>Payload: ${workflow.payload}</li>
    <li>Status: ${workflow.status}</li>
    <li>Start Time: <fmt:formatDate value="${workflow.startTime.time}" pattern="yy-MMM-dd"/></li>
    <li>End Time: <fmt:formatDate value="${workflow.endTime.time}" pattern="yy-MMM-dd"/></li>
</ul>

<hr/>

<h4>Workflow Step History</h4>

<c:forEach var="workItems" items="workItems">

    <h5>${workItem.nodeId}</h5>

    <ul>
        <li>Assignee: ${workItem.assignee}</li>
        <li>Status: ${workItem.status}</li>
        <li>Start Time: <fmt:formatDate value="${workItem.startTime.time}" pattern="yy-MMM-dd"/></li>
        <li>End Time: <fmt:formatDate value="${workItem.endTime.time}" pattern="yy-MMM-dd"/></li>
    </ul>

</c:forEach>