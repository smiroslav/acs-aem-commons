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
%><%@page session="false" import="java.util.UUID"
%><%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %>

<wcmmode:edit>
    <cq:includeClientLib categories="cq.ckeditor"/>

    <%
        final String uuid = "uuid_" + UUID.randomUUID().toString();

    %>



    <form method="post" class="inline-text-parsys-form"
          data-page="<%= currentPage.getPath() %>.html"
          data-uuid="<%= uuid %>"
          action="<%= resource.getPath() %>"/>
        <textarea id="<%= uuid %>" name="./text" style="width:100%;height:500px;"><%= properties.get("text", "")%></textarea>

        <input name=".inline-text-parsys-submit" type="submit" value="Save"/>
    </form>

    <script type="text/javascript">
        CKEDITOR.replace( '<%= uuid %>' );
    </script>

    <script>
        $(function() {
           $('body').on('submit', '.inline-text-parsys-form', function(e) {
               var $form = $(this);
               e.preventDefault();


               $.post($form.attr('action'), $form.serialize(), function(data) {
                    window.location = $form.data('page');
               });

           });
        });
    </script>

</wcmmode:edit>


