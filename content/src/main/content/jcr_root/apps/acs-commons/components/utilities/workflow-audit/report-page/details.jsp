<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %><%

    log.error("SLING FORWARD");

%><<sling:forward path="${resource.path}"
               replaceSelectors=""
               resourceType="acs-commons/components/utilities/workflow-audit/details-page" />