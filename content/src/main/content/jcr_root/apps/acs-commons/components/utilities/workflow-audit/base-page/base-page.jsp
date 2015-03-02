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
  --%>
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %><%

    pageContext.setAttribute("pagePath", resourceResolver.map(currentPage.getPath()));
    pageContext.setAttribute("resourcePath", resourceResolver.map(resource.getPath()));
    //pageContext.setAttribute("favicon", resourceResolver.map(component.getPath() + "/clientlibs/images/favicon.ico"));

%><!doctype html>
<html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

    <title>Workflow Audit | ACS AEM Commons</title>
    <link rel="shortcut icon" href="${favicon}"/>

    <cq:includeClientLib css="acs-commons.workflow-audit.app"/>
</head>

<body>
    <div id="acs-commons-workflow-audit-app">
        <header class="top">

            <div class="logo">
                <a href="/"><i class="icon-marketingcloud medium"></i></a>
            </div>

            <nav class="crumbs">
                <a href="/miscadmin">Tools</a>
                <a href="${pagePath}.html">Workflow Audit</a>
            </nav>
        </header>

        <div class="page" role="main">
            <div class="content">
                <div class="content-container">
                    <div class="content-container-inner">
                        <h1>Workflow Audit</h1>

                        <cq:include script="includes/content.jsp"/>
                    </div>
                </div>
            </div>
        </div>

    </div>

    <cq:includeClientLib js="acs-commons.workflow-audit.app"/>

    <%-- Register angular app; Decreases chances of collisions w other angular apps on the page (ex. via injection) --%>
    <script type="text/javascript">
        angular.bootstrap(document.getElementById('acs-commons-workflow-audit-app'),
                ['workflowAuditApp']);
    </script>
</body>
</html>