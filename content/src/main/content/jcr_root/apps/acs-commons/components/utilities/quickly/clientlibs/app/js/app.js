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

/*global angular: false, console: false */
var quickly = angular.module('quickly',['ngSanitize']).config(function($sceProvider) {
    // Completely disable SCE.
    $sceProvider.enabled(false);
});

$(function() {
    $('body').keypress(function(e){
        var elementId = 'acs-commons-quickly-ctrl';

        // ctrl-space
        if(e.ctrlKey && e.which === 0) {
            angular.element(document.getElementById(elementId)).scope().app.toggle();
            angular.element(document.getElementById(elementId)).scope().$apply();
            e.preventDefault();
        }
    });
});