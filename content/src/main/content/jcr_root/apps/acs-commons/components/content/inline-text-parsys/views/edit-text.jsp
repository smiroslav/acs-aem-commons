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
%><%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %><%
%><%@page session="false"%><%

    final String ckEditorConfigURI =component.getProperties().get("ckEditorConfig",
            component.getPath() + "/ckeditor/config.js");

    final String editorID = "acs-commons-long-form-editor-id_" + resource.getPath();
%>

<wcmmode:edit>
    <cq:includeClientLib css="acs-commons.wcm.long-form-text"/>

    <form>
        <textarea id="<%= editorID %>"
                  class="acs-commons-long-form-text-editor"
                  data-config="<%=  xssAPI.getValidHref(ckEditorConfigURI) %>"
                  data-current-page="<%= xssAPI.getValidHref(currentPage.getPath()) %>.html"
                  data-action="<%= xssAPI.getValidHref(resource.getPath()) %>"
                  name="./text"><%= properties.get("text", "")%></textarea>
    </form>

    <cq:includeClientLib js="acs-commons.wcm.long-form-text"/>

    <script>
        ACS.CQ.wcm.LongFormText.init(document.getElementById('<%= editorID %>'));
    </script>
</wcmmode:edit>


